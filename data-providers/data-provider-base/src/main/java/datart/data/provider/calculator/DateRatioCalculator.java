package datart.data.provider.calculator;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import datart.core.base.consts.ValueType;
import datart.core.base.PageInfo;
import datart.core.data.provider.Column;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import datart.core.data.provider.SingleTypedValue;
import datart.core.data.provider.StdSqlOperator;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.Calc;
import datart.core.data.provider.sql.FilterOperator;
import datart.core.data.provider.sql.FunctionColumn;
import datart.core.data.provider.sql.GroupByOperator;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class DateRatioCalculator extends AbstractCalculator {

    private static final String TYPE = "dateRatio";
    static final int MAX_SUPPLEMENTARY_ROWS = 100_000;

    private Dataframe historyReference;
    private final Map<String, Dataframe> periodReferences = new HashMap<>();

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public void calculate(Dataframe dataframe,
                          AggregateOperator aggregate,
                          ExecuteParam executeParam,
                          DataProviderSource source,
                          QueryScript queryScript,
                          DataProvider dataProvider) throws Exception {
        if (dataframe == null || aggregate == null || aggregate.getCalc() == null
                || dataframe.getRows() == null || dataframe.getRows().isEmpty()) {
            return;
        }
        DateRatioConfig config = parseConfig(aggregate.getCalc());
        int dataIndex = findColumnIndex(dataframe, aggregate.getAlias());
        if (dataIndex < 0) {
            throw new IllegalStateException("Advanced calculation column is missing: " + aggregate.getAlias());
        }
        dataframe.getColumns().get(dataIndex).setCalc(aggregate.getCalc());
        if (StringUtils.isBlank(config.select)) {
            calculateFromCurrentFrame(dataframe, dataIndex, executeParam, source, queryScript, dataProvider, config);
        } else {
            calculateSelectedPeriod(dataframe, dataIndex, aggregate, executeParam,
                    source, queryScript, dataProvider, config);
        }
    }

    private void calculateFromCurrentFrame(Dataframe dataframe,
                                           int dataIndex,
                                           ExecuteParam executeParam,
                                           DataProviderSource source,
                                           QueryScript queryScript,
                                           DataProvider dataProvider,
                                           DateRatioConfig config) {
        DateField dateField = findDateField(executeParam, dataframe);
        if (dateField == null) {
            clearColumn(dataframe, dataIndex);
            return;
        }
        Dataframe reference = dataframe;
        if (dataProvider != null) {
            if (historyReference == null) {
                historyReference = executeBounded(dataProvider, source, queryScript,
                        unpagedCopy(executeParam), "period comparison history");
            }
            reference = historyReference;
        }
        int referenceDataIndex = findColumnIndex(reference,
                dataframe.getColumns().get(dataIndex).columnKey());
        DateField referenceDateField = findDateField(executeParam, reference);
        if (referenceDataIndex < 0 || referenceDateField == null) {
            clearColumn(dataframe, dataIndex);
            return;
        }
        List<Integer> referenceDimensionIndexes = dimensionIndexes(executeParam, reference,
                referenceDateField.columnIndex);
        List<Integer> outputDimensionIndexes = dimensionIndexes(executeParam, dataframe,
                dateField.columnIndex);
        Map<HistoryKey, BigDecimal> history = new HashMap<>();
        for (List<Object> row : reference.getRows()) {
            BigDecimal value = decimal(row.get(referenceDataIndex));
            String period = normalizePeriod(row.get(referenceDateField.columnIndex), referenceDateField.level);
            if (value != null && period != null) {
                history.put(new HistoryKey(dimensions(row, referenceDimensionIndexes), period), value);
            }
        }
        for (List<Object> row : dataframe.getRows()) {
            BigDecimal current = decimal(row.get(dataIndex));
            String period = normalizePeriod(row.get(dateField.columnIndex), dateField.level);
            BigDecimal previous = period == null ? null : history.get(new HistoryKey(
                    dimensions(row, outputDimensionIndexes), DatePeriod.parse(period, dateField.level)
                            .previous(config.ratioType).label()));
            row.set(dataIndex, calculateValue(current, previous, config.valueType));
        }
    }

    private void calculateSelectedPeriod(Dataframe dataframe,
                                         int dataIndex,
                                         AggregateOperator aggregate,
                                         ExecuteParam executeParam,
                                         DataProviderSource source,
                                         QueryScript queryScript,
                                         DataProvider dataProvider,
                                         DateRatioConfig config) throws Exception {
        if (dataProvider == null || config.column == null || config.column.length == 0) {
            clearColumn(dataframe, dataIndex);
            return;
        }
        DateLevel level = DateLevel.fromSnippet(config.snippet)
                .orElseThrow(() -> new IllegalArgumentException("Date ratio snippet is invalid"));
        DatePeriod currentPeriod = DatePeriod.parse(config.select, level);
        Dataframe currentFrame = executePeriod(dataProvider, source, queryScript, executeParam,
                config.column, currentPeriod);
        Dataframe previousFrame = executePeriod(dataProvider, source, queryScript, executeParam,
                config.column, currentPeriod.previous(config.ratioType));
        Map<List<Object>, BigDecimal> currentValues = valuesByDimension(currentFrame, aggregate, executeParam);
        Map<List<Object>, BigDecimal> previousValues = valuesByDimension(previousFrame, aggregate, executeParam);
        List<Integer> dimensions = dimensionIndexes(executeParam, dataframe, -1);
        for (List<Object> row : dataframe.getRows()) {
            List<Object> key = dimensions(row, dimensions);
            row.set(dataIndex, calculateValue(currentValues.get(key), previousValues.get(key), config.valueType));
        }
    }

    private Dataframe executePeriod(DataProvider dataProvider,
                                    DataProviderSource source,
                                    QueryScript queryScript,
                                    ExecuteParam original,
                                    String[] dateColumn,
                                    DatePeriod period) throws Exception {
        ExecuteParam copy = unpagedCopy(original);
        String dateColumnKey = String.join(".", dateColumn);
        copy.getFilters().removeIf(filter -> dateColumnKey.equals(filter.getColumnKey()));
        copy.getFilters().add(boundFilter(dateColumn, FilterOperator.SqlOperator.GTE, period.start));
        copy.getFilters().add(boundFilter(dateColumn, FilterOperator.SqlOperator.LT, period.end()));
        String cacheKey = String.join(".", dateColumn) + ':' + period.level + ':'
                + period.start + ':' + period.end();
        Dataframe cached = periodReferences.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        Dataframe result = executeBounded(dataProvider, source, queryScript, copy,
                "selected period " + period.label());
        periodReferences.put(cacheKey, result);
        return result;
    }

    private Dataframe executeBounded(DataProvider dataProvider,
                                     DataProviderSource source,
                                     QueryScript queryScript,
                                     ExecuteParam param,
                                     String description) {
        try {
            Dataframe result = dataProvider.execute(source, queryScript, param);
            if (result != null && result.getRows() != null
                    && result.getRows().size() > MAX_SUPPLEMENTARY_ROWS) {
                throw new IllegalStateException("Period comparison " + description
                        + " exceeded " + MAX_SUPPLEMENTARY_ROWS + " rows");
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load " + description, e);
        }
    }

    private FilterOperator boundFilter(String[] column, FilterOperator.SqlOperator operator, LocalDate value) {
        FilterOperator filter = new FilterOperator();
        filter.setColumn(column);
        filter.setSqlOperator(operator);
        filter.setValues(new SingleTypedValue[]{new SingleTypedValue(value.toString(), ValueType.DATE)});
        return filter;
    }

    private ExecuteParam copy(ExecuteParam original) {
        ExecuteParam copy = new ExecuteParam();
        copy.setKeywords(list(original.getKeywords()));
        copy.setColumns(list(original.getColumns()));
        copy.setAggregators(list(original.getAggregators()));
        copy.setFilters(list(original.getFilters()));
        copy.setGroups(list(original.getGroups()));
        copy.setOrders(list(original.getOrders()));
        copy.setFunctionColumns(list(original.getFunctionColumns()));
        copy.setIncludeColumns(original.getIncludeColumns() == null
                ? null : new HashSet<>(original.getIncludeColumns()));
        copy.setPageInfo(copyPageInfo(original.getPageInfo()));
        copy.setServerAggregate(original.isServerAggregate());
        copy.setConcurrencyOptimize(false);
        copy.setCacheEnable(false);
        copy.setCacheExpires(original.getCacheExpires());
        return copy;
    }

    private ExecuteParam unpagedCopy(ExecuteParam original) {
        ExecuteParam copy = copy(original);
        copy.setOrders(new ArrayList<>());
        copy.setPageInfo(PageInfo.builder()
                .pageNo(1)
                .pageSize(MAX_SUPPLEMENTARY_ROWS + 1L)
                .countTotal(false)
                .build());
        return copy;
    }

    private PageInfo copyPageInfo(PageInfo pageInfo) {
        if (pageInfo == null) {
            return null;
        }
        return PageInfo.builder()
                .pageNo(pageInfo.getPageNo())
                .pageSize(pageInfo.getPageSize())
                .total(pageInfo.getTotal())
                .countTotal(pageInfo.isCountTotal())
                .build();
    }

    private <T> List<T> list(List<T> values) {
        return values == null ? new ArrayList<>() : new ArrayList<>(values);
    }

    private Map<List<Object>, BigDecimal> valuesByDimension(Dataframe dataframe,
                                                            AggregateOperator aggregate,
                                                            ExecuteParam executeParam) {
        if (dataframe == null || dataframe.getRows() == null) {
            return Collections.emptyMap();
        }
        int valueIndex = findColumnIndex(dataframe, aggregate.getAlias());
        if (valueIndex < 0) {
            return Collections.emptyMap();
        }
        List<Integer> indexes = dimensionIndexes(executeParam, dataframe, -1);
        Map<List<Object>, BigDecimal> values = new HashMap<>();
        for (List<Object> row : dataframe.getRows()) {
            values.put(dimensions(row, indexes), decimal(row.get(valueIndex)));
        }
        return values;
    }

    private DateField findDateField(ExecuteParam executeParam, Dataframe dataframe) {
        DateField selected = null;
        if (executeParam.getFunctionColumns() == null) {
            return null;
        }
        for (FunctionColumn function : executeParam.getFunctionColumns()) {
            Optional<DateLevel> level = DateLevel.fromSnippet(function.getSnippet());
            int columnIndex = findColumnIndex(dataframe, function.getAlias());
            if (level.isPresent() && columnIndex >= 0
                    && (selected == null || level.get().rank > selected.level.rank)) {
                selected = new DateField(level.get(), columnIndex);
            }
        }
        return selected;
    }

    private List<Integer> dimensionIndexes(ExecuteParam executeParam, Dataframe dataframe, int excluded) {
        List<Integer> indexes = new ArrayList<>();
        if (executeParam.getGroups() == null) {
            return indexes;
        }
        for (GroupByOperator group : executeParam.getGroups()) {
            int index = findColumnIndex(dataframe,
                    StringUtils.defaultIfBlank(group.getAlias(), group.getColumnKey()));
            if (index >= 0 && index != excluded) {
                indexes.add(index);
            }
        }
        return indexes;
    }

    private int findColumnIndex(Dataframe dataframe, String alias) {
        if (dataframe.getColumns() == null || alias == null) {
            return -1;
        }
        for (int i = 0; i < dataframe.getColumns().size(); i++) {
            Column column = dataframe.getColumns().get(i);
            if (alias.equals(column.columnKey()) || alias.equals(column.columnName())) {
                return i;
            }
        }
        return -1;
    }

    private List<Object> dimensions(List<Object> row, List<Integer> indexes) {
        List<Object> dimensions = new ArrayList<>(indexes.size());
        for (Integer index : indexes) {
            dimensions.add(row.get(index));
        }
        return dimensions;
    }

    private Object calculateValue(BigDecimal current, BigDecimal previous, ValueTypeMode mode) {
        if (current == null || previous == null) {
            return null;
        }
        if (mode == ValueTypeMode.diff) {
            return current.subtract(previous).doubleValue();
        }
        if (mode == ValueTypeMode.percent && previous.compareTo(BigDecimal.ZERO) != 0) {
            return current.divide(previous, 8, RoundingMode.HALF_UP)
                    .subtract(BigDecimal.ONE).doubleValue();
        }
        return null;
    }

    private BigDecimal decimal(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private String normalizePeriod(Object value, DateLevel level) {
        if (value == null) {
            return null;
        }
        try {
            return DatePeriod.parse(String.valueOf(value), level).label();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private DateRatioConfig parseConfig(Calc calc) {
        JSONObject json = (JSONObject) JSON.toJSON(calc.getConfig());
        if (json == null) {
            throw new IllegalArgumentException("Date ratio configuration is incomplete");
        }
        DateRatioConfig config = new DateRatioConfig();
        config.snippet = json.getString("snippet");
        config.select = json.getString("select");
        JSONArray column = json.getJSONArray("column");
        config.column = column == null ? null : column.toArray(new String[0]);
        try {
            config.ratioType = RatioType.valueOf(json.getString("ratioType"));
            config.valueType = ValueTypeMode.valueOf(json.getString("valueType"));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Date ratio configuration is incomplete", e);
        }
        return config;
    }

    private void clearColumn(Dataframe dataframe, int dataIndex) {
        for (List<Object> row : dataframe.getRows()) {
            row.set(dataIndex, null);
        }
    }

    private enum RatioType {
        last,
        year
    }

    private enum ValueTypeMode {
        diff,
        percent
    }

    private enum DateLevel {
        YEAR(1, StdSqlOperator.AGG_DATE_YEAR),
        QUARTER(2, StdSqlOperator.AGG_DATE_QUARTER),
        MONTH(3, StdSqlOperator.AGG_DATE_MONTH),
        WEEK(4, StdSqlOperator.AGG_DATE_WEEK),
        DAY(5, StdSqlOperator.AGG_DATE_DAY);

        private final int rank;
        private final StdSqlOperator operator;

        DateLevel(int rank, StdSqlOperator operator) {
            this.rank = rank;
            this.operator = operator;
        }

        private static Optional<DateLevel> fromSnippet(String snippet) {
            if (snippet == null) {
                return Optional.empty();
            }
            for (DateLevel level : values()) {
                if (snippet.startsWith(level.operator.getSymbol())) {
                    return Optional.of(level);
                }
            }
            return Optional.empty();
        }
    }

    private static class DateField {
        private final DateLevel level;
        private final int columnIndex;

        private DateField(DateLevel level, int columnIndex) {
            this.level = level;
            this.columnIndex = columnIndex;
        }
    }

    private static class HistoryKey {
        private final List<Object> dimensions;
        private final String period;

        private HistoryKey(List<Object> dimensions, String period) {
            this.dimensions = dimensions;
            this.period = period;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof HistoryKey)) {
                return false;
            }
            HistoryKey that = (HistoryKey) o;
            return Objects.equals(dimensions, that.dimensions) && Objects.equals(period, that.period);
        }

        @Override
        public int hashCode() {
            return Objects.hash(dimensions, period);
        }
    }

    private static class DatePeriod {
        private static final WeekFields ISO_WEEK = WeekFields.ISO;
        private final DateLevel level;
        private final LocalDate start;

        private DatePeriod(DateLevel level, LocalDate start) {
            this.level = level;
            this.start = start;
        }

        private static DatePeriod parse(String value, DateLevel level) {
            try {
                switch (level) {
                    case YEAR:
                        return new DatePeriod(level, LocalDate.of(Integer.parseInt(value), 1, 1));
                    case QUARTER:
                        String[] quarter = value.replace("Q", "").split("-");
                        int quarterNumber = Integer.parseInt(quarter[1]);
                        return new DatePeriod(level, LocalDate.of(Integer.parseInt(quarter[0]),
                                (quarterNumber - 1) * 3 + 1, 1));
                    case MONTH:
                        String[] month = value.split("-");
                        return new DatePeriod(level, LocalDate.of(Integer.parseInt(month[0]),
                                Integer.parseInt(month[1]), 1));
                    case WEEK:
                        String[] week = value.split("-");
                        int weekYear = Integer.parseInt(week[0]);
                        int weekNumber = Integer.parseInt(week[1].replace("W", ""));
                        LocalDate weekStart = LocalDate.of(weekYear, 1, 4)
                                .with(ISO_WEEK.weekOfWeekBasedYear(), weekNumber)
                                .with(ISO_WEEK.dayOfWeek(), DayOfWeek.MONDAY.getValue());
                        return new DatePeriod(level, weekStart);
                    case DAY:
                        String[] day = value.split("-");
                        return new DatePeriod(level, LocalDate.of(Integer.parseInt(day[0]),
                                Integer.parseInt(day[1]), Integer.parseInt(day[2])));
                    default:
                        throw new IllegalArgumentException("Unsupported date level");
                }
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Invalid " + level + " period: " + value, e);
            }
        }

        private DatePeriod previous(RatioType type) {
            if (type == RatioType.year) {
                if (level == DateLevel.WEEK) {
                    int targetYear = start.get(ISO_WEEK.weekBasedYear()) - 1;
                    int week = start.get(ISO_WEEK.weekOfWeekBasedYear());
                    LocalDate lastWeek = LocalDate.of(targetYear, 12, 28);
                    int maxWeek = lastWeek.get(ISO_WEEK.weekOfWeekBasedYear());
                    LocalDate target = LocalDate.of(targetYear, 1, 4)
                            .with(ISO_WEEK.weekOfWeekBasedYear(), Math.min(week, maxWeek))
                            .with(ISO_WEEK.dayOfWeek(), DayOfWeek.MONDAY.getValue());
                    return new DatePeriod(level, target);
                }
                return new DatePeriod(level, start.minusYears(1));
            }
            switch (level) {
                case YEAR:
                    return new DatePeriod(level, start.minusYears(1));
                case QUARTER:
                    return new DatePeriod(level, start.minusMonths(3));
                case MONTH:
                    return new DatePeriod(level, start.minusMonths(1));
                case WEEK:
                    return new DatePeriod(level, start.minusWeeks(1));
                case DAY:
                    return new DatePeriod(level, start.minusDays(1));
                default:
                    throw new IllegalArgumentException("Unsupported date level");
            }
        }

        private LocalDate end() {
            switch (level) {
                case YEAR:
                    return start.plusYears(1);
                case QUARTER:
                    return start.plusMonths(3);
                case MONTH:
                    return start.plusMonths(1);
                case WEEK:
                    return start.plusWeeks(1);
                case DAY:
                    return start.plusDays(1);
                default:
                    throw new IllegalArgumentException("Unsupported date level");
            }
        }

        private String label() {
            switch (level) {
                case YEAR:
                    return String.valueOf(start.getYear());
                case QUARTER:
                    return start.getYear() + "-" + (((start.getMonthValue() - 1) / 3) + 1);
                case MONTH:
                    return YearMonth.from(start).toString();
                case WEEK:
                    return String.format("%04d-%02d", start.get(ISO_WEEK.weekBasedYear()),
                            start.get(ISO_WEEK.weekOfWeekBasedYear()));
                case DAY:
                    return start.format(DateTimeFormatter.ISO_LOCAL_DATE);
                default:
                    throw new IllegalArgumentException("Unsupported date level");
            }
        }
    }

    private static class DateRatioConfig {
        private String[] column;
        private String snippet;
        private String select;
        private RatioType ratioType;
        private ValueTypeMode valueType;
    }
}

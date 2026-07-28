package datart.data.provider.calculator;

import datart.core.base.consts.ValueType;
import datart.core.base.PageInfo;
import datart.core.data.provider.Column;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.SingleTypedValue;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.Calc;
import datart.core.data.provider.sql.FilterOperator;
import datart.core.data.provider.sql.FunctionColumn;
import datart.core.data.provider.sql.GroupByOperator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DateRatioCalculatorTest {

    @Test
    void calculatesPreviousMonthPercentagePerDimensionAndHandlesZero() throws Exception {
        AggregateOperator aggregate = aggregate("SUM(amount)-ratio", "last", "percent");
        Dataframe dataframe = dataframe(Arrays.asList(
                row("east", "2024-01", 100),
                row("east", "2024-02", 150),
                row("west", "2024-01", 0),
                row("west", "2024-02", 10)
        ));

        new DateRatioCalculator().calculate(dataframe, aggregate, executeParam(aggregate), null, null, null);

        assertNull(dataframe.getRows().get(0).get(2));
        assertEquals(0.5d, dataframe.getRows().get(1).get(2));
        assertNull(dataframe.getRows().get(2).get(2));
        assertNull(dataframe.getRows().get(3).get(2));
        assertEquals(aggregate.getCalc(), dataframe.getColumns().get(2).getCalc());
    }

    @Test
    void calculatesPreviousYearDifference() throws Exception {
        AggregateOperator aggregate = aggregate("SUM(amount)-year", "year", "diff");
        Dataframe dataframe = dataframe(Arrays.asList(
                row("east", "2023-02", 100),
                row("east", "2024-02", 130)
        ));

        new DateRatioCalculator().calculate(dataframe, aggregate, executeParam(aggregate), null, null, null);

        assertNull(dataframe.getRows().get(0).get(2));
        assertEquals(30d, dataframe.getRows().get(1).get(2));
    }

    @Test
    void loadsUnpagedHistoryWhenPreviousPeriodIsOutsideCurrentPage() throws Exception {
        AggregateOperator aggregate = aggregate("SUM(amount)-ratio", "last", "percent");
        ExecuteParam param = executeParam(aggregate);
        param.setPageInfo(PageInfo.builder().pageNo(2).pageSize(1).countTotal(true).total(2).build());
        Dataframe page = dataframe(Collections.singletonList(row("east", "2024-2", 150)));
        DataProvider provider = mock(DataProvider.class);
        List<ExecuteParam> requests = new ArrayList<>();
        when(provider.execute(isNull(), isNull(), any(ExecuteParam.class))).thenAnswer(invocation -> {
            requests.add(invocation.getArgument(2));
            return dataframe("SUM(amount)-ratio", Arrays.asList(
                    row("east", "2024-1", 100),
                    row("east", "2024-2", 150)));
        });

        new DateRatioCalculator().calculate(page, aggregate, param, null, null, provider);

        assertEquals(0.5d, page.getRows().get(0).get(2));
        assertEquals(1, requests.size());
        assertEquals(1, requests.get(0).getPageInfo().getPageNo());
        assertEquals(DateRatioCalculator.MAX_SUPPLEMENTARY_ROWS,
                requests.get(0).getPageInfo().getPageSize());
        assertEquals(false, requests.get(0).getPageInfo().isCountTotal());
        assertEquals(2, param.getPageInfo().getPageNo());
        assertEquals(2, param.getPageInfo().getTotal());
    }

    @Test
    void queriesSelectedAndPreviousPeriodsWithoutMutatingOriginalFilters() throws Exception {
        AggregateOperator aggregate = aggregate("SUM(amount)-selected", "last", "percent");
        Map<String, Object> config = config(aggregate);
        config.put("column", Collections.singletonList("created_at"));
        config.put("select", "2024-02");

        ExecuteParam param = executeParam(aggregate);
        param.setGroups(Collections.singletonList(param.getGroups().get(0)));
        param.setFunctionColumns(Collections.emptyList());
        FilterOperator originalFilter = new FilterOperator();
        originalFilter.setColumn("created_at");
        originalFilter.setSqlOperator(FilterOperator.SqlOperator.EQ);
        originalFilter.setValues(new SingleTypedValue[]{
                new SingleTypedValue("2024-02-15", ValueType.DATE)
        });
        param.setFilters(Collections.singletonList(originalFilter));
        param.setPageInfo(PageInfo.builder().pageNo(3).pageSize(10).countTotal(true).total(77).build());

        DataProvider provider = mock(DataProvider.class);
        List<ExecuteParam> requests = new ArrayList<>();
        when(provider.execute(isNull(), isNull(), any(ExecuteParam.class))).thenAnswer(invocation -> {
            ExecuteParam request = invocation.getArgument(2);
            requests.add(request);
            String start = String.valueOf(request.getFilters().stream()
                    .filter(filter -> filter.getSqlOperator() == FilterOperator.SqlOperator.GTE)
                    .findFirst().orElseThrow(AssertionError::new).getValues()[0].getValue());
            double value = "2024-02-01".equals(start) ? 150d : 100d;
            return dataframe("SUM(amount)-selected", Collections.singletonList(row("east", value)));
        });

        Dataframe result = dataframe("SUM(amount)-selected", Collections.singletonList(row("east", 0d)));
        new DateRatioCalculator().calculate(result, aggregate, param, null, null, provider);

        assertEquals(0.5d, result.getRows().get(0).get(1));
        assertEquals(2, requests.size());
        assertEquals(1, param.getFilters().size());
        assertEquals(FilterOperator.SqlOperator.EQ, param.getFilters().get(0).getSqlOperator());
        assertNotSame(param.getFilters(), requests.get(0).getFilters());
        assertEquals(2, requests.get(0).getFilters().size());
        assertEquals(2, requests.get(1).getFilters().size());
        assertNotSame(param.getPageInfo(), requests.get(0).getPageInfo());
        assertEquals(1, requests.get(0).getPageInfo().getPageNo());
        assertEquals(DateRatioCalculator.MAX_SUPPLEMENTARY_ROWS,
                requests.get(0).getPageInfo().getPageSize());
        assertEquals(false, requests.get(0).getPageInfo().isCountTotal());
        assertEquals(3, param.getPageInfo().getPageNo());
        assertEquals(77, param.getPageInfo().getTotal());
    }

    @Test
    void factoryReturnsIndependentCalculatorInstances() {
        CalculatorFactory factory = new CalculatorFactory(Collections.singletonList(new DateRatioCalculator()));

        assertNotSame(factory.create("dateRatio"), factory.create("dateRatio"));
    }

    @Test
    void rejectsOversizedSupplementaryResults() throws Exception {
        AggregateOperator aggregate = aggregate("SUM(amount)-ratio", "last", "percent");
        ExecuteParam param = executeParam(aggregate);
        Dataframe page = dataframe(Collections.singletonList(row("east", "2024-02", 150)));
        DataProvider provider = mock(DataProvider.class);
        Dataframe oversized = dataframe("SUM(amount)-ratio", Collections.nCopies(
                DateRatioCalculator.MAX_SUPPLEMENTARY_ROWS + 1,
                row("east", "2024-01", 100)));
        when(provider.execute(isNull(), isNull(), any(ExecuteParam.class))).thenReturn(oversized);

        DateRatioCalculator calculator = new DateRatioCalculator();
        assertThrows(IllegalStateException.class,
                () -> calculator.calculate(page, aggregate, param, null, null, provider));
    }

    private AggregateOperator aggregate(String alias, String ratioType, String valueType) {
        Map<String, Object> config = new HashMap<>();
        config.put("snippet", "AGG_DATE_MONTH(created_at)");
        config.put("ratioType", ratioType);
        config.put("valueType", valueType);
        Calc calc = new Calc();
        calc.setKey(alias);
        calc.setType("dateRatio");
        calc.setConfig(config);
        AggregateOperator aggregate = new AggregateOperator();
        aggregate.setAlias(alias);
        aggregate.setSqlOperator(AggregateOperator.SqlOperator.SUM);
        aggregate.setColumn("amount");
        aggregate.setCalc(calc);
        return aggregate;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> config(AggregateOperator aggregate) {
        return (Map<String, Object>) aggregate.getCalc().getConfig();
    }

    private ExecuteParam executeParam(AggregateOperator aggregate) {
        GroupByOperator region = new GroupByOperator();
        region.setAlias("region");
        region.setColumn("region");
        GroupByOperator month = new GroupByOperator();
        month.setAlias("month");
        month.setColumn("created_at");
        FunctionColumn function = new FunctionColumn();
        function.setAlias("month");
        function.setSnippet("AGG_DATE_MONTH(created_at)");
        ExecuteParam param = new ExecuteParam();
        param.setGroups(Arrays.asList(region, month));
        param.setFunctionColumns(Collections.singletonList(function));
        param.setAggregators(Collections.singletonList(aggregate));
        return param;
    }

    private Dataframe dataframe(List<List<Object>> rows) {
        return dataframe(rows.size() == 2 ? "SUM(amount)-year" : "SUM(amount)-ratio", rows);
    }

    private Dataframe dataframe(String alias, List<List<Object>> rows) {
        Dataframe dataframe = new Dataframe();
        dataframe.setColumns(rows.get(0).size() == 3
                ? Arrays.asList(
                Column.of(ValueType.STRING, "region"),
                Column.of(ValueType.STRING, "month"),
                Column.of(ValueType.NUMERIC, alias))
                : Arrays.asList(
                Column.of(ValueType.STRING, "region"),
                Column.of(ValueType.NUMERIC, alias)));
        dataframe.setRows(new ArrayList<>(rows));
        return dataframe;
    }

    private List<Object> row(Object... values) {
        return new ArrayList<>(Arrays.asList(values));
    }
}

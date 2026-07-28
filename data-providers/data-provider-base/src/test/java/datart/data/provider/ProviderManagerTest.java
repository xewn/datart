package datart.data.provider;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.SelectColumn;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.Calc;
import datart.core.data.provider.sql.FunctionColumn;
import datart.core.data.provider.sql.GroupByOperator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProviderManagerTest {

    private final ProviderManager providerManager = new ProviderManager();

    @Test
    void leavesRowsUnchangedForUnrestrictedPermissions() {
        Dataframe dataframe = dataframe("id");

        excludeColumns(dataframe, null);
        assertEquals(Collections.singletonList("id-value"), dataframe.getRows().get(0));

        excludeColumns(dataframe, Collections.singleton(SelectColumn.of(null, "*")));
        assertEquals(Collections.singletonList("id-value"), dataframe.getRows().get(0));
    }

    @Test
    void deniesEveryColumnForAnEmptyPermissionSet() {
        Dataframe dataframe = dataframe("id", "name");

        excludeColumns(dataframe, Collections.emptySet());

        assertEquals(Arrays.asList(null, null), dataframe.getRows().get(0));
    }

    @Test
    void matchesPlainAliasAndAggregateColumnsExactly() {
        Dataframe dataframe = dataframe("id", "order_id", "SUM(id)", "COUNT(DISTINCT id)", "display_id");
        Set<SelectColumn> include = new HashSet<>();
        include.add(SelectColumn.of("display_id", "id"));

        excludeColumns(dataframe, include);

        assertEquals(
                Arrays.asList("id-value", null, "SUM(id)-value", "COUNT(DISTINCT id)-value", "display_id-value"),
                dataframe.getRows().get(0));
    }

    @Test
    void treatsRegexMetacharactersInColumnKeysLiterally() {
        Dataframe dataframe = dataframe("price[usd]", "SUM(price[usd])", "priceu");

        excludeColumns(dataframe, Collections.singleton(SelectColumn.of(null, "price[usd]")));

        assertEquals(
                Arrays.asList("price[usd]-value", "SUM(price[usd])-value", null),
                dataframe.getRows().get(0));
    }

    @Test
    void calculationKeyIncludesStableSortedConfiguration() {
        ExecuteParam percent = executeParam(calcConfig("ratioType", "last", "valueType", "percent"));
        ExecuteParam difference = executeParam(calcConfig("ratioType", "last", "valueType", "diff"));
        ExecuteParam reordered = executeParam(calcConfig("valueType", "percent", "ratioType", "last"));

        assertNotEquals(ProviderManager.calculationKey(percent), ProviderManager.calculationKey(difference));
        assertEquals(ProviderManager.calculationKey(percent), ProviderManager.calculationKey(reordered));
    }

    @Test
    void reusesDateRatioHistoryAcrossMetricsInOneRequest() throws Exception {
        AggregateOperator first = aggregate("SUM(amount)-first",
                calcConfig("ratioType", "last", "valueType", "percent"));
        AggregateOperator second = aggregate("SUM(amount)-second",
                calcConfig("ratioType", "last", "valueType", "percent"));
        GroupByOperator month = new GroupByOperator();
        month.setAlias("month");
        month.setColumn("created_at");
        FunctionColumn function = new FunctionColumn();
        function.setAlias("month");
        function.setSnippet("AGG_DATE_MONTH(created_at)");
        ExecuteParam param = new ExecuteParam();
        param.setAggregators(Arrays.asList(first, second));
        param.setGroups(Collections.singletonList(month));
        param.setFunctionColumns(Collections.singletonList(function));

        Dataframe page = dataframeWithValues(
                Arrays.asList("month", "SUM(amount)-first", "SUM(amount)-second"),
                Collections.singletonList(Arrays.asList("2024-02", 150, 300)));
        Dataframe history = dataframeWithValues(
                Arrays.asList("month", "SUM(amount)-first", "SUM(amount)-second"),
                Arrays.asList(Arrays.asList("2024-01", 100, 200),
                        Arrays.asList("2024-02", 150, 300)));
        DataProvider provider = mock(DataProvider.class);
        when(provider.execute(isNull(), isNull(), any(ExecuteParam.class))).thenReturn(history);

        ReflectionTestUtils.invokeMethod(providerManager, "applyCalculations",
                page, null, null, param, provider);

        verify(provider, times(1)).execute(isNull(), isNull(), any(ExecuteParam.class));
        assertEquals(Arrays.asList("2024-02", 0.5d, 0.5d), page.getRows().get(0));
    }

    private ExecuteParam executeParam(Map<String, Object> config) {
        AggregateOperator aggregate = aggregate("SUM(amount)-ratio", config);
        ExecuteParam param = new ExecuteParam();
        param.setAggregators(Collections.singletonList(aggregate));
        return param;
    }

    private AggregateOperator aggregate(String alias, Map<String, Object> config) {
        Calc calc = new Calc();
        calc.setKey(alias);
        calc.setType("dateRatio");
        calc.setConfig(config);
        AggregateOperator aggregate = new AggregateOperator();
        aggregate.setAlias(alias);
        aggregate.setColumn("amount");
        aggregate.setSqlOperator(AggregateOperator.SqlOperator.SUM);
        aggregate.setCalc(calc);
        return aggregate;
    }

    private Map<String, Object> calcConfig(String firstKey, String firstValue,
                                           String secondKey, String secondValue) {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put(firstKey, firstValue);
        config.put(secondKey, secondValue);
        return config;
    }

    private void excludeColumns(Dataframe dataframe, Set<SelectColumn> include) {
        ReflectionTestUtils.invokeMethod(providerManager, "excludeColumns", dataframe, include);
    }

    private Dataframe dataframe(String... keys) {
        Dataframe dataframe = new Dataframe();
        List<Column> columns = new ArrayList<>();
        List<Object> row = new ArrayList<>();
        for (String key : keys) {
            columns.add(Column.of(ValueType.STRING, key));
            row.add(key + "-value");
        }
        dataframe.setColumns(columns);
        dataframe.setRows(Collections.singletonList(row));
        return dataframe;
    }

    private Dataframe dataframeWithValues(List<String> keys, List<List<Object>> rows) {
        Dataframe dataframe = new Dataframe();
        List<Column> columns = new ArrayList<>();
        for (String key : keys) {
            columns.add(Column.of(ValueType.STRING, key));
        }
        dataframe.setColumns(columns);
        dataframe.setRows(new ArrayList<>(rows));
        return dataframe;
    }
}

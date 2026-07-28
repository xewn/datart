package datart.data.provider;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.SelectColumn;
import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.Calc;
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

    private ExecuteParam executeParam(Map<String, Object> config) {
        Calc calc = new Calc();
        calc.setKey("ratio");
        calc.setType("dateRatio");
        calc.setConfig(config);
        AggregateOperator aggregate = new AggregateOperator();
        aggregate.setAlias("SUM(amount)-ratio");
        aggregate.setColumn("amount");
        aggregate.setSqlOperator(AggregateOperator.SqlOperator.SUM);
        aggregate.setCalc(calc);
        ExecuteParam param = new ExecuteParam();
        param.setAggregators(Collections.singletonList(aggregate));
        return param;
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
}

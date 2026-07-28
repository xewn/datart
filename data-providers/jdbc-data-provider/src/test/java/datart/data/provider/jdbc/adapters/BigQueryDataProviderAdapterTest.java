package datart.data.provider.jdbc.adapters;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.sql.AggregateOperator;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BigQueryDataProviderAdapterTest {

    @Test
    void readsBigQueryDatasetsFromJdbcSchemas() throws Exception {
        Connection connection = mock(Connection.class);
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        ResultSet schemas = mock(ResultSet.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getSchemas()).thenReturn(schemas);
        when(schemas.next()).thenReturn(true, false);
        when(schemas.getString(1)).thenReturn("analytics");

        BigQueryDataProviderAdapter adapter = new TestBigQueryAdapter(connection);

        Set<String> databases = adapter.readAllDatabases();

        assertEquals(1, databases.size());
        assertEquals("analytics", databases.iterator().next());
        verify(metadata).getSchemas();
    }

    @Test
    void temporarilyReplacesInvalidAggregateAliasesAndRestoresResultColumns() {
        AggregateOperator sum = aggregate("SUM(cnt)");
        AggregateOperator count = aggregate("COUNT(cnt)");
        AggregateOperator valid = aggregate("already_valid");
        ExecuteParam executeParam = ExecuteParam.builder()
                .aggregators(Arrays.asList(sum, count, valid))
                .build();

        Map<String, String> aliases = BigQueryDataProviderAdapter.useSafeAggregateAliases(executeParam);

        assertEquals("DATART_AGG_0", sum.getAlias());
        assertEquals("DATART_AGG_1", count.getAlias());
        assertEquals("already_valid", valid.getAlias());

        Dataframe dataframe = new Dataframe();
        dataframe.setColumns(Arrays.asList(
                Column.of(ValueType.NUMERIC, "DATART_AGG_0"),
                Column.of(ValueType.NUMERIC, "DATART_AGG_1"),
                Column.of(ValueType.NUMERIC, "already_valid")
        ));
        BigQueryDataProviderAdapter.restoreAggregateAliases(executeParam, aliases, dataframe);

        assertEquals("SUM(cnt)", sum.getAlias());
        assertEquals("COUNT(cnt)", count.getAlias());
        assertEquals("already_valid", valid.getAlias());
        assertEquals("SUM(cnt)", dataframe.getColumns().get(0).columnName());
        assertEquals("COUNT(cnt)", dataframe.getColumns().get(1).columnName());
        assertEquals("already_valid", dataframe.getColumns().get(2).columnName());
    }

    private AggregateOperator aggregate(String alias) {
        AggregateOperator operator = new AggregateOperator();
        operator.setAlias(alias);
        return operator;
    }

    private static class TestBigQueryAdapter extends BigQueryDataProviderAdapter {
        private final Connection connection;

        private TestBigQueryAdapter(Connection connection) {
            this.connection = connection;
        }

        @Override
        protected Connection getConn() {
            return connection;
        }
    }
}

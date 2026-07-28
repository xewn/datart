package datart.data.provider.calcite;

import datart.core.data.provider.sql.AggregateOperator;
import datart.core.data.provider.sql.OrderOperator;
import datart.data.provider.calcite.dialect.H2Dialect;
import org.apache.calcite.sql.SqlNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlBuilderTest {

    @Test
    void rendersCountDistinctOrdering() {
        OrderOperator order = new OrderOperator();
        order.setColumn("user_id");
        order.setAggOperator(AggregateOperator.SqlOperator.COUNT_DISTINCT);
        order.setOperator(OrderOperator.SqlOperator.DESC);

        SqlNode orderNode = ReflectionTestUtils.invokeMethod(
                SqlBuilder.builder(), "createOrderNode", order);
        String sql = orderNode.toSqlString(H2Dialect.DEFAULT).getSql();

        assertTrue(sql.contains("COUNT(DISTINCT"), sql);
        assertTrue(sql.endsWith("DESC"), sql);
    }

    @Test
    void rendersEscapedCustomValueOrderingWithUnknownValuesLast() {
        OrderOperator order = new OrderOperator();
        order.setColumn("region");
        order.setOperator(OrderOperator.SqlOperator.CUSTOMIZE);
        order.setValue(Arrays.asList("O'Reilly", "East", "East"));

        SqlNode orderNode = ReflectionTestUtils.invokeMethod(
                SqlBuilder.builder(), "createOrderNode", order);
        String sql = orderNode.toSqlString(H2Dialect.DEFAULT).getSql();

        assertTrue(sql.contains("CASE"), sql);
        assertTrue(sql.contains("'O''Reilly'"), sql);
        assertTrue(sql.contains("'East' THEN 1"), sql);
        assertTrue(sql.contains("ELSE 2"), sql);
    }
}

package datart.data.provider.calcite.dialect;

import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.parser.SqlParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MsSqlStdOperatorSupportTest {

    @Test
    void rendersIsoWeekAndWeekBasedYear() throws Exception {
        SqlNode expression = SqlParser.create("AGG_DATE_WEEK(created_at)").parseExpression();

        String sql = expression.toSqlString(new MsSqlStdOperatorSupport()).getSql();

        assertTrue(sql.contains("DATEPART(isowk"));
        assertTrue(sql.contains("YEAR(DATEADD"));
        assertFalse(sql.contains("DATEPART(ww"));
    }
}

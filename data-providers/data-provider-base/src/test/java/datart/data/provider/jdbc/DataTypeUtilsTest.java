package datart.data.provider.jdbc;

import datart.core.base.consts.ValueType;
import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataTypeUtilsTest {

    @Test
    void mapsKnownAndUnknownJdbcTypes() {
        assertEquals(ValueType.NUMERIC, DataTypeUtils.jdbcType2DataType(Types.INTEGER));
        assertEquals(ValueType.DATE, DataTypeUtils.jdbcType2DataType(Types.TIMESTAMP));
        assertEquals(ValueType.STRING, DataTypeUtils.jdbcType2DataType(Integer.MIN_VALUE));
    }
}

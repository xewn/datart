package datart.data.provider.jdbc;

import com.alibaba.druid.pool.DruidDataSource;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class DataSourceFactoryDruidImplTest {

    @Test
    void keepsRetryingAfterAnAcquireFailure() throws Exception {
        JdbcProperties properties = new JdbcProperties();
        properties.setDbType("h2");
        properties.setUrl("jdbc:h2:mem:druid-recovery");
        properties.setDriverClass("org.h2.Driver");
        properties.setProperties(new Properties());

        DruidDataSource dataSource = new DataSourceFactoryDruidImpl().createDataSource(properties);
        try {
            assertFalse(dataSource.isBreakAfterAcquireFailure());
            assertEquals(3, dataSource.getConnectionErrorRetryAttempts());
        } finally {
            dataSource.close();
        }
    }
}

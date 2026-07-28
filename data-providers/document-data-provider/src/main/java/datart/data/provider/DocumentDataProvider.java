package datart.data.provider;

import datart.core.common.MessageResolver;
import datart.core.data.provider.Column;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.DataProviderConfigTemplate;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.QueryScript;
import datart.data.provider.document.DocumentClientFactory;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public class DocumentDataProvider extends DataProvider {

    private static final String I18N_PREFIX = "config.template.document.";
    static final int MAX_RESULT_ROWS = 100_000;
    private final DocumentClientFactory clients = new DocumentClientFactory();

    @Override
    public Object test(DataProviderSource source) {
        clients.testConnection(source);
        return true;
    }

    @Override
    public Set<String> readAllDatabases(DataProviderSource source) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public Set<String> readTables(DataProviderSource source, String database) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public Set<Column> readTableColumns(DataProviderSource source, String schema, String table) throws SQLException {
        return Collections.emptySet();
    }

    @Override
    public String getConfigDisplayName(String name) {
        return MessageResolver.getMessage(I18N_PREFIX + name);
    }

    @Override
    public String getConfigDescription(String name) {
        String key = I18N_PREFIX + name + ".desc";
        String message = MessageResolver.getMessage(key);
        return key.equals(message) ? null : message;
    }

    @Override
    public String getQueryKey(DataProviderSource source, QueryScript script, ExecuteParam executeParam) {
        int limit = resultLimit(executeParam);
        long offset = resultOffset(executeParam, limit);
        return "Q" + DigestUtils.sha256Hex(String.valueOf(source.getSourceId()) + '\0'
                + script.getScript() + '\0' + offset + '\0' + limit);
    }

    @Override
    public Dataframe execute(DataProviderSource source, QueryScript script, ExecuteParam executeParam) {
        if (script == null || script.getScript() == null) {
            throw new IllegalArgumentException("MongoDB query command is required");
        }
        int limit = resultLimit(executeParam);
        return clients.getClient(source).execute(script.getScript(), resultOffset(executeParam, limit), limit);
    }

    static int resultLimit(ExecuteParam executeParam) {
        if (executeParam == null || executeParam.getPageInfo() == null
                || executeParam.getPageInfo().getPageSize() <= 0) {
            return MAX_RESULT_ROWS;
        }
        return (int) Math.min(executeParam.getPageInfo().getPageSize(), MAX_RESULT_ROWS);
    }

    static long resultOffset(ExecuteParam executeParam, int limit) {
        long pageNo = executeParam == null || executeParam.getPageInfo() == null
                ? 1L : Math.max(1L, executeParam.getPageInfo().getPageNo());
        try {
            return Math.multiplyExact(pageNo - 1L, limit);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Document query page offset is too large", e);
        }
    }

    @Override
    public String getConfigFile() {
        return "document-data-provider.json";
    }

    @Override
    public boolean validateFunction(DataProviderSource source, String snippet) {
        return false;
    }

    @Override
    public void resetSource(DataProviderSource source) {
        clients.reset(source);
    }

    @Override
    public void close() {
        clients.close();
    }

    @Override
    public DataProviderConfigTemplate getConfigTemplate() throws IOException {
        DataProviderConfigTemplate template = super.getConfigTemplate();
        for (DataProviderConfigTemplate.Attribute attribute : template.getAttributes()) {
            attribute.setDisplayName(getConfigDisplayName(attribute.getName()));
            attribute.setDescription(getConfigDescription(attribute.getName()));
            if ("dbType".equals(attribute.getName())) {
                Properties mongo = new Properties();
                mongo.setProperty("dbType", "MONGODB");
                mongo.setProperty("url", "mongodb://localhost:27017/database");
                attribute.setOptions(Collections.<Object>singletonList(mongo));
            }
        }
        return template;
    }
}

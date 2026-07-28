package datart.data.provider.document;

import com.alibaba.fastjson.JSON;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import datart.core.base.consts.ValueType;
import datart.core.data.provider.Column;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MongoDocumentClient implements DocumentClient {

    private final MongoClient client;
    private final MongoDatabase database;

    MongoDocumentClient(DataProviderSource source) {
        String url = String.valueOf(source.getProperties().get("url"));
        ConnectionString connectionString = new ConnectionString(url);
        String databaseName = connectionString.getDatabase();
        if (StringUtils.isBlank(databaseName)) {
            throw new IllegalArgumentException("MongoDB connection URL must include a database name");
        }
        client = MongoClients.create(connectionString);
        database = client.getDatabase(databaseName);
    }

    @Override
    public void ping() {
        database.runCommand(new Document("ping", 1));
    }

    @Override
    public Dataframe execute(String command) {
        Document parsed = parseReadOnlyCommand(command);
        return toDataframe(database.runCommand(parsed), command);
    }

    @Override
    public void close() {
        client.close();
    }

    static Document parseReadOnlyCommand(String command) {
        final Document parsed;
        try {
            parsed = Document.parse(command);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("MongoDB command must be valid JSON", e);
        }
        boolean find = parsed.get("find") instanceof String;
        boolean aggregate = parsed.get("aggregate") instanceof String;
        if (find == aggregate) {
            throw new IllegalArgumentException("Only one read-only find or aggregate command is allowed");
        }
        if (aggregate && containsWriteStage(parsed.get("pipeline"))) {
            throw new IllegalArgumentException("MongoDB aggregate commands cannot contain $out or $merge");
        }
        return parsed;
    }

    private static boolean containsWriteStage(Object value) {
        if (value instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) value;
            if (map.containsKey("$out") || map.containsKey("$merge")) {
                return true;
            }
            for (Object nested : map.values()) {
                if (containsWriteStage(nested)) {
                    return true;
                }
            }
        } else if (value instanceof Iterable) {
            for (Object nested : (Iterable<?>) value) {
                if (containsWriteStage(nested)) {
                    return true;
                }
            }
        }
        return false;
    }

    static Dataframe toDataframe(Document response, String script) {
        List<Document> batch = firstBatch(response);
        LinkedHashMap<String, ValueType> columnTypes = new LinkedHashMap<>();
        for (Document row : batch) {
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                Object value = entry.getValue();
                ValueType inferred = value == null ? null : inferType(value);
                ValueType current = columnTypes.get(entry.getKey());
                if (!columnTypes.containsKey(entry.getKey()) || current == null) {
                    columnTypes.put(entry.getKey(), inferred);
                } else if (inferred != null && current != inferred) {
                    columnTypes.put(entry.getKey(), ValueType.STRING);
                }
            }
        }

        List<Column> columns = new ArrayList<>();
        for (Map.Entry<String, ValueType> entry : columnTypes.entrySet()) {
            ValueType type = entry.getValue() == null ? ValueType.STRING : entry.getValue();
            columns.add(Column.of(type, entry.getKey()));
        }
        List<List<Object>> rows = new ArrayList<>();
        for (Document document : batch) {
            List<Object> row = new ArrayList<>();
            for (String columnName : columnTypes.keySet()) {
                row.add(normalize(document.get(columnName)));
            }
            rows.add(row);
        }

        Dataframe dataframe = new Dataframe();
        dataframe.setColumns(columns);
        dataframe.setRows(rows);
        dataframe.setScript(script);
        return dataframe;
    }

    private static List<Document> firstBatch(Document response) {
        if (response == null) {
            return Collections.emptyList();
        }
        Document cursor = response.get("cursor", Document.class);
        if (cursor == null) {
            return Collections.emptyList();
        }
        List<Document> batch = cursor.getList("firstBatch", Document.class);
        return batch == null ? Collections.emptyList() : batch;
    }

    private static ValueType inferType(Object value) {
        if (value instanceof Date) {
            return ValueType.DATE;
        }
        if (value instanceof Number || value instanceof Decimal128) {
            return ValueType.NUMERIC;
        }
        if (value instanceof Boolean) {
            return ValueType.BOOLEAN;
        }
        return ValueType.STRING;
    }

    private static Object normalize(Object value) {
        if (value instanceof Document || value instanceof List || value instanceof Map) {
            return JSON.toJSONString(value);
        }
        if (value instanceof ObjectId) {
            return ((ObjectId) value).toHexString();
        }
        if (value instanceof Decimal128) {
            return ((Decimal128) value).bigDecimalValue();
        }
        return value;
    }
}

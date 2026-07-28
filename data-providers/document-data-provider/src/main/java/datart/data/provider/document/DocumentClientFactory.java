package datart.data.provider.document;

import datart.core.data.provider.DataProviderSource;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DocumentClientFactory implements AutoCloseable {

    interface ClientCreator {
        DocumentClient create(DataProviderSource source);
    }

    private final Map<String, ClientHolder> clients = new ConcurrentHashMap<>();
    private final ClientCreator clientCreator;

    public DocumentClientFactory() {
        this(MongoDocumentClient::new);
    }

    DocumentClientFactory(ClientCreator clientCreator) {
        this.clientCreator = clientCreator;
    }

    public DocumentClient getClient(DataProviderSource source) {
        validate(source);
        String fingerprint = fingerprint(source);
        String identity = StringUtils.defaultIfBlank(source.getSourceId(), "anonymous:" + fingerprint);
        ClientHolder holder = clients.compute(identity, (key, current) -> {
            if (current != null && current.fingerprint.equals(fingerprint)) {
                return current;
            }
            close(current);
            return new ClientHolder(fingerprint, clientCreator.create(source));
        });
        return holder.client;
    }

    public void testConnection(DataProviderSource source) {
        validate(source);
        DocumentClient client = clientCreator.create(source);
        try {
            client.ping();
        } finally {
            client.close();
        }
    }

    public void reset(DataProviderSource source) {
        if (source == null) {
            return;
        }
        String sourceId = source.getSourceId();
        if (StringUtils.isBlank(sourceId)) {
            close(clients.remove("anonymous:" + fingerprint(source)));
        } else {
            close(clients.remove(sourceId));
        }
    }

    @Override
    public void close() {
        clients.values().forEach(DocumentClientFactory::close);
        clients.clear();
    }

    private static void validate(DataProviderSource source) {
        if (source == null || source.getProperties() == null) {
            throw new IllegalArgumentException("Document data source properties are required");
        }
        String dbType = String.valueOf(source.getProperties().get("dbType"));
        if (!"MONGODB".equalsIgnoreCase(dbType)) {
            throw new IllegalArgumentException("Unsupported document database type: " + dbType);
        }
        String url = String.valueOf(source.getProperties().get("url"));
        if (StringUtils.isBlank(url) || "null".equals(url)) {
            throw new IllegalArgumentException("MongoDB connection URL is required");
        }
    }

    private static String fingerprint(DataProviderSource source) {
        Object dbType = source.getProperties().get("dbType");
        Object url = source.getProperties().get("url");
        return DigestUtils.sha256Hex(String.valueOf(dbType) + '\0' + String.valueOf(url));
    }

    private static void close(ClientHolder holder) {
        if (holder != null) {
            holder.client.close();
        }
    }

    private static class ClientHolder {
        private final String fingerprint;
        private final DocumentClient client;

        private ClientHolder(String fingerprint, DocumentClient client) {
            this.fingerprint = fingerprint;
            this.client = client;
        }
    }
}

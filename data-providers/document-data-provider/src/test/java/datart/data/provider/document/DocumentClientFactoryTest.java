package datart.data.provider.document;

import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.Dataframe;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentClientFactoryTest {

    @Test
    void isolatesSourcesAndClosesReplacedOrResetClients() {
        List<FakeClient> created = new ArrayList<>();
        DocumentClientFactory factory = new DocumentClientFactory(source -> {
            FakeClient client = new FakeClient();
            created.add(client);
            return client;
        });
        DataProviderSource first = source("source-1", "mongodb://localhost/one");
        DataProviderSource second = source("source-2", "mongodb://localhost/two");

        DocumentClient firstClient = factory.getClient(first);
        assertSame(firstClient, factory.getClient(first));
        DocumentClient secondClient = factory.getClient(second);
        assertNotSame(firstClient, secondClient);

        first.getProperties().put("url", "mongodb://localhost/changed");
        DocumentClient replacement = factory.getClient(first);
        assertNotSame(firstClient, replacement);
        assertTrue(created.get(0).closed);
        assertFalse(created.get(1).closed);

        factory.reset(first);
        assertTrue(created.get(2).closed);
        factory.close();
        assertTrue(created.get(1).closed);
    }

    @Test
    void testsConnectionsWithoutCachingClients() {
        List<FakeClient> created = new ArrayList<>();
        DocumentClientFactory factory = new DocumentClientFactory(source -> {
            FakeClient client = new FakeClient();
            created.add(client);
            return client;
        });
        DataProviderSource source = source(null, "mongodb://localhost/test");

        factory.testConnection(source);
        DocumentClient cached = factory.getClient(source);

        assertTrue(created.get(0).closed);
        assertNotSame(created.get(0), cached);
        assertFalse(created.get(1).closed);
        factory.close();
    }

    private DataProviderSource source(String id, String url) {
        DataProviderSource source = new DataProviderSource();
        source.setSourceId(id);
        HashMap<String, Object> properties = new HashMap<>();
        properties.put("dbType", "MONGODB");
        properties.put("url", url);
        source.setProperties(properties);
        return source;
    }

    private static class FakeClient implements DocumentClient {
        private boolean closed;

        @Override
        public void ping() {
        }

        @Override
        public Dataframe execute(String command, long offset, int maxRows) {
            return new Dataframe();
        }

        @Override
        public void close() {
            closed = true;
        }
    }
}

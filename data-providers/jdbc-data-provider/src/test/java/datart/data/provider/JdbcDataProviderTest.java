package datart.data.provider;

import datart.core.data.provider.DataProviderSource;
import datart.data.provider.jdbc.JdbcProperties;
import datart.data.provider.jdbc.adapters.JdbcDataProviderAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JdbcDataProviderTest {

    @Test
    void reusesAdapterWhenConnectionPropertiesAreUnchanged() {
        TestJdbcDataProvider provider = new TestJdbcDataProvider();
        JdbcDataProviderAdapter adapter = adapter("original", provider.events);
        provider.adapters.add(adapter);
        DataProviderSource source = source("jdbc:h2:mem:one");

        assertSame(adapter, provider.match(source));
        assertSame(adapter, provider.match(source));

        assertEquals(1, provider.createCount.get());
        verify(adapter, never()).close();
    }

    @Test
    void createsReplacementBeforeClosingChangedAdapter() {
        TestJdbcDataProvider provider = new TestJdbcDataProvider();
        JdbcDataProviderAdapter original = adapter("original", provider.events);
        JdbcDataProviderAdapter replacement = adapter("replacement", provider.events);
        provider.adapters.add(original);
        provider.adapters.add(replacement);
        provider.match(source("jdbc:h2:mem:one"));
        provider.events.clear();

        JdbcDataProviderAdapter result = provider.match(source("jdbc:h2:mem:two"));

        assertSame(replacement, result);
        assertEquals(
                asList("create:jdbc:h2:mem:two", "close:original"),
                provider.events);
        verify(original).close();
        verify(replacement, never()).close();
    }

    @Test
    void preservesWorkingAdapterWhenReplacementCreationFails() {
        TestJdbcDataProvider provider = new TestJdbcDataProvider();
        JdbcDataProviderAdapter original = adapter("original", provider.events);
        provider.adapters.add(original);
        DataProviderSource originalSource = source("jdbc:h2:mem:one");
        provider.match(originalSource);
        provider.failNextCreation.set(true);

        assertThrows(IllegalStateException.class,
                () -> provider.match(source("jdbc:h2:mem:two")));

        assertSame(original, provider.match(originalSource));
        verify(original, never()).close();
    }

    @Test
    void createsOneReplacementForConcurrentRequests() throws Exception {
        TestJdbcDataProvider provider = new TestJdbcDataProvider();
        JdbcDataProviderAdapter original = adapter("original", provider.events);
        JdbcDataProviderAdapter replacement = adapter("replacement", provider.events);
        provider.adapters.add(original);
        provider.adapters.add(replacement);
        provider.match(source("jdbc:h2:mem:one"));
        DataProviderSource changedSource = source("jdbc:h2:mem:two");

        int workerCount = 8;
        ExecutorService executor = Executors.newFixedThreadPool(workerCount);
        CountDownLatch ready = new CountDownLatch(workerCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<JdbcDataProviderAdapter>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < workerCount; i++) {
                futures.add(executor.submit(() -> {
                    ready.countDown();
                    start.await();
                    return provider.match(changedSource);
                }));
            }
            ready.await();
            start.countDown();
            for (Future<JdbcDataProviderAdapter> future : futures) {
                assertSame(replacement, future.get());
            }
        } finally {
            executor.shutdownNow();
        }

        assertEquals(2, provider.createCount.get());
        verify(original).close();
        verify(replacement, never()).close();
    }

    private JdbcDataProviderAdapter adapter(String name, List<String> events) {
        JdbcDataProviderAdapter adapter = mock(JdbcDataProviderAdapter.class);
        doAnswer(invocation -> {
            events.add("close:" + name);
            return null;
        }).when(adapter).close();
        return adapter;
    }

    private DataProviderSource source(String url) {
        DataProviderSource source = new DataProviderSource();
        source.setSourceId("source-1");
        source.setName("source");
        Map<String, Object> properties = new HashMap<>();
        properties.put(JdbcDataProvider.DB_TYPE, "H2");
        properties.put(JdbcDataProvider.URL, url);
        properties.put(JdbcDataProvider.DRIVER_CLASS, "org.h2.Driver");
        source.setProperties(properties);
        return source;
    }

    private List<String> asList(String... values) {
        List<String> result = new ArrayList<>();
        Collections.addAll(result, values);
        return result;
    }

    private static class TestJdbcDataProvider extends JdbcDataProvider {

        private final Queue<JdbcDataProviderAdapter> adapters = new ConcurrentLinkedQueue<>();
        private final AtomicInteger createCount = new AtomicInteger();
        private final AtomicBoolean failNextCreation = new AtomicBoolean();
        private final List<String> events = Collections.synchronizedList(new ArrayList<>());

        @Override
        protected JdbcDataProviderAdapter createDataProvider(JdbcProperties jdbcProperties) {
            createCount.incrementAndGet();
            events.add("create:" + jdbcProperties.getUrl());
            if (failNextCreation.compareAndSet(true, false)) {
                throw new IllegalStateException("creation failed");
            }
            JdbcDataProviderAdapter adapter = adapters.remove();
            when(adapter.getJdbcProperties()).thenReturn(jdbcProperties);
            return adapter;
        }

        private JdbcDataProviderAdapter match(DataProviderSource source) {
            return ReflectionTestUtils.invokeMethod(this, "matchProviderAdapter", source);
        }
    }
}

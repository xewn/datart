package datart.data.provider;

import datart.core.base.PageInfo;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.ExecuteParam;
import datart.core.data.provider.DataProviderSource;
import datart.core.data.provider.QueryScript;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class DocumentDataProviderTest {

    @Test
    void isDiscoverableThroughDataProviderSpi() {
        boolean discovered = false;
        for (DataProvider provider : ServiceLoader.load(DataProvider.class)) {
            if (provider instanceof DocumentDataProvider) {
                discovered = true;
                break;
            }
        }
        assertTrue(discovered);
    }

    @Test
    void capsDocumentResultsByRequestedPageAndHardLimit() {
        ExecuteParam requested = new ExecuteParam();
        requested.setPageInfo(PageInfo.builder().pageSize(25).build());
        ExecuteParam oversized = new ExecuteParam();
        oversized.setPageInfo(PageInfo.builder().pageSize(Integer.MAX_VALUE).build());

        assertEquals(25, DocumentDataProvider.resultLimit(requested));
        assertEquals(DocumentDataProvider.MAX_RESULT_ROWS,
                DocumentDataProvider.resultLimit(oversized));
        assertEquals(DocumentDataProvider.MAX_RESULT_ROWS,
                DocumentDataProvider.resultLimit(null));
        assertEquals(50, DocumentDataProvider.resultOffset(page(3, 25), 25));
    }

    @Test
    void queryKeyIncludesEffectivePaginationWindow() {
        DocumentDataProvider provider = new DocumentDataProvider();
        DataProviderSource source = new DataProviderSource();
        source.setSourceId("source-1");
        QueryScript script = QueryScript.builder().script("{\"find\":\"people\"}").build();

        assertNotEquals(provider.getQueryKey(source, script, page(1, 25)),
                provider.getQueryKey(source, script, page(2, 25)));
        assertNotEquals(provider.getQueryKey(source, script, page(1, 25)),
                provider.getQueryKey(source, script, page(1, 50)));
    }

    private ExecuteParam page(long pageNo, long pageSize) {
        ExecuteParam param = new ExecuteParam();
        param.setPageInfo(PageInfo.builder().pageNo(pageNo).pageSize(pageSize).build());
        return param;
    }
}

package datart.data.provider;

import datart.core.base.PageInfo;
import datart.core.data.provider.DataProvider;
import datart.core.data.provider.ExecuteParam;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    }
}

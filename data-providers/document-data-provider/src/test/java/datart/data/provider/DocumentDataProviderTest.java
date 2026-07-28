package datart.data.provider;

import datart.core.data.provider.DataProvider;
import org.junit.jupiter.api.Test;

import java.util.ServiceLoader;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
}

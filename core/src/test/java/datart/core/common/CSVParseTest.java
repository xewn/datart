package datart.core.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CSVParseTest {

    @TempDir
    Path tempDir;

    @Test
    void trimsSurroundingWhitespaceFromFields() throws Exception {
        Path csv = writeCsv("trim.csv", "name,amount", " Alice , 42 ");

        List<List<Object>> rows = CSVParse.create(csv.toString()).parse();

        assertEquals(Arrays.asList("Alice", "42"), rows.get(1));
    }

    @Test
    void preservesWhitespaceInsideFields() throws Exception {
        Path csv = writeCsv("internal-space.csv", "name", "Alice Smith");

        List<List<Object>> rows = CSVParse.create(csv.toString()).parse();

        assertEquals("Alice Smith", rows.get(1).get(0));
    }

    private Path writeCsv(String name, String... lines) throws Exception {
        Path csv = tempDir.resolve(name);
        return Files.write(csv, Arrays.asList(lines), StandardCharsets.UTF_8);
    }
}

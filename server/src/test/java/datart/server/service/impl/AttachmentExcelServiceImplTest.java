package datart.server.service.impl;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttachmentExcelServiceImplTest {

    private final AttachmentExcelServiceImpl service = new AttachmentExcelServiceImpl(null);

    @Test
    void createsSafeUniqueSheetNames() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            assertEquals("Sales", addSheet(workbook, "Sales", 0));
            assertEquals("Sales (2)", addSheet(workbook, "Sales", 1));
            assertEquals("Sales (3)", addSheet(workbook, "Sales", 2));
            assertEquals("sales (4)", addSheet(workbook, "sales", 3));

            String invalid = addSheet(workbook, "[Budget]/2026*?", 4);
            assertFalse(invalid.matches(".*[\\\\/?*\\[\\]:].*"), invalid);

            assertEquals("Sheet5", addSheet(workbook, "   ", 5));

            String longName = "A very long worksheet name that exceeds thirty one characters";
            String firstLongName = addSheet(workbook, longName, 6);
            String secondLongName = addSheet(workbook, longName, 7);
            assertTrue(firstLongName.length() <= 31, firstLongName);
            assertTrue(secondLongName.length() <= 31, secondLongName);

            Set<String> uniqueNames = new HashSet<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                String sheetName = workbook.getSheetName(i);
                assertTrue(uniqueNames.add(sheetName.toLowerCase(Locale.ROOT)), sheetName);
                assertTrue(sheetName.length() <= 31, sheetName);
            }
        }
    }

    private String addSheet(Workbook workbook, String requestedName, int index) {
        String sheetName = service.uniqueSheetName(workbook, requestedName, index);
        workbook.createSheet(sheetName);
        return sheetName;
    }
}

package datart.server.packaging;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionLayoutTest {

    @Test
    void assemblyIncludesContainerFilesAndExecutableLaunchers() throws Exception {
        Path descriptor = Paths.get("src", "main", "resources", "assembly", "assembly.xml");
        Document document = DocumentBuilderFactory.newInstance()
                .newDocumentBuilder()
                .parse(descriptor.toFile());

        assertTrue(hasInclude(document, "docker-compose.yml"));
        assertTrue(hasInclude(document, ".dockerignore"));
        assertEquals("0755", fileSetValue(document, "datart-server.sh", "fileMode"));
        assertEquals("unix", fileSetValue(document, "datart-server.sh", "lineEnding"));
        assertEquals("dos", fileSetValue(document, "datart-server.cmd", "lineEnding"));
    }

    private boolean hasInclude(Document document, String expected) {
        NodeList includes = document.getElementsByTagName("include");
        for (int i = 0; i < includes.getLength(); i++) {
            if (expected.equals(includes.item(i).getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }

    private String fileSetValue(Document document, String include, String tagName) {
        NodeList fileSets = document.getElementsByTagName("fileSet");
        for (int i = 0; i < fileSets.getLength(); i++) {
            Element fileSet = (Element) fileSets.item(i);
            if (containsInclude(fileSet, include)) {
                NodeList values = fileSet.getElementsByTagName(tagName);
                return values.getLength() == 0 ? null : values.item(0).getTextContent().trim();
            }
        }
        return null;
    }

    private boolean containsInclude(Element fileSet, String expected) {
        NodeList includes = fileSet.getElementsByTagName("include");
        for (int i = 0; i < includes.getLength(); i++) {
            Node include = includes.item(i);
            if (expected.equals(include.getTextContent().trim())) {
                return true;
            }
        }
        return false;
    }
}

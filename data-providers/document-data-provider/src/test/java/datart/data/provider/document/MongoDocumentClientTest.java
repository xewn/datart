package datart.data.provider.document;

import datart.core.base.consts.ValueType;
import datart.core.data.provider.Dataframe;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import com.mongodb.client.MongoDatabase;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MongoDocumentClientTest {

    @Test
    void convertsFirstBatchWithStableColumnsAndTypes() {
        Document response = new Document("cursor", new Document("firstBatch", Arrays.asList(
                new Document("name", "Ada").append("age", 36).append("active", true)
                        .append("score", null).append("mixed", "text"),
                new Document("age", 37).append("city", "London")
                        .append("score", 9).append("mixed", 10)
        )));

        Dataframe dataframe = MongoDocumentClient.toDataframe(response, "{\"find\":\"people\"}");

        assertEquals(Arrays.asList("name", "age", "active", "score", "mixed", "city"), Arrays.asList(
                dataframe.getColumns().get(0).columnName(),
                dataframe.getColumns().get(1).columnName(),
                dataframe.getColumns().get(2).columnName(),
                dataframe.getColumns().get(3).columnName(),
                dataframe.getColumns().get(4).columnName(),
                dataframe.getColumns().get(5).columnName()));
        assertEquals(Arrays.asList(ValueType.STRING, ValueType.NUMERIC, ValueType.BOOLEAN,
                        ValueType.NUMERIC, ValueType.STRING, ValueType.STRING),
                Arrays.asList(dataframe.getColumns().get(0).getType(), dataframe.getColumns().get(1).getType(),
                        dataframe.getColumns().get(2).getType(), dataframe.getColumns().get(3).getType(),
                        dataframe.getColumns().get(4).getType(), dataframe.getColumns().get(5).getType()));
        assertEquals(Arrays.asList("Ada", 36, true, null, "text", null), dataframe.getRows().get(0));
        assertEquals(Arrays.asList(null, 37, null, 9, 10, "London"), dataframe.getRows().get(1));
        assertEquals("{\"find\":\"people\"}", dataframe.getScript());
    }

    @Test
    void returnsEmptyDataframeWhenCursorBatchIsMissing() {
        Dataframe dataframe = MongoDocumentClient.toDataframe(new Document("ok", 1), "{}");

        assertEquals(Collections.emptyList(), dataframe.getColumns());
        assertEquals(Collections.emptyList(), dataframe.getRows());
    }

    @Test
    void acceptsReadCommandsAndRejectsWriteCommands() {
        assertEquals("people", MongoDocumentClient.parseReadOnlyCommand("{\"find\":\"people\"}").getString("find"));
        assertThrows(IllegalArgumentException.class,
                () -> MongoDocumentClient.parseReadOnlyCommand("{\"drop\":\"people\"}"));
        assertThrows(IllegalArgumentException.class,
                () -> MongoDocumentClient.parseReadOnlyCommand(
                        "{\"drop\":\"people\",\"find\":\"people\"}"));
        assertThrows(IllegalArgumentException.class,
                () -> MongoDocumentClient.parseReadOnlyCommand(
                        "{\"aggregate\":\"people\",\"pipeline\":[{\"$out\":\"copy\"}],\"cursor\":{}}"));
        assertThrows(IllegalArgumentException.class,
                () -> MongoDocumentClient.parseReadOnlyCommand("not-json"));
    }

    @Test
    void drainsAllCursorBatches() {
        MongoDatabase database = mock(MongoDatabase.class);
        Document command = new Document("find", "people");
        Document getMore = new Document("getMore", 42L).append("collection", "people");
        when(database.runCommand(command)).thenReturn(new Document("cursor", new Document("id", 42L)
                .append("firstBatch", Collections.singletonList(new Document("name", "Ada")))));
        when(database.runCommand(getMore)).thenReturn(new Document("cursor", new Document("id", 0L)
                .append("nextBatch", Collections.singletonList(new Document("name", "Grace")))));

        assertEquals(Arrays.asList(new Document("name", "Ada"), new Document("name", "Grace")),
                MongoDocumentClient.readAllBatches(database, command));
        verify(database).runCommand(getMore);
    }
}

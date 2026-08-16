package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import javax.xml.parsers.DocumentBuilderFactory;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

class CodSettlementMappingTest {
    @Test
    void mapsTableRelationshipsAndColumnContract() throws ReflectiveOperationException {
        Table table = CodSettlement.class.getAnnotation(Table.class);
        assertEquals("CodSettlement", table.name());
        assertEquals(1, table.uniqueConstraints().length);
        assertEquals("UQ_CodSettlement_ShipperShift", table.uniqueConstraints()[0].name());
        assertEquals(java.util.List.of("shipper_id", "shift_id"),
                java.util.List.of(table.uniqueConstraints()[0].columnNames()));

        var id = CodSettlement.class.getDeclaredField("settlementId");
        assertEquals(int.class, id.getType());
        assertTrue(id.isAnnotationPresent(Id.class));
        assertEquals(GenerationType.IDENTITY, id.getAnnotation(GeneratedValue.class).strategy());
        assertColumn("settlementId", int.class, "settlement_id", false, 255, 0, 0);

        assertJoinColumn("shipper", User.class, "shipper_id", false, false);
        assertJoinColumn("shift", WorkShift.class, "shift_id", false, false);
        assertJoinColumn("receivedBy", User.class, "received_by", true, true);

        assertColumn("status", String.class, "status", false, 20, 0, 0);
        assertColumn("expectedAmount", BigDecimal.class, "expected_amount", false, 255, 18, 2);
        assertColumn("submittedAmount", BigDecimal.class, "submitted_amount", false, 255, 18, 2);
        assertColumn("verifiedAmount", BigDecimal.class, "verified_amount", true, 255, 18, 2);
        assertColumn("reason", String.class, "reason", true, 500, 0, 0);
        assertColumn("submittedAt", LocalDateTime.class, "submitted_at", false, 255, 0, 0);
        assertColumn("verifiedAt", LocalDateTime.class, "verified_at", true, 255, 0, 0);
        assertColumn("createdAt", LocalDateTime.class, "created_at", false, 255, 0, 0);
        assertColumn("updatedAt", LocalDateTime.class, "updated_at", false, 255, 0, 0);
    }

    @Test
    void registersEntityInPersistenceUnit() throws Exception {
        NodeList classes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(Path.of("src/main/resources/META-INF/persistence.xml").toFile())
                .getElementsByTagName("class");

        assertTrue(IntStream.range(0, classes.getLength())
                .mapToObj(classes::item)
                .anyMatch(node -> "entity.CodSettlement".equals(node.getTextContent().trim())));
    }

    private static void assertJoinColumn(String fieldName, Class<?> type, String name, boolean nullable,
            boolean optional) throws ReflectiveOperationException {
        var field = CodSettlement.class.getDeclaredField(fieldName);
        JoinColumn column = field.getAnnotation(JoinColumn.class);
        assertEquals(type, field.getType());
        assertEquals(optional, field.getAnnotation(ManyToOne.class).optional());
        assertEquals(name, column.name());
        assertEquals(nullable, column.nullable());
    }

    private static void assertColumn(String fieldName, Class<?> type, String name, boolean nullable,
            int length, int precision, int scale) throws ReflectiveOperationException {
        var field = CodSettlement.class.getDeclaredField(fieldName);
        Column column = field.getAnnotation(Column.class);
        assertEquals(type, field.getType());
        assertEquals(name, column.name());
        assertEquals(nullable, column.nullable());
        assertEquals(length, column.length());
        assertEquals(precision, column.precision());
        assertEquals(scale, column.scale());
        assertFalse(column.unique());
    }
}

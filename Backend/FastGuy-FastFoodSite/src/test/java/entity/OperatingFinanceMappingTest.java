package entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.IntStream;

import javax.xml.parsers.DocumentBuilderFactory;

import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

class OperatingFinanceMappingTest {
    @Test
    void mapsOperatingExpenseContract() throws ReflectiveOperationException {
        assertEquals("OperatingExpense", OperatingExpense.class.getAnnotation(Table.class).name());
        assertId(OperatingExpense.class, "expenseId", "expense_id");
        assertColumn(OperatingExpense.class, "expenseDate", LocalDate.class, "expense_date", false, 255, 0, 0);
        assertEnum(OperatingExpense.class, "category", OperatingExpense.Category.class, "category", 20);
        assertColumn(OperatingExpense.class, "description", String.class, "description", false, 500, 0, 0);
        assertColumn(OperatingExpense.class, "amount", BigDecimal.class, "amount", false, 255, 18, 2);
        assertJoin(OperatingExpense.class, "createdBy", "created_by");
        assertColumn(OperatingExpense.class, "createdAt", LocalDateTime.class, "created_at", false, 255, 0, 0);
        assertColumn(OperatingExpense.class, "updatedAt", LocalDateTime.class, "updated_at", false, 255, 0, 0);
        assertEquals(java.util.Set.of("RENT", "UTILITIES", "SALARY", "MARKETING", "MAINTENANCE", "OTHER"),
                java.util.Arrays.stream(OperatingExpense.Category.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet()));
    }

    @Test
    void mapsFixedAssetContract() throws ReflectiveOperationException {
        assertEquals("FixedAsset", FixedAsset.class.getAnnotation(Table.class).name());
        assertId(FixedAsset.class, "assetId", "asset_id");
        assertColumn(FixedAsset.class, "assetName", String.class, "asset_name", false, 255, 0, 0);
        assertColumn(FixedAsset.class, "acquisitionCost", BigDecimal.class, "acquisition_cost", false, 255, 18, 2);
        assertColumn(FixedAsset.class, "salvageValue", BigDecimal.class, "salvage_value", false, 255, 18, 2);
        assertColumn(FixedAsset.class, "depreciationStartDate", LocalDate.class, "depreciation_start_date", false, 255, 0, 0);
        assertColumn(FixedAsset.class, "usefulLifeMonths", int.class, "useful_life_months", false, 255, 0, 0);
        assertEnum(FixedAsset.class, "status", FixedAsset.Status.class, "status", 20);
        assertColumn(FixedAsset.class, "retiredAt", LocalDateTime.class, "retired_at", true, 255, 0, 0);
        assertJoin(FixedAsset.class, "createdBy", "created_by");
        assertColumn(FixedAsset.class, "createdAt", LocalDateTime.class, "created_at", false, 255, 0, 0);
        assertColumn(FixedAsset.class, "updatedAt", LocalDateTime.class, "updated_at", false, 255, 0, 0);
        assertEquals(java.util.List.of("ACTIVE", "RETIRED"),
                java.util.Arrays.stream(FixedAsset.Status.values()).map(Enum::name).toList());
    }

    @Test
    void registersEntitiesAndDefinesMigrationPolicy() throws Exception {
        NodeList classes = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(Path.of("src/main/resources/META-INF/persistence.xml").toFile()).getElementsByTagName("class");
        for (String name : java.util.List.of("entity.OperatingExpense", "entity.FixedAsset")) {
            assertTrue(IntStream.range(0, classes.getLength()).mapToObj(classes::item)
                    .anyMatch(node -> name.equals(node.getTextContent().trim())), name);
        }
        String migration = Files.readString(Path.of("../../database/migrations/060_operating_finance.sql"));
        String validator = Files.readString(Path.of("../../database/migrations/060_validate.sql"));
        for (String token : java.util.List.of("SET XACT_ABORT ON", "BEGIN TRY", "BEGIN CATCH", "sp_getapplock",
                "Run 000_preflight_history.sql first", "060 schema partially exists", "060 history exists but",
                "CREATE TABLE dbo.OperatingExpense", "CREATE TABLE dbo.FixedAsset", "CK_OperatingExpense_Amount",
                "CK_FixedAsset_Value", "CK_FixedAsset_Retirement", "IX_OperatingExpense_ExpenseDate",
                "IX_FixedAsset_Status_DepreciationStartDate")) assertTrue(migration.contains(token), token);
        assertTrue(migration.indexOf("SchemaMigrationHistory") < migration.indexOf("BEGIN TRANSACTION"));
        assertTrue(!migration.contains("CREATE TABLE dbo.SchemaMigrationHistory"));
        for (String token : java.util.List.of("sys.foreign_keys", "sys.check_constraints", "sys.indexes",
                "dbo.OperatingExpense", "dbo.FixedAsset", "invalid operating expense", "invalid fixed asset"))
            assertTrue(validator.contains(token), token);
    }

    private static void assertId(Class<?> type, String fieldName, String columnName) throws ReflectiveOperationException {
        var field = type.getDeclaredField(fieldName);
        assertTrue(field.isAnnotationPresent(Id.class));
        assertEquals(GenerationType.IDENTITY, field.getAnnotation(GeneratedValue.class).strategy());
        assertEquals(columnName, field.getAnnotation(Column.class).name());
    }

    private static void assertJoin(Class<?> type, String fieldName, String columnName) throws ReflectiveOperationException {
        var field = type.getDeclaredField(fieldName);
        assertEquals(User.class, field.getType());
        assertEquals(false, field.getAnnotation(ManyToOne.class).optional());
        assertEquals(columnName, field.getAnnotation(JoinColumn.class).name());
        assertEquals(false, field.getAnnotation(JoinColumn.class).nullable());
    }

    private static void assertEnum(Class<?> owner, String fieldName, Class<?> type, String name, int length)
            throws ReflectiveOperationException {
        var field = owner.getDeclaredField(fieldName);
        assertEquals(type, field.getType());
        assertEquals(EnumType.STRING, field.getAnnotation(Enumerated.class).value());
        assertColumn(owner, fieldName, type, name, false, length, 0, 0);
    }

    private static void assertColumn(Class<?> owner, String fieldName, Class<?> type, String name, boolean nullable,
            int length, int precision, int scale) throws ReflectiveOperationException {
        var column = owner.getDeclaredField(fieldName).getAnnotation(Column.class);
        assertEquals(type, owner.getDeclaredField(fieldName).getType());
        assertEquals(name, column.name());
        assertEquals(nullable, column.nullable());
        assertEquals(length, column.length());
        assertEquals(precision, column.precision());
        assertEquals(scale, column.scale());
    }
}

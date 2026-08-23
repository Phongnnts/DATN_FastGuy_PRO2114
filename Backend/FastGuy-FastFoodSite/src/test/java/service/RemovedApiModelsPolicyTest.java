package service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

class RemovedApiModelsPolicyTest {
    private static final List<String> REMOVED = List.of("ProductCombo", "ProductComboItem", "SupportTicket", "Notification", "NotificationReadReceipt");

    @Test
    void removedModelsHaveNoBackendArtifactsOrPersistenceMappings() throws Exception {
        Path main = Path.of("src/main");
        for (String name : REMOVED) {
            assertFalse(Files.exists(main.resolve("java/entity/" + name + ".java")), name);
            assertFalse(Files.readString(main.resolve("resources/META-INF/persistence.xml")).contains("entity." + name), name);
        }
        assertTrue(Files.exists(main.resolve("java/entity/ProductModifierGroup.java")));
        assertTrue(Files.exists(main.resolve("java/entity/ProductModifierOption.java")));
    }
}

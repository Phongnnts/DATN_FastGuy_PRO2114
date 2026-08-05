package service;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class StoreConfigPolicyTest {
    @Test
    void textKeysAllowEmptyValuesWhileOthersStillReject() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (!TEXT_KEYS.contains(key)) throw new IllegalArgumentException(\"Invalid config value for \" + key)"));
        assertTrue(src.contains("value = \"\";"));
    }

    @Test
    void taxRateConstrainedToZeroThroughOneHundred() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (\"tax_rate\".equals(key) && (fee.compareTo(BigDecimal.ZERO) < 0 || fee.compareTo(HUNDRED) > 0))"));
        assertTrue(src.contains("tax_rate must be between 0 and 100"));
    }

    @Test
    void estimatedDeliveryMinutesConstrainedToTenThroughOneEighty() throws IOException {
        String src = Files.readString(Path.of("src/main/java/service/StoreConfigService.java"));
        assertTrue(src.contains("if (\"estimated_delivery_minutes\".equals(key) && (minutes < 10 || minutes > 180))"));
        assertTrue(src.contains("estimated_delivery_minutes must be between 10 and 180"));
    }
}

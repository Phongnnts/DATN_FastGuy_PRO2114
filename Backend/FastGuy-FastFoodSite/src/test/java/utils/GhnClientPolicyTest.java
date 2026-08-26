package utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class GhnClientPolicyTest {
    @Test
    void masterDataFailsClosedInsteadOfReturningEmptySuccess() {
        assertThrows(IllegalStateException.class, () -> GhnClient.extractDataList(401, Map.of("message", "Token is required")));
        assertThrows(IllegalStateException.class, () -> GhnClient.extractDataList(200, Map.of("data", Map.of())));
        assertEquals(List.of(Map.of("ProvinceID", 201)), GhnClient.extractDataList(200, Map.of("data", List.of(Map.of("ProvinceID", 201)))));
    }
}

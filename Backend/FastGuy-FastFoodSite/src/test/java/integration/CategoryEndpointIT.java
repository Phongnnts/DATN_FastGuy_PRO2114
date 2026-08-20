package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class CategoryEndpointIT {
    @Test
    void liveCategoriesResponseMatchesContract() throws Exception {
        String baseUrl = System.getProperty("fastguy.integration.baseUrl");
        assertFalse(baseUrl == null || baseUrl.isBlank(),
                "Set -Dfastguy.integration.baseUrl to an explicit test environment");

        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/categories")).GET().build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request,
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode());

        JsonNode body = new ObjectMapper().readTree(response.body());
        assertEquals(Set.of("status", "data"), body.properties().stream().map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
        assertEquals("success", body.path("status").asText());
        assertTrue(body.path("data").isArray());
        assertFalse(body.path("data").isEmpty());
        for (JsonNode category : body.path("data")) {
            assertEquals(Set.of("categoryId", "name", "description", "imageUrl", "sortOrder", "productCount"),
                    category.properties().stream().map(java.util.Map.Entry::getKey).collect(java.util.stream.Collectors.toSet()));
            assertTrue(category.path("categoryId").isIntegralNumber());
            assertTrue(category.path("categoryId").asLong() > 0);
            assertTrue(category.path("name").isTextual());
            assertFalse(category.path("name").asText().isBlank());
            assertTrue(category.path("description").isTextual());
            assertTrue(category.path("sortOrder").isIntegralNumber());
            assertTrue(category.path("productCount").isIntegralNumber());
            assertTrue(category.path("productCount").asLong() >= 0);
        }
    }
}

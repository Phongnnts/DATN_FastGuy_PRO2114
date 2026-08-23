package integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import service.HomepageService;
import utils.DatabaseUtil;

class HomepageServiceIT {
    @AfterAll
    static void closeDatabase() {
        DatabaseUtil.close();
    }

    @Test
    void disposableDatabaseProducesContractReadyHomepageReadModel() {
        Map<String, Object> data = new HomepageService().getHomepage();

        assertEquals(Set.of("bestSellers", "featuredReviews"), data.keySet());
        List<?> bestSellers = (List<?>) data.get("bestSellers");
        List<?> featuredReviews = (List<?>) data.get("featuredReviews");
        assertFalse(bestSellers.isEmpty());
        assertTrue(bestSellers.size() <= 6);
        assertFalse(featuredReviews.isEmpty());
        assertTrue(featuredReviews.size() <= 3);

        @SuppressWarnings("unchecked")
        Map<String, Object> product = (Map<String, Object>) bestSellers.get(0);
        assertEquals(Boolean.TRUE, product.get("bestSeller"));
        assertTrue(product.get("defaultVariant") instanceof Map);
        assertTrue(product.get("variants") instanceof List);
        assertTrue(product.get("modifierGroups") instanceof List);

        @SuppressWarnings("unchecked")
        Map<String, Object> review = (Map<String, Object>) featuredReviews.get(0);
        assertEquals(Set.of("reviewId", "rating", "comment", "userName", "avatarUrl", "createdAt"), review.keySet());
    }
}

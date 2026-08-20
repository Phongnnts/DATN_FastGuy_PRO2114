package service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import dao.ReviewDAO;
import entity.Review;

class ReviewFeaturedMutationTest {
    @Test
    void serviceDelegatesAtomicOrderMutationAndMapsFeaturedState() {
        Review review = new Review(); review.setReviewId(9); review.setFeatured(true);
        FakeReviewDAO dao = new FakeReviewDAO(review);
        ReviewService service = new ReviewService(dao);

        Map<String, Object> mapped = service.setFeaturedByOrderId(44, true);

        assertEquals(44, dao.orderId);
        assertTrue(dao.featured);
        assertEquals(true, mapped.get("featured"));
    }

    @Test
    void eligibilityRequiresPublicSafeReviewWhenEnabling() {
        Review blank = new Review(); blank.setComment(" ");
        assertThrows(IllegalStateException.class, () -> ReviewService.requireFeaturedEligibility(blank));

        Review noConsent = new Review(); noConsent.setComment("Ngon"); noConsent.setCreatedAt(java.time.LocalDateTime.now());
        entity.User user = new entity.User(); user.setStatus("ACTIVE"); user.setFullName(" An "); noConsent.setUser(user);
        assertThrows(IllegalStateException.class, () -> ReviewService.requireFeaturedEligibility(noConsent));

        noConsent.setHomepageConsent(true);
        ReviewService.requireFeaturedEligibility(noConsent);
    }

    @Test
    void adminMappingExposesExactEligibilityAndReason() {
        Review review = new Review(); review.setReviewId(9); review.setComment("Ngon"); review.setCreatedAt(java.time.LocalDateTime.now());
        entity.User user = new entity.User(); user.setStatus("ACTIVE"); user.setFullName("An"); review.setUser(user);
        ReviewService service = new ReviewService(new FakeReviewDAO(review));

        Map<String, Object> noConsent = service.getAdminByOrderId(44);
        assertEquals(false, noConsent.get("featureEligible"));
        assertEquals("MISSING_HOMEPAGE_CONSENT", noConsent.get("featureIneligibilityReason"));

        review.setHomepageConsent(true);
        Map<String, Object> eligible = service.getAdminByOrderId(44);
        assertEquals(true, eligible.get("featureEligible"));
        assertEquals(null, eligible.get("featureIneligibilityReason"));
    }

    @Test
    void daoRequiresConsentForFeaturedReadsAndEnabling() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dao/ReviewDAO.java"));
        assertTrue(source.contains("r.featured = true AND r.homepageConsent = true"));
        assertTrue(source.contains("featured && (!Boolean.TRUE.equals(review.getHomepageConsent())"));
    }

    @Test
    void disablingDoesNotRequirePublicEligibility() {
        Review blank = new Review(); blank.setComment(" "); blank.setFeatured(true);
        FakeReviewDAO dao = new FakeReviewDAO(blank);
        new ReviewService(dao).setFeaturedByOrderId(44, false);
        assertEquals(false, dao.featured);
    }

    private static class FakeReviewDAO extends ReviewDAO {
        private final Review result; private int orderId; private boolean featured;
        FakeReviewDAO(Review result) { this.result = result; }
        @Override public Review findByOrderId(int orderId) { return result; }
        @Override public Review setFeaturedByOrderId(int orderId, boolean featured) { this.orderId = orderId; this.featured = featured; return result; }
    }
}

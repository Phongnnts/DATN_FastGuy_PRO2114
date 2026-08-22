package dao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import entity.Orders;
import entity.Product;
import entity.Review;
import entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityTransaction;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.TypedQuery;

class ReviewDAOProductScopeTest {
    @Test
    void aggregateAndPublicQueriesAreProductScopedBoundedAndBatchSummaryAvoidsNPlusOne() throws Exception {
        String source = Files.readString(Path.of("src/main/java/dao/ReviewDAO.java"));

        assertTrue(source.contains("summariesByProductIds(List<Integer> productIds)"));
        assertTrue(source.contains("r.product.productId IN :productIds"));
        assertTrue(source.contains("GROUP BY r.product.productId"));
        assertTrue(source.contains("findPublicByProductId(int productId, int page, int size)"));
        assertTrue(source.contains("setFirstResult((page - 1) * size)"));
        assertTrue(source.contains("setMaxResults(size)"));
        assertTrue(source.contains("ORDER BY r.createdAt DESC, r.reviewId DESC"));
        assertTrue(source.contains("aggregateByProductId(int productId)"));
        assertEquals(Map.of(), new ReviewDAO().summariesByProductIds(List.of()));
    }

    @Test
    void summaryProjectionAcceptsHibernateDoubleAverageWithoutConstructorMismatch() {
        List<String> events = new ArrayList<>();
        ReviewDAO.ProductReviewSummary projection = new ReviewDAO.ProductReviewSummary(31, 4.2, 16L);
        TypedQuery<ReviewDAO.ProductReviewSummary> query = (TypedQuery<ReviewDAO.ProductReviewSummary>) Proxy.newProxyInstance(
                TypedQuery.class.getClassLoader(), new Class<?>[] {TypedQuery.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setParameter" -> proxy;
                    case "getResultList" -> List.of(projection);
                    default -> null;
                });
        EntityManager em = (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] {EntityManager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "createQuery" -> query;
                    case "close" -> { events.add("close"); yield null; }
                    default -> null;
                });

        Map<Integer, ReviewDAO.ProductReviewSummary> summaries = new ReviewDAO(() -> em).summariesByProductIds(List.of(31));

        assertEquals(4.2, summaries.get(31).averageRating());
        assertEquals(16L, summaries.get(31).reviewCount());
        assertEquals(List.of("close"), events);
    }

    @Test
    void eligibilityRequiresActiveUserOwnerDeliveredOrderAndPurchasedProduct() {
        Orders order = order(7, "ACTIVE", "USER", "DELIVERED");

        ReviewDAO.requireEligible(order, 7, 1);

        order.getUser().setStatus("INACTIVE");
        assertThrows(IllegalArgumentException.class, () -> ReviewDAO.requireEligible(order, 7, 1));
        order.getUser().setStatus("ACTIVE");
        order.getUser().setRole("ADMIN");
        assertThrows(IllegalArgumentException.class, () -> ReviewDAO.requireEligible(order, 7, 1));
        order.getUser().setRole("USER");
        assertThrows(IllegalArgumentException.class, () -> ReviewDAO.requireEligible(order, 8, 1));
        order.setOrderStatus("SHIPPING");
        assertThrows(IllegalArgumentException.class, () -> ReviewDAO.requireEligible(order, 7, 1));
        order.setOrderStatus("DELIVERED");
        assertThrows(IllegalArgumentException.class, () -> ReviewDAO.requireEligible(order, 7, 0));
    }

    @Test
    void saveBeginsFlushesCommitsAndCloses() {
        List<String> events = new ArrayList<>();
        EntityManager em = entityManager(events, order(7, "ACTIVE", "USER", "DELIVERED"), 1L, 0L, null);

        Review review = new ReviewDAO(() -> em).save(7, 11, 31, 5, "Ngon", false);

        assertEquals(31, review.getProduct().getProductId());
        assertEquals(List.of("begin", "find:PESSIMISTIC_WRITE", "membership", "duplicate", "persist", "flush", "commit", "close"), events);
    }

    @Test
    void eligibilityFailureRollsBackAndCloses() {
        List<String> events = new ArrayList<>();
        EntityManager em = entityManager(events, order(7, "ACTIVE", "USER", "SHIPPING"), 1L, 0L, null);

        assertThrows(IllegalArgumentException.class, () -> new ReviewDAO(() -> em).save(7, 11, 31, 5, null, false));

        assertEquals(List.of("begin", "find:PESSIMISTIC_WRITE", "membership", "rollback", "close"), events);
    }

    @Test
    void uniqueTripleFlushRaceMapsToStableConflictAndRollsBack() {
        List<String> events = new ArrayList<>();
        PersistenceException duplicate = new PersistenceException("Violation of UNIQUE KEY constraint 'UQ_Review_UserOrderProduct'");
        EntityManager em = entityManager(events, order(7, "ACTIVE", "USER", "DELIVERED"), 2L, 0L, duplicate);

        ReviewDAO.AlreadyReviewedException error = assertThrows(ReviewDAO.AlreadyReviewedException.class,
                () -> new ReviewDAO(() -> em).save(7, 11, 31, 5, null, false));

        assertEquals("ALREADY_REVIEWED", error.getMessage());
        assertEquals(List.of("begin", "find:PESSIMISTIC_WRITE", "membership", "duplicate", "persist", "flush", "rollback", "close"), events);
    }

    @Test
    void unrelatedPersistenceFailurePropagatesAndRollsBack() {
        List<String> events = new ArrayList<>();
        PersistenceException failure = new PersistenceException("connection closed");
        EntityManager em = entityManager(events, order(7, "ACTIVE", "USER", "DELIVERED"), 1L, 0L, failure);

        PersistenceException actual = assertThrows(PersistenceException.class,
                () -> new ReviewDAO(() -> em).save(7, 11, 31, 5, null, false));

        assertSame(failure, actual);
        assertEquals(List.of("begin", "find:PESSIMISTIC_WRITE", "membership", "duplicate", "persist", "flush", "rollback", "close"), events);
    }

    private static EntityManager entityManager(List<String> events, Orders order, long membership, long existing,
            PersistenceException flushFailure) {
        EntityTransaction transaction = transaction(events);
        return (EntityManager) Proxy.newProxyInstance(EntityManager.class.getClassLoader(), new Class<?>[] {EntityManager.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "getTransaction" -> transaction;
                    case "find" -> {
                        assertEquals(Orders.class, args[0]);
                        assertEquals(LockModeType.PESSIMISTIC_WRITE, args[2]);
                        events.add("find:" + args[2]);
                        yield order;
                    }
                    case "createQuery" -> query(events, (String) args[0], membership, existing);
                    case "getReference" -> args[0] == User.class ? order.getUser() : product((int) args[1]);
                    case "persist" -> { events.add("persist"); yield null; }
                    case "flush" -> {
                        events.add("flush");
                        if (flushFailure != null) throw flushFailure;
                        yield null;
                    }
                    case "close" -> { events.add("close"); yield null; }
                    default -> null;
                });
    }

    private static TypedQuery<Long> query(List<String> events, String jpql, long membership, long existing) {
        boolean membershipQuery = jpql.contains("OrderItem");
        events.add(membershipQuery ? "membership" : "duplicate");
        return (TypedQuery<Long>) Proxy.newProxyInstance(TypedQuery.class.getClassLoader(), new Class<?>[] {TypedQuery.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "setParameter" -> proxy;
                    case "getSingleResult" -> membershipQuery ? membership : existing;
                    default -> null;
                });
    }

    private static EntityTransaction transaction(List<String> events) {
        boolean[] active = {false};
        return (EntityTransaction) Proxy.newProxyInstance(EntityTransaction.class.getClassLoader(), new Class<?>[] {EntityTransaction.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "begin" -> { active[0] = true; events.add("begin"); yield null; }
                    case "isActive" -> active[0];
                    case "commit" -> { active[0] = false; events.add("commit"); yield null; }
                    case "rollback" -> { active[0] = false; events.add("rollback"); yield null; }
                    default -> null;
                });
    }

    private static Orders order(int userId, String status, String role, String orderStatus) {
        User user = new User();
        user.setUserId(userId);
        user.setStatus(status);
        user.setRole(role);
        Orders order = new Orders();
        order.setUser(user);
        order.setOrderStatus(orderStatus);
        return order;
    }

    private static Product product(int productId) {
        Product product = new Product();
        product.setProductId(productId);
        return product;
    }
}

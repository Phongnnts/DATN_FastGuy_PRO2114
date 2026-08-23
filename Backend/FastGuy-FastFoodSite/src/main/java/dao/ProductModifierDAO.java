package dao;

import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProductModifierDAO {
    public List<ProductModifierGroup> groups(int productId) { return query("SELECT g FROM ProductModifierGroup g WHERE g.product.productId = :id ORDER BY g.sortOrder, g.modifierGroupId", ProductModifierGroup.class, "id", productId); }
    public List<ProductModifierOption> options(int groupId) { return query("SELECT o FROM ProductModifierOption o WHERE o.group.modifierGroupId = :id ORDER BY o.sortOrder, o.modifierOptionId", ProductModifierOption.class, "id", groupId); }
    public ProductModifierGroup group(int id) { return find(ProductModifierGroup.class, id); }
    public ProductModifierOption option(int id) { return find(ProductModifierOption.class, id); }
    public Map<Integer, List<ProductModifierGroup>> groupsByProductIds(List<Integer> productIds) {
        Map<Integer, List<ProductModifierGroup>> result = new HashMap<>();
        if (productIds.isEmpty()) return result;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.createQuery("SELECT g FROM ProductModifierGroup g WHERE g.product.productId IN :ids AND g.isActive = true ORDER BY g.product.productId, g.sortOrder, g.modifierGroupId", ProductModifierGroup.class)
                    .setParameter("ids", productIds).getResultList().forEach(g -> result.computeIfAbsent(g.getProduct().getProductId(), ignored -> new ArrayList<>()).add(g));
            return result;
        } finally { em.close(); }
    }
    public Map<Integer, List<ProductModifierOption>> optionsByGroupIds(List<Integer> groupIds) {
        Map<Integer, List<ProductModifierOption>> result = new HashMap<>();
        if (groupIds.isEmpty()) return result;
        EntityManager em = DatabaseUtil.getEntityManager();
        try {
            em.createQuery("SELECT o FROM ProductModifierOption o WHERE o.group.modifierGroupId IN :ids AND o.isActive = true ORDER BY o.group.modifierGroupId, o.sortOrder, o.modifierOptionId", ProductModifierOption.class)
                    .setParameter("ids", groupIds).getResultList().forEach(o -> result.computeIfAbsent(o.getGroup().getModifierGroupId(), ignored -> new ArrayList<>()).add(o));
            return result;
        } finally { em.close(); }
    }
    public void save(Object entity) { transact(em -> { if (id(entity) == 0) em.persist(entity); else em.merge(entity); }); }
    public void deleteOption(int id) {
        transact(em -> {
            ProductModifierOption option = em.find(ProductModifierOption.class, id);
            if (option == null) return;
            if (isOptionReferenced(em, id)) option.setIsActive(false); else em.remove(option);
        });
    }
    public void deleteGroup(int id) {
        transact(em -> {
            ProductModifierGroup group = em.find(ProductModifierGroup.class, id);
            if (group == null) return;
            List<ProductModifierOption> options = em.createQuery("SELECT o FROM ProductModifierOption o WHERE o.group.modifierGroupId = :id", ProductModifierOption.class).setParameter("id", id).getResultList();
            boolean referenced = options.stream().anyMatch(option -> isOptionReferenced(em, option.getModifierOptionId()));
            if (referenced) { group.setIsActive(false); options.forEach(option -> option.setIsActive(false)); return; }
            options.forEach(em::remove); em.remove(group);
        });
    }
    private boolean isOptionReferenced(EntityManager em, int optionId) {
        Long count = (Long) em.createNativeQuery("SELECT COUNT(*) FROM OrderItem WHERE modifiers_json LIKE ?").setParameter(1, "%\"modifierOptionId\":" + optionId + "%").getSingleResult();
        return count > 0;
    }
    private <T> T find(Class<T> type, int id) { EntityManager em = DatabaseUtil.getEntityManager(); try { return em.find(type, id); } finally { em.close(); } }
    private <T> List<T> query(String jpql, Class<T> type, String key, int value) { EntityManager em = DatabaseUtil.getEntityManager(); try { return em.createQuery(jpql, type).setParameter(key, value).getResultList(); } finally { em.close(); } }
    private int id(Object entity) { return entity instanceof ProductModifierGroup group ? group.getModifierGroupId() : ((ProductModifierOption) entity).getModifierOptionId(); }
    private void transact(java.util.function.Consumer<EntityManager> action) { EntityManager em = DatabaseUtil.getEntityManager(); try { em.getTransaction().begin(); action.accept(em); em.getTransaction().commit(); } catch (RuntimeException e) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); throw e; } finally { em.close(); } }
}

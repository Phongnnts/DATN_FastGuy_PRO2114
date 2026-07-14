package dao;

import entity.ProductCombo;
import entity.ProductComboItem;
import entity.ProductModifierGroup;
import entity.ProductModifierOption;
import jakarta.persistence.EntityManager;
import utils.DatabaseUtil;
import java.util.List;

public class ProductModifierDAO {
    public List<ProductModifierGroup> groups(int productId) { return query("SELECT g FROM ProductModifierGroup g WHERE g.product.productId = :id ORDER BY g.sortOrder, g.modifierGroupId", ProductModifierGroup.class, "id", productId); }
    public List<ProductModifierOption> options(int groupId) { return query("SELECT o FROM ProductModifierOption o WHERE o.group.modifierGroupId = :id ORDER BY o.sortOrder, o.modifierOptionId", ProductModifierOption.class, "id", groupId); }
    public ProductModifierGroup group(int id) { return find(ProductModifierGroup.class, id); }
    public ProductModifierOption option(int id) { return find(ProductModifierOption.class, id); }
    public ProductCombo combo(int productId) { List<ProductCombo> rows = query("SELECT c FROM ProductCombo c WHERE c.product.productId = :id", ProductCombo.class, "id", productId); return rows.isEmpty() ? null : rows.get(0); }
    public List<ProductComboItem> comboItems(int comboId) { return query("SELECT i FROM ProductComboItem i WHERE i.combo.comboId = :id ORDER BY i.sortOrder, i.comboItemId", ProductComboItem.class, "id", comboId); }
    public ProductComboItem comboItem(int id) { return find(ProductComboItem.class, id); }
    public void save(Object entity) { transact(em -> { if (id(entity) == 0) em.persist(entity); else em.merge(entity); }); }
    public void delete(Class<?> type, int id) { transact(em -> { Object value = em.find(type, id); if (value != null) em.remove(value); }); }
    private <T> T find(Class<T> type, int id) { EntityManager em = DatabaseUtil.getEntityManager(); try { return em.find(type, id); } finally { em.close(); } }
    private <T> List<T> query(String jpql, Class<T> type, String key, int value) { EntityManager em = DatabaseUtil.getEntityManager(); try { return em.createQuery(jpql, type).setParameter(key, value).getResultList(); } finally { em.close(); } }
    private int id(Object entity) { if (entity instanceof ProductModifierGroup x) return x.getModifierGroupId(); if (entity instanceof ProductModifierOption x) return x.getModifierOptionId(); if (entity instanceof ProductCombo x) return x.getComboId(); return ((ProductComboItem) entity).getComboItemId(); }
    private void transact(java.util.function.Consumer<EntityManager> action) { EntityManager em = DatabaseUtil.getEntityManager(); try { em.getTransaction().begin(); action.accept(em); em.getTransaction().commit(); } catch (RuntimeException e) { if (em.getTransaction().isActive()) em.getTransaction().rollback(); throw e; } finally { em.close(); } }
}

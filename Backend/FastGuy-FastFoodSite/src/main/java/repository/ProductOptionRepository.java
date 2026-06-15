package repository;

import config.HibernateConfig;
import entity.ProductOption;
import jakarta.persistence.EntityManager;

import java.util.List;

public class ProductOptionRepository {

    public List<ProductOption> findByProductId(Long productId) {
        try (EntityManager em = HibernateConfig.getEntityManager()) {
            return em.createQuery(
                    "SELECT po FROM ProductOption po WHERE po.product.productId = :pid",
                    ProductOption.class)
                    .setParameter("pid", productId)
                    .getResultList();
        }
    }
}

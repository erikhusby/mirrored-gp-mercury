package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.AthenaGenericDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */

@Stateful
@RequestScoped
public class ProductOrderDao extends AthenaGenericDao {

    /**
     *
     * @param orderTitle
     * @return
     */
    public ProductOrder findOrderByTitle(String orderTitle) {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<ProductOrder> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ProductOrder.class);
        Root<ProductOrder> root = criteriaQuery.from(ProductOrder.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ProductOrder_.title), orderTitle));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    /**
     *
     * @return
     */
    public List<ProductOrder> findAllOrders() {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<ProductOrder> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ProductOrder.class);
        criteriaQuery.from(ProductOrder.class);
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    /**
     *
     * @param orderId
     * @return
     */
    public ProductOrder findById(Long orderId) {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<ProductOrder> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(ProductOrder.class);
        Root<ProductOrder> root = criteriaQuery.from(ProductOrder.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(ProductOrder_.id), orderId));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
}

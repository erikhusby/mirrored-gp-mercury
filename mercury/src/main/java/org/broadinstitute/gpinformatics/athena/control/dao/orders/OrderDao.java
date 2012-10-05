package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.control.dao.AthenaGenericDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.Order;
import org.broadinstitute.gpinformatics.athena.entity.orders.Order_;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.List;

/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.Order}s.
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */

@Stateful
@RequestScoped
public class OrderDao extends AthenaGenericDao {

    /**
     *
     * @param orderTitle
     * @return
     */
    public Order findOrderByTitle(String orderTitle) {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<Order> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(Order.class);
        Root<Order> root = criteriaQuery.from(Order.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(Order_.title), orderTitle));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }

    /**
     *
     * @return
     */
    public List<Order> findAllOrders() {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<Order> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(Order.class);
        criteriaQuery.from(Order.class);
        return entityManager.createQuery(criteriaQuery).getResultList();
    }

    /**
     *
     * @param orderId
     * @return
     */
    public Order findById(Long orderId) {
        EntityManager entityManager = getAthenaThreadEntityManager().getEntityManager();
        CriteriaQuery<Order> criteriaQuery =
                entityManager.getCriteriaBuilder().createQuery(Order.class);
        Root<Order> root = criteriaQuery.from(Order.class);
        criteriaQuery.where(entityManager.getCriteriaBuilder().equal(root.get(Order_.id), orderId));
        return entityManager.createQuery(criteriaQuery).getSingleResult();
    }
}

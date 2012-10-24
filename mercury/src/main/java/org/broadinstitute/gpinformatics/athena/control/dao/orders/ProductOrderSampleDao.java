package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import java.util.List;


/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}s
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 10/5/12
 * Time: 6:17 PM
 */
@Stateful
@RequestScoped
public class ProductOrderSampleDao extends GenericDao {

    /**
     * Find ProductOrderSamples by ProductOrder
     * @return
     */
    public List<ProductOrderSample> findByProductOrder(ProductOrder productOrder) {

//        EntityManager entityManager = getEntityManager();
//        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
//        CriteriaQuery<ProductOrderSample> criteriaQuery = criteriaBuilder.createQuery(ProductOrderSample.class);
//        Root<ProductOrderSample> root = criteriaQuery.from(ProductOrderSample.class);
//        criteriaQuery.where(criteriaBuilder.equal(root.get(ProductOrderSample_.productOrder), productOrder));
//
//        try {
//            return entityManager.createQuery(criteriaQuery).getResultList();
//        } catch (NoResultException ignored) {
//            return Collections.emptyList();
//        }

        return findList(ProductOrderSample.class, ProductOrderSample_.productOrder, productOrder);

    }


}

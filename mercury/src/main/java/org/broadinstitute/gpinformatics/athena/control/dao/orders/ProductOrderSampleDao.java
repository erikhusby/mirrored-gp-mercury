package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.mercury.control.dao.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
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

        EntityManager em = getEntityManager();
        CriteriaQuery<ProductOrderSample> criteriaQuery =
                getEntityManager().getCriteriaBuilder().createQuery(ProductOrderSample.class);
        Root<ProductOrderSample> root = criteriaQuery.from(ProductOrderSample.class);
        criteriaQuery.where(em.getCriteriaBuilder().equal(root.get(ProductOrderSample_.productOrder), productOrder ));
        final List<ProductOrderSample> productOrderSamples = em.createQuery(criteriaQuery).getResultList();

        return productOrderSamples;
    }


}

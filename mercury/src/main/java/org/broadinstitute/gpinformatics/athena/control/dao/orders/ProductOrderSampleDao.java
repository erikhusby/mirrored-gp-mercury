package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import java.util.Collections;
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
    //TODO hmc not tested yet    
    public List<ProductOrderSample> findByProductOrder(ProductOrder productOrder) {

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProductOrderSample> criteriaQuery = criteriaBuilder.createQuery(ProductOrderSample.class);
        Root<ProductOrderSample> root = criteriaQuery.from(ProductOrderSample.class);
        criteriaQuery.where(criteriaBuilder.equal(root.get(ProductOrderSample_.productOrder), productOrder));

        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

}

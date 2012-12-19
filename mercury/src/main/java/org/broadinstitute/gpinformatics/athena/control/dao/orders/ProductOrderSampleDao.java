package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.List;


/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}
 * @author mccrory
 */
@Stateful
@RequestScoped
public class ProductOrderSampleDao extends GenericDao {

    /**
     * Find ProductOrderSamples by ProductOrder
     * @return
     */
    public List<ProductOrderSample> findByProductOrder(ProductOrder productOrder) {
        return findList(ProductOrderSample.class, ProductOrderSample_.productOrder, productOrder);
    }

    public List<ProductOrderSample> findByOrderAndName(@Nonnull ProductOrder productOrder, @Nonnull String sampleName) {
        if (productOrder == null) {
            throw new NullPointerException("Null Product Order.");
        }
        if (sampleName == null) {
            throw new NullPointerException("Null Sample Name.");
        }

        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrderSample> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrderSample.class);

        Predicate[] predicates = new Predicate[2];

        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);
        predicates[0] = criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.productOrder), productOrder);
        predicates[1] = criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.sampleName), sampleName);

        criteriaQuery.where(predicates);

        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}

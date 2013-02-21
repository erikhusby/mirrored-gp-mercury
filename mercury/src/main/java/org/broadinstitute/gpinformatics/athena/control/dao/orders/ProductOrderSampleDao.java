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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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

    public List<ProductOrderSample> findByOrderAndIndex(@Nonnull ProductOrder productOrder, @Nonnull List<Integer> indices) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrderSample> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrderSample.class);

        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);
        Predicate[] predicates = new Predicate[] {
                criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.productOrder), productOrder),
                productOrderSampleRoot.get(ProductOrderSample_.samplePosition).in(indices)
        };

        criteriaQuery.where(predicates);

        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
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

        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);
        Predicate[] predicates = new Predicate[] {
                criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.productOrder), productOrder),
                criteriaBuilder.equal(productOrderSampleRoot.get(ProductOrderSample_.sampleName), sampleName)
        };

        criteriaQuery.where(predicates);

        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    /**
     * For a list of sample names, return corresponding ProductOrderSamples
     * @param sampleNames list of sample names
     * @return map from sample name to List of ProductOrderSample entity.  The list is empty if none were found for
     * the key.
     */
    public Map<String, List<ProductOrderSample>> findMapBySamples(List<String> sampleNames) {
        Map<String, List<ProductOrderSample>> mapSampleNameToProductOrderSampleList =
                new HashMap<String, List<ProductOrderSample>>();
        for (String sampleName : sampleNames) {
            mapSampleNameToProductOrderSampleList.put(sampleName, new ArrayList<ProductOrderSample>());
        }
        List<ProductOrderSample> productOrderSamples = findListByList(ProductOrderSample.class,
                ProductOrderSample_.sampleName, sampleNames);
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            mapSampleNameToProductOrderSampleList.get(productOrderSample.getSampleName()).add(productOrderSample);
        }
        return mapSampleNameToProductOrderSampleList;
    }

}

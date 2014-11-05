package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Page;

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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * Dao for {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample}s.
 */
@Stateful
@RequestScoped
public class ProductOrderSampleDao extends GenericDao {

    /**
     * For the given PDO, finds the PDO samples
     * with the given sample names.
     */
    public List<ProductOrderSample> findByOrderKeyAndSampleNames(@Nonnull String pdoKey, @Nonnull Set sampleNames) {
        // split the list to avoid oracle's 1000 in clause limit
        List<Collection<String>> listOfSampleNameLists = BaseSplitter.split(sampleNames,1000);
        List<ProductOrderSample> allSamples = new ArrayList<>(sampleNames.size());

        for (Collection<String> listOfSampleNames : listOfSampleNameLists) {
            allSamples.addAll(_findByOrderKeyAndSampleNames(pdoKey, listOfSampleNames));
        }
        return allSamples;
    }

    /**
     * Does the heavy lifting for #findByOrderKeyAndSampleNames.  Assumes that
     * samplesNames has <= 1000 items.
     */
    private List<ProductOrderSample> _findByOrderKeyAndSampleNames(@Nonnull String pdoKey, @Nonnull Collection sampleNames) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrderSample> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrderSample.class);
        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);

        criteriaQuery.where(criteriaBuilder.equal(productOrderSampleRoot.join(ProductOrderSample_.productOrder).get(ProductOrder_.jiraTicketKey),pdoKey),
                            productOrderSampleRoot.get(ProductOrderSample_.sampleName).in(sampleNames));
        try {
            return entityManager.createQuery(criteriaQuery).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    /**
     * Find by ProductOrder and sample name.
     * @param productOrder ProductOrder.
     * @param sampleName Name of sample.
     * @return The matching ProductOrderSample.
     */
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
    public Map<String,Set<ProductOrderSample>> findMapBySamples(Collection<String> sampleNames) {
        Map<String, Set<ProductOrderSample>> mapSampleNameToProductOrderSampleList =
                new HashMap<>();
        for (String sampleName : sampleNames) {
            mapSampleNameToProductOrderSampleList.put(sampleName, new HashSet<ProductOrderSample>());
        }
        List<ProductOrderSample> productOrderSamples = findListByList(ProductOrderSample.class,
                ProductOrderSample_.sampleName, sampleNames);
        for (ProductOrderSample productOrderSample : productOrderSamples) {
            mapSampleNameToProductOrderSampleList.get(productOrderSample.getName()).add(productOrderSample);
        }
        return mapSampleNameToProductOrderSampleList;
    }


    /**
     * Return a count of the number of samples in this {@link ProductOrder} that have billing ledger entries of any
     * kind (billed, billing session started, or billing session not started).
     *
     * @param productOrder The PDO.
     *
     * @return Number of samples in the PDO with billing ledger entries.
     *
     */
    public long countSamplesWithLedgerEntries(@Nonnull ProductOrder productOrder) {

        CriteriaBuilder cb = getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        cq.distinct(true);

        Root<ProductOrderSample> root = cq.from(ProductOrderSample.class);
        root.join(ProductOrderSample_.ledgerItems);

        cq.select(cb.count(root));
        cq.where(cb.equal(root.get(ProductOrderSample_.productOrder), productOrder));

        return getEntityManager().createQuery(cq).getSingleResult();
    }

    public List<ProductOrderSample> findBySamples(Collection<String> sampleNames) {
        return findListByList(ProductOrderSample.class,ProductOrderSample_.sampleName,sampleNames);
    }

    /**
     * returns a block of product order samples
     * @param pageNumber the intended page of results requested by the user
     * @return
     */
    public Page<ProductOrderSample> getBlockOfSamples(int pageNumber) {

        int first = (pageNumber - 1) * GenericDao.RESULT_DEFAULT_PAGE_SIZE;
        List<ProductOrderSample> sampleList = findAll(ProductOrderSample.class,
                first,GenericDao.RESULT_DEFAULT_PAGE_SIZE);

        return new Page<>(sampleList, pageNumber, first, GenericDao.RESULT_DEFAULT_PAGE_SIZE);
    }
}

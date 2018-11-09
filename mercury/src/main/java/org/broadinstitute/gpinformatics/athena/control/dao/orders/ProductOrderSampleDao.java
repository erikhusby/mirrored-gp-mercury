package org.broadinstitute.gpinformatics.athena.control.dao.orders;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample_;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject_;
import org.broadinstitute.gpinformatics.infrastructure.common.BaseSplitter;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
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
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
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
     * Used primarily for a fixup test, this method will return a block of results from the total set of product
     * order samples which are not previously bound to a mercury sample
     * @param page              Zero based index of the Block of samples to return
     * @param sampleBlockSize    Number of samples to return per block
     * @return a block of samples out of the total set
     */
    public List<ProductOrderSample> findSamplesWithoutMercurySample(int page, int sampleBlockSize) {

        return findAll(ProductOrderSample.class,
                new GenericDaoCallback<ProductOrderSample>() {
                    @Override
                    public void callback(CriteriaQuery<ProductOrderSample> criteriaQuery,
                                         Root<ProductOrderSample> root) {

                        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
                        Join<ProductOrderSample, MercurySample> mercurySampleJoin =
                                root.join(ProductOrderSample_.mercurySample, JoinType.LEFT);


                        Subquery<String> sampleNameSubquery = criteriaQuery.subquery(String.class);
                        Root<MercurySample> mercurySampleRoot = sampleNameSubquery.from(MercurySample.class);

                        sampleNameSubquery.select(mercurySampleRoot.get(MercurySample_.sampleKey));

                        Predicate predicate = builder.and(builder.isNull(mercurySampleJoin.get(MercurySample_.sampleKey)),
                                builder.in(root.get(ProductOrderSample_.sampleName)).value(sampleNameSubquery));

                        criteriaQuery.where(predicate);

                        criteriaQuery.orderBy(builder.asc(root.get(ProductOrderSample_.sampleName)));
                    }
                },
                page * sampleBlockSize, sampleBlockSize);
    }

    /**
     * Find all ProductOrderSamples in a ResearchProject which are available for data submissions.
     *
     * @param researchProjectKey The research project to search.
     * @return List<ProductOrderSample> which are in ResearchProject with key researchProjectKey
     */
    public List<ProductOrderSample> findSubmissionSamples(String researchProjectKey) {
        EntityManager entityManager = getEntityManager();
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();

        CriteriaQuery<ProductOrderSample> criteriaQuery =
                criteriaBuilder.createQuery(ProductOrderSample.class);
        Root<ProductOrderSample> productOrderSampleRoot = criteriaQuery.from(ProductOrderSample.class);
        productOrderSampleRoot.fetch(ProductOrderSample_.mercurySample);
        Join<ProductOrderSample, ProductOrder> productOrderJoin =
                productOrderSampleRoot.join(ProductOrderSample_.productOrder);
        Join<ProductOrder, ResearchProject> researchProjectrJoin = productOrderJoin.join(ProductOrder_.researchProject);

        Predicate predicate =
                criteriaBuilder.equal(researchProjectrJoin.get(ResearchProject_.jiraTicketKey), researchProjectKey);

        Predicate orderStatusPredicate = criteriaBuilder.not(productOrderJoin.get(ProductOrder_.orderStatus)
                .in(ProductOrder.OrderStatus.Draft, ProductOrder.OrderStatus.Abandoned, ProductOrder.OrderStatus.Pending));

        criteriaQuery.where(predicate).having(orderStatusPredicate);
        criteriaQuery.orderBy(criteriaBuilder.desc(productOrderJoin.get(ProductOrder_.placedDate)));

        try {
            TypedQuery<ProductOrderSample> query = entityManager.createQuery(criteriaQuery);
            return query.getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
    }
}

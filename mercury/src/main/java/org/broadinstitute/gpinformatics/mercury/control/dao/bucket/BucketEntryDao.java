package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder_;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.products.Product_;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketCount;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.SetJoin;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 10/31/12
 *         Time: 10:58 AM
 */
@Stateful
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@RequestScoped
public class BucketEntryDao extends GenericDao {

    public List<BucketEntry> findByIds(List<Long> ids) {
        return findListByList(BucketEntry.class, BucketEntry_.bucketEntryId, ids);
    }

    public BucketEntry findByVesselAndPO(LabVessel vessel, ProductOrder productOrder) {

        CriteriaBuilder vesselPOCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = vesselPOCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where(vesselPOCriteria.and(vesselPOCriteria.equal(root.get(BucketEntry_.labVessel), vessel),
                                         vesselPOCriteria.equal(root.get(BucketEntry_.productOrder),
                                                                productOrder)));
        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    public List<BucketEntry> findByProductOrder(ProductOrder productOrder) {

        CriteriaBuilder vesselPOCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = vesselPOCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where(vesselPOCriteria.equal(root.get(BucketEntry_.productOrder),
                                                                productOrder));
        try {
            return getEntityManager().createQuery(query).getResultList();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    public List<BucketEntry> findByVesselAndBatch(LabVessel vessel, LabBatch labBatch) {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = criteriaBuilder.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where(criteriaBuilder.and(criteriaBuilder.equal(root.get(BucketEntry_.labVessel), vessel),
                criteriaBuilder.equal(root.get(BucketEntry_.labBatch), labBatch)));
        try {
            return getEntityManager().createQuery(query).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    public BucketEntry findByVesselAndBucket(LabVessel vessel, Bucket bucket) {
        CriteriaBuilder vesselBucketCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = vesselBucketCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where(vesselBucketCriteria.and(vesselBucketCriteria.equal(root.get(BucketEntry_.labVessel), vessel),
                                             vesselBucketCriteria.equal(root.get(BucketEntry_.bucket),
                                                                        bucket)));
        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    public List<BucketEntry> findBySampleAndBucket(List<String> sampleIds, Bucket bucket) {
        CriteriaBuilder bucketCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = bucketCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);

        Join<BucketEntry, LabVessel> bucketEntryLabVesselJoin = root.join(BucketEntry_.labVessel);
        SetJoin<LabVessel, MercurySample> labVesselMercurySamplesJoin = bucketEntryLabVesselJoin.join(LabVessel_.mercurySamples);

        query.where(bucketCriteria.and(
            bucketCriteria.equal(root.get(BucketEntry_.bucket), bucket),
            bucketCriteria.equal(root.get(BucketEntry_.status), BucketEntry.Status.Active),
            labVesselMercurySamplesJoin.get(MercurySample_.sampleKey).in(sampleIds)
        ));

        try {
            return getEntityManager().createQuery(query).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }


    public List<BucketEntry> findByProductAndBucket(List<String> product, Bucket bucket) {
        CriteriaBuilder bucketCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = bucketCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        Join<BucketEntry, ProductOrder> entryProductOrderJoin = root.join(BucketEntry_.productOrder);
        Join<ProductOrder, Product> productJoin = entryProductOrderJoin.join(ProductOrder_.product);

        query.where(bucketCriteria.and(
            bucketCriteria.equal(root.get(BucketEntry_.bucket), bucket),
            bucketCriteria.equal(root.get(BucketEntry_.status), BucketEntry.Status.Active),
            bucketCriteria.or(
                productJoin.get(Product_.partNumber).in(product),
                bucketCriteria.or(
                    productJoin.get(Product_.productName).in(product),
                    bucketCriteria.or(product.stream()
                        .map(p -> bucketCriteria.like(productJoin.get(Product_.productName), String.format("%%%s%%", p)))
                        .toArray(Predicate[]::new))
                )
            )
        ));

        try {
            return getEntityManager().createQuery(query).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    public List<BucketEntry> findByProductOrderAndBucket(List<String> pdo, Bucket bucket) {
        CriteriaBuilder bucketCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = bucketCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        Join<BucketEntry, ProductOrder> entryProductOrderJoin = root.join(BucketEntry_.productOrder);

        query.where(bucketCriteria.and(
            bucketCriteria.equal(root.get(BucketEntry_.bucket), bucket),
            bucketCriteria.equal(root.get(BucketEntry_.status), BucketEntry.Status.Active),
            bucketCriteria.or(
                entryProductOrderJoin.get(ProductOrder_.jiraTicketKey).in(pdo),
                entryProductOrderJoin.get(ProductOrder_.title).in(pdo)
            )
        ));

        try {
            return getEntityManager().createQuery(query).getResultList();
        } catch (NoResultException ignored) {
            return Collections.emptyList();
        }
    }

    public Map<String, BucketCount> getBucketCounts() {
        CriteriaBuilder criteriaBuilder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketCount> criteriaQuery = criteriaBuilder.createQuery(BucketCount.class);
        Root<Bucket> root = criteriaQuery.from(Bucket.class);

        Join<Bucket, BucketEntry> bucketEntryBucketJoin = root.join(Bucket_.bucketEntries, JoinType.LEFT);
        Join<Bucket, BucketEntry> reworkEntryBucketJoin = root.join(Bucket_.reworkEntries, JoinType.LEFT);

        Path<String> bucketNamePath = root.get(Bucket_.bucketDefinitionName);
        CriteriaQuery<BucketCount> multiSelect = criteriaQuery.select(
                criteriaBuilder.construct(
                        BucketCount.class,
                        bucketNamePath,
                        criteriaBuilder.countDistinct(bucketEntryBucketJoin),
                        criteriaBuilder.countDistinct(reworkEntryBucketJoin)
                )).groupBy(bucketNamePath);

        List<BucketCount> resultList = getEntityManager().createQuery(multiSelect).getResultList();
        Map<String, BucketCount> bucketCountMap = new HashMap<>(resultList.size());
        for (BucketCount bucketCount : resultList) {
            bucketCountMap.put(bucketCount.getBucket(), bucketCount);
        }
        return bucketCountMap;
    }
}

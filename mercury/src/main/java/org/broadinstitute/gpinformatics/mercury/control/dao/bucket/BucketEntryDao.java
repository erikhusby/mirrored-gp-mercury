package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketCount;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

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
import javax.persistence.criteria.Root;
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

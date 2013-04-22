package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 */
public class ActiveBucketCallback implements GenericDao.GenericDaoCallback<BucketEntry>  {

    private final BucketEntryDao bucketEntryDao;
    private final BucketEntry.Status targetStatus;


    public ActiveBucketCallback(BucketEntryDao bucketEntryDao, BucketEntry.Status targetStatus) {
        this.bucketEntryDao = bucketEntryDao;
        this.targetStatus = targetStatus;
    }

    @Override
    public void callback(CriteriaQuery<BucketEntry> bucketEntryCriteriaQuery,
                         Root<BucketEntry> bucketEntryRoot) {

        CriteriaBuilder criteriaBuilder = bucketEntryDao.getEntityManager().getCriteriaBuilder();
//        bucketEntryCriteriaQuery.where(criteriaBuilder.and(criteriaBuilder.equal(bucketEntryRoot.get(BucketEntry_.labVessel))))
    }
}

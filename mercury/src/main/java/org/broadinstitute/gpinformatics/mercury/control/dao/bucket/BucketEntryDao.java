package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;

import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.persistence.NoResultException;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * @author Scott Matthews
 *         Date: 10/31/12
 *         Time: 10:58 AM
 */
@Stateful
@TransactionAttribute ( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class BucketEntryDao extends GenericDao {

    public BucketEntry findByVesselAndPO(LabVessel vessel, String productOrder) {
        CriteriaBuilder vesselPOCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = vesselPOCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where ( vesselPOCriteria.and ( vesselPOCriteria.equal ( root.get ( BucketEntry_.labVessel ), vessel ),
                                             vesselPOCriteria.equal ( root.get ( BucketEntry_.poBusinessKey ),
                                                                      productOrder ) ) );
        try{
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

    public BucketEntry findByVesselAndBucket(LabVessel vessel, Bucket bucket) {
        CriteriaBuilder vesselBucketCriteria = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<BucketEntry> query = vesselBucketCriteria.createQuery(BucketEntry.class);
        Root<BucketEntry> root = query.from(BucketEntry.class);
        query.where(vesselBucketCriteria.and(vesselBucketCriteria.equal(root.get(BucketEntry_.labVessel),vessel),
                                             vesselBucketCriteria.equal(root.get(BucketEntry_.bucketExistence),
                                                                        bucket)));
        try {
            return getEntityManager().createQuery(query).getSingleResult();
        } catch (NoResultException ignored) {
            return null;
        }
    }

}

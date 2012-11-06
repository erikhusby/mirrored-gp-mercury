package org.broadinstitute.gpinformatics.mercury.control.dao.bucket;

import org.broadinstitute.gpinformatics.infrastructure.jpa.GenericDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket_;

import javax.annotation.Nonnull;
import javax.ejb.Stateful;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;

@Stateful
@TransactionAttribute( TransactionAttributeType.SUPPORTS)
@RequestScoped
public class BucketDao extends GenericDao {

    /**
     * Simple finder method that allows the system to retrieve an existing bucket by the name workflow process step
     * that to which the bucket is associated
     *
     * @param bucketName Unique name of a workflow process step that is associated with a bucket
     * @return An instance of a bucket tied to the given bucket name
     */
    public Bucket findByName( @Nonnull String bucketName){
        return findSingle(Bucket.class, Bucket_.bucketDefinitionName, bucketName);
    }

}

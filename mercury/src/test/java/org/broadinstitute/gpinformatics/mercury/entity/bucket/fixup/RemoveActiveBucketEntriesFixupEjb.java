package org.broadinstitute.gpinformatics.mercury.entity.bucket.fixup;

import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Moved from an inner class per JavaEE7/Wildfly requirement
 */
@Stateful
public class RemoveActiveBucketEntriesFixupEjb {

    @Inject
    private BucketDao bucketDao;

    public void removeActiveBucketEntries() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        Collection<BucketEntry> entries = new ArrayList<>();
        entries.addAll(bucket.getBucketEntries());
        entries.addAll(bucket.getReworkEntries());
        for (BucketEntry bucketEntry : entries) {
            bucket.removeEntry(bucketEntry);
            bucketDao.remove(bucketEntry);
        }
    }
}

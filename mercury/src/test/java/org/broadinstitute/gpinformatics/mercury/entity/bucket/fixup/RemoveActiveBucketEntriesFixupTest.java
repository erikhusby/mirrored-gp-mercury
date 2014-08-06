package org.broadinstitute.gpinformatics.mercury.entity.bucket.fixup;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.ejb.Stateful;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author breilly
 */
@Test(groups = TestGroups.FIXUP)
public class RemoveActiveBucketEntriesFixupTest extends Arquillian {

    @Inject
    private RemoveActiveBucketEntriesFixupEjb ejb;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @Test(enabled = false)
    public void removeActiveBucketEntries() {
        ejb.removeActiveBucketEntries();
    }

    @Stateful
    public static class RemoveActiveBucketEntriesFixupEjb {

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
}

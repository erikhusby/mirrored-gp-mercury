package org.broadinstitute.gpinformatics.mercury.entity.bucket;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.ReworkReasonDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.rapsheet.ReworkEntry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class BucketEntryFixupTest extends Arquillian {

    @Inject
    BucketDao bucketDao;

    @Inject
    BucketEntryDao bucketEntryDao;

    @Inject
    ReworkReasonDao reworkReasonDao;

    @Inject
    LabVesselDao labVesselDao;

    @Inject
    ProductOrderDao productOrderDao;

    @Inject
    UserTransaction utx;

    /**
     * Use test deployment here to talk to the actual jira
     *
     * @return
     */
    @Deployment
    public static WebArchive buildMercuryWar() {

        /*
         * If the need comes to utilize this fixup in production, change the buildMercuryWar parameters accordingly
         */
        return DeploymentBuilder.buildMercuryWar(
                org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV, "dev");
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }
        utx.begin();
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        // Skip if no injections, since we're not running in container.
        if (utx == null) {
            return;
        }

        utx.commit();
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void archiveReworkEntries() throws Exception {

        Bucket fixupBucket = bucketDao.findByName("Pico/Plating Bucket");


        for (BucketEntry reworkEntry : fixupBucket.getReworkEntries()) {
            reworkEntry.setStatus(BucketEntry.Status.Archived);
        }
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void remove0150385070FromShearingBucketForGPLIM1932() {
        Bucket bucket = bucketDao.findByName("Shearing Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0150385070");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void remove0155694973FromPoolingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pooling Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0155694973");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void remove0155694973FromPicoPlatingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0155694973");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = false)
    public void remove0159876899FromPicoPlatingBucketGPLIM2503() {
        Bucket bucket = bucketDao.findByName("Pico/Plating Bucket");
        LabVessel vessel = labVesselDao.findByIdentifier("0159876899");

        BucketEntry bucketEntry = bucket.findEntry(vessel);
        bucket.removeEntry(bucketEntry);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION, enabled = true)
    public void setProductOrderReferences() {
        List<BucketEntry> bucketEntriesToFix = bucketDao.findList(BucketEntry.class, BucketEntry_.productOrder, null);

        for(BucketEntry entry:bucketEntriesToFix) {
            ProductOrder orderToUpdate = productOrderDao.findByBusinessKey(entry.getPoBusinessKey());
            if(orderToUpdate != null) {
                entry.setProductOrder(orderToUpdate);
            }
        }
    }
}

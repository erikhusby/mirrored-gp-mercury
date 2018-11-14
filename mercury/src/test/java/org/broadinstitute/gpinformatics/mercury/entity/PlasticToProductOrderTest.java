package org.broadinstitute.gpinformatics.mercury.entity;


import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.BarcodedTubeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.UserTransaction;

import static org.testng.Assert.fail;

@Test(groups = {TestGroups.STUBBY})
@Dependent
public class PlasticToProductOrderTest extends StubbyContainerTest {

    public PlasticToProductOrderTest(){}

    @Inject
    private BucketDao bucketDao;

    @Inject
    private BucketEntryDao bucketEntryDao;

    @Inject
    private BucketEjb bucketResource;

    @Inject
    private BarcodedTubeDao tubeDao;

    @Inject
    private TubeFormationDao rackDao;

    @Inject
    private StaticPlateDao plateDao;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private LabEventFactory eventFactory;

    @Inject
    private UserTransaction utx;

    public static final String BUCKET_REFERENCE_NAME = "Start";
    private Bucket bucket;
    private String tubeBarcode;
    private WorkflowBucketDef bucketDef;

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }
        utx.begin();

        if (bucketDao == null) {
            return;
        }
        bucketDef = new WorkflowBucketDef(BUCKET_REFERENCE_NAME);

        bucket = new Bucket(bucketDef);
        bucketDao.persist(bucket);
        bucketDao.flush();
        bucketDao.clear();

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        // Skip if no injections, meaning we're not running in container
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    /**
     * Get some unindexed samples from bsp, then apply indexes for them.
     * Does the PDO mapping find the right thing?
     */
    @Test(enabled = false)
    public void test_incoming_sample_unindexed_index_added_in_workflow() {
        fail();
    }

    /**
     * Gin up some pre-indexed samples, run through an exome
     * workflow and verify that the right PDOs are found.
     */
    @Test(enabled = false)
    public void test_incoming_sample_indexed_already() {
        fail();
    }

    /**
     * Put the same sample into the bucket twice, one for PDO x
     * and one for PDO y.  After they're pooled halfway through
     * and exome workflow, does the code find the right
     * PDO for each sample?
     */
    @Test(enabled = false)
    public void test_sample_in_bucket_for_two_different_pdos() {
        fail();
    }

    /**
     * Put two different samples into a bucket, each with a different
     * PDO.  Pool them in an exome workflow.  Does the code
     * find the right PDO for each sample?
     */
    @Test(enabled = false)
    public void test_pool_across_pdos() {
        fail();
    }

    /**
     * Take an unindexed sample and run it through
     * the exome workflow, adding an index along the way.
     * At a point in the event graph after the indexing,
     * create a {@link LabEvent} that sets a different
     * {@link LabEvent#productOrderId}, and then apply some
     * events and transfers after that.  The branch
     * below this is considered a dev branch.
     * <p/>
     * Does the code map the stuff on the dev branch
     * to the dev PDO?  Does the stuff on the earlier
     * production branch map to the production PDO?
     */
    @Test(enabled = false)
    public void test_development_branch() {
        fail();
    }

    /**
     * Given 3 samples from 3 different PDOs, run
     * a workflow that pools two of them.  Then
     * do a few transfers and add the third
     * sample to the pool at a later step.
     * Does the pool find the right PDO for each
     * of the 3 samples?  For an interim vessel
     * that contains only two samples, does the
     * code find the right 2 samples and map them
     * to the right PDOs?
     */
    @Test(enabled = false)
    public void test_multistep_pooling() {
        fail();
    }


}

package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.*;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

/**
 * Container test of MercuryClientService
 *
 * @author epolk
 */

@Test(enabled = false, groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryClientServiceTest extends Arquillian {
    private Log logger = LogFactory.getLog(getClass());
    // Id used for jira ticket and batch that are only used by this test.
    private static final String TEST_LCSET_NAME = "LCSET-MCSTEST";
    // This is a GDNA sample known to BSP and not otherwise used in Mercury (i.e. only used by this test).
    private static final String NON_MERCURY_SAMPLE_NAME = "SM-1T7HE";
    // This is a bsp user known by BSP.
    private static final long REAL_BSP_USER_ID = 10647L;
    // This is a pdo known by Jira.
    private static final String REAL_PDO_KEY = "PDO-8";

    private ProductOrder pdo1;
    private ProductOrder pdo2;


    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    private MercuryClientService service;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private LabEventDao labEventDao;
    @Inject
    private BucketEntryDao bucketEntryDao;
    @Inject
    private JiraTicketDao jiraTicketDao;
    @Inject
    private LabBatchDAO labBatchDao;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() {
        pdo1 = ProductOrderTestFactory.buildExExProductOrder(1);
        pdo1.setJiraTicketKey(REAL_PDO_KEY);
        pdo1.setCreatedBy(REAL_BSP_USER_ID);

        pdo2 = ProductOrderTestFactory.buildExExProductOrder(2);
        pdo2.setJiraTicketKey(REAL_PDO_KEY);
        pdo2.setCreatedBy(REAL_BSP_USER_ID);

        List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();
        pdoSamples.add(new ProductOrderSample(NON_MERCURY_SAMPLE_NAME));
        pdo1.setSamples(pdoSamples);

        pdoSamples.clear();
        for (int i = 1; i < 3; ++i) {
            ProductOrderSample sample = new ProductOrderSample(NON_MERCURY_SAMPLE_NAME);
            sample.setSamplePosition(i);
            pdoSamples.add(sample);
        }
        pdo2.setSamples(pdoSamples);

        if (labVesselDao != null) {
            deleteTestSample();
        }
    }

    // For some unknown reason Arquillian runs this without error, but fails to actually run sql to delete things.
    // This test allows the samples etc to remain in the database and will delete it all at the start of the run.
    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() {
        if (labVesselDao != null) {
            deleteTestSample();
        }
    }

    // Deletes the mercury sample used in this test.  When dao's are defined but there is no transaction, this does nothing.
    private void deleteTestSample() {
        try {
            List<LabVessel> labVessels = labVesselDao.findBySampleKey(NON_MERCURY_SAMPLE_NAME);

            List<BucketEntry> bucketEntries = bucketEntryDao.findListByList(BucketEntry.class, BucketEntry_.labVessel, labVessels);
            for (BucketEntry bucketEntry : bucketEntries) {
                logger.info("Deleting bucket entry " + bucketEntry.getBucketEntryId());
                bucketEntryDao.remove(bucketEntry);
            }
            List<LabEvent> labEvents = labEventDao.findListByList(LabEvent.class, LabEvent_.inPlaceLabVessel, labVessels);
            for (LabEvent labEvent : labEvents) {
                logger.info("Deleting " + labEvent.getLabEventType() + " event " + labEvent.getLabEventId());
                labEventDao.remove(labEvent);
            }

            JiraTicket jiraTicket = jiraTicketDao.fetchByName(TEST_LCSET_NAME);
            if (jiraTicket != null) {
                logger.info("Deleting jiraTicket " + TEST_LCSET_NAME);
                jiraTicketDao.remove(jiraTicket);
            }

            LabBatch labBatch = labBatchDao.findByName(TEST_LCSET_NAME);
            if (labBatch != null) {
                logger.info("Deleting labBatch " + TEST_LCSET_NAME);
                labBatchDao.remove(labBatch);
            }

            for (LabVessel labVessel : labVessels) {
                logger.info("Deleting vessel " + labVessel.getLabel());
                labVesselDao.remove(labVessel);
            }

            for (MercurySample mercurySample : mercurySampleDao.findBySampleKey(NON_MERCURY_SAMPLE_NAME)) {
                logger.info("Deleting sample " + mercurySample.getSampleKey());
                mercurySampleDao.remove(mercurySample);
            }
        } catch (Exception e) {
            logger.error("Caught while deleting test records", e);
        }
    }

    public void testSampleToPicoBucket() throws Exception {

        // First time through, creates the initial vessel & sample
        Assert.assertEquals(labVesselDao.findBySampleKey(NON_MERCURY_SAMPLE_NAME).size(), 0);
        Collection<ProductOrderSample> addedSamples1 = service.addSampleToPicoBucket(pdo1);
        Assert.assertEquals(addedSamples1.size(), pdo1.getSamples().size());

        // Second time through it reuses the existing vessel & sample
        Collection<ProductOrderSample> addedSamples2 = service.addSampleToPicoBucket(pdo2);
        Assert.assertEquals(pdo2.getSamples().size(), 2);
        Assert.assertEquals(addedSamples2.size(), pdo2.getSamples().size());

        // Arquillian is unable to delete the samples after the test is run, so samples are
        // put in a batch so that they can be more easily deleted by the next test run.
        LabBatch testBatch = makeTestJiraAndBatch(new HashSet<LabVessel>(labVesselDao.findBySampleKey(NON_MERCURY_SAMPLE_NAME)));
        assert(testBatch.getStartingLabVessels().size() > 0);
    }

    private LabBatch makeTestJiraAndBatch(Set<LabVessel> labVessels) {
        // Creates a jira ticket and batch to allow subsequent deletion of the lb_starting_lab_vessels records.
        JiraTicket jiraTicket = jiraTicketDao.fetchByName(TEST_LCSET_NAME);
        if (jiraTicket == null) {
            jiraTicket = new JiraTicket(new JiraServiceStub(), TEST_LCSET_NAME);
            jiraTicketDao.persist(jiraTicket);
        }

        LabBatch labBatch = labBatchDao.findByName(TEST_LCSET_NAME);
        if (labBatch == null) {
            labBatch = new LabBatch(TEST_LCSET_NAME, labVessels, LabBatchType.WORKFLOW);
            labBatchDao.persist(labBatch);
        }

        return labBatch;
    }

}

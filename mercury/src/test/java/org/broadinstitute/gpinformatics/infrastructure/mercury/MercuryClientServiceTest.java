package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketEntryDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.testng.Assert.fail;

/**
 * Container test of MercuryClientService
 *
 * @author epolk
 */

@Test(enabled = true, groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryClientServiceTest extends Arquillian {
    private Log logger = LogFactory.getLog(getClass());
    private String nonMercurySampleName = "SM-1T7HE";
    //private String nonMercurySampleName = "SM-4B5XS";
    private static final int SAMPLE_SIZE = 1;
    private static final long REAL_BSP_USER_ID = 10647L;
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

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() {
        pdo1 = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
        pdo1.setJiraTicketKey(REAL_PDO_KEY);
        pdo1.setCreatedBy(REAL_BSP_USER_ID);

        pdo2 = ProductOrderTestFactory.buildExExProductOrder(SAMPLE_SIZE);
        pdo2.setJiraTicketKey(REAL_PDO_KEY);
        pdo2.setCreatedBy(REAL_BSP_USER_ID);

        List<ProductOrderSample> pdoSamples = new ArrayList<ProductOrderSample>();
        pdoSamples.add(new ProductOrderSample(nonMercurySampleName));
        pdo1.setSamples(pdoSamples);

        pdoSamples.clear();
        pdoSamples.add(new ProductOrderSample(nonMercurySampleName));
        pdo2.setSamples(pdoSamples);

        if (labVesselDao != null) {
            deleteTestSample();
        }
    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void afterMethod() {
        if (labVesselDao != null) {
            deleteTestSample();
        }
    }

    /** Deletes the mercury sample used in this test. */
    private void deleteTestSample() {
        try {
            List<LabVessel> labVessels = labVesselDao.findBySampleKey(nonMercurySampleName);

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
            for (LabVessel labVessel : labVessels) {
                logger.info("Deleting vessel " + labVessel.getLabel());
                labVesselDao.remove(labVessel);
            }

            for (MercurySample mercurySample : mercurySampleDao.findBySampleKey(nonMercurySampleName)) {
                logger.info("Deleting sample " + mercurySample.getSampleKey());
                mercurySampleDao.remove(mercurySample);
            }
        } catch (Exception e) {
            logger.error("Error finding and deleting the test sample, vessel, lab event, bucket entry", e);
            fail("Error finding and deleting the test sample, vessel, lab event, bucket entry");
        }
    }

    public void testSampleToPicoBucket() throws Exception {
        // First time through, creates the initial vessel & sample
        Collection<ProductOrderSample> addedSamples1 = service.addSampleToPicoBucket(pdo1);
        Assert.assertEquals(addedSamples1.size(), pdo1.getSamples().size());

        // Second time through it reuses the existing vessel & sample
        Collection<ProductOrderSample> addedSamples2 = service.addSampleToPicoBucket(pdo2);
        Assert.assertEquals(addedSamples2.size(), pdo2.getSamples().size());
    }

}

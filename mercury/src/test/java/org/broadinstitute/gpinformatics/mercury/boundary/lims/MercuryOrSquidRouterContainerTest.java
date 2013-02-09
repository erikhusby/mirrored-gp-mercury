package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 2/7/13
 *         Time: 11:10 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryOrSquidRouterContainerTest extends ContainerTest {

    @Inject
    UserTransaction utx;

    @Inject
    private ProductOrderDao poDao;

    @Inject
    private LabVesselDao vesselDao;

    @Inject
    private ProductDao productDao;

    @Inject
    private ResearchProjectDao researchProjectDao;

    @Inject
    private BettaLimsMessageFactory bettaLimsMessageFactory;

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private BucketDao bucketDao;

    private ProductOrder testExExOrder;
    private ProductOrder squidProductOrder;
    private String       squidPdoJiraKey;

    private String exExJiraKey;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        utx.begin();

        testExExOrder =
                poDao.findByWorkflowName(WorkflowConfig.WorkflowName.EXOME_EXPRESS.getWorkflowName()).iterator().next();
        exExJiraKey = testExExOrder.getBusinessKey();

        squidProductOrder =
                poDao.findByWorkflowName(WorkflowConfig.WorkflowName.WHOLE_GENOME.getWorkflowName()).iterator().next();
        squidPdoJiraKey = squidProductOrder.getBusinessKey();

    }

    @AfterMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testExomeExpressTubeEvent() throws Exception {

        testExExOrder = poDao.findByBusinessKey(exExJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildVesselsForPdo(testExExOrder, "Pico/Plating Bucket");

        String rackBarcode = "REXEX" + (new Date()).toString();
        LabEventTest.PicoPlatingJaxbBuilder jaxbBuilder =
                new LabEventTest.PicoPlatingJaxbBuilder(rackBarcode, new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                               "", bettaLimsMessageFactory);
        jaxbBuilder.invoke();

        for (BettaLIMSMessage msg : jaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(msg);
            vesselDao.flush();
            vesselDao.clear();
        }

//        LabVessel finalItem = vesselDao.findByIdentifier(rackBarcode);
        RackOfTubes finalItem = (RackOfTubes)vesselDao.findByIdentifier(jaxbBuilder.getPicoPlatingNormalizaionBarcode());
        TubeFormation finalFormation = finalItem.getTubeFormations().iterator().next();

        Assert.assertTrue(finalFormation.getEvents().size() > 0);
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressTubeEvent() throws Exception {

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube = buildVesselsForPdo(squidProductOrder, null);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        LabEventTest.PreFlightJaxbBuilder jaxbBuilder =
                new LabEventTest.PreFlightJaxbBuilder(bettaLimsMessageFactory, "",
                                                             new ArrayList<String>(mapBarcodeToTube.keySet()));
        jaxbBuilder.invoke();

        for (BettaLIMSMessage msg : jaxbBuilder.getMessageList()) {
            bettalimsMessageResource.processMessage(msg);
            vesselDao.flush();
            vesselDao.clear();
        }

        RackOfTubes finalItem = (RackOfTubes)vesselDao.findByIdentifier(jaxbBuilder.getRackBarcode());
        TubeFormation finalTubes = finalItem.getTubeFormations().iterator().next();

        Assert.assertTrue(finalTubes.getEvents().size() == 0);
    }

    private Map<String, TwoDBarcodedTube> buildVesselsForPdo(ProductOrder productOrder, String bucketName) {

        Set<LabVessel> tubes = new HashSet<LabVessel>(productOrder.getTotalSampleCount());

        List<String> barcodes = new ArrayList<String>(productOrder.getTotalSampleCount());

        List<BucketEntry> bucketEntries = new ArrayList<BucketEntry>(productOrder.getTotalSampleCount());

        Bucket bucket = null;
        if (bucketName != null) {
            bucket = bucketDao.findByName(bucketName);
            if (bucket == null) {
                bucket = new Bucket(new WorkflowBucketDef(bucketName));
            }
        }

        for (ProductOrderSample currSample : productOrder.getSamples()) {
            TwoDBarcodedTube newTube = new TwoDBarcodedTube("R" + currSample.getSampleName());
            newTube.addSample(new MercurySample(productOrder.getBusinessKey(), currSample.getSampleName()));
            tubes.add(newTube);
            barcodes.add(newTube.getLabel());
            if (bucketName != null && bucket.findEntry(newTube) == null) {
                bucket.addEntry(productOrder.getBusinessKey(), newTube);
            }
        }

        LabBatch testBatch = new LabBatch(" ", tubes);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                new LinkedHashMap<String, TwoDBarcodedTube>(productOrder.getTotalSampleCount());

        if (bucketName != null) {
            bucketDao.persist(bucket);
            bucketDao.flush();
            bucketDao.clear();
        } else {
            vesselDao.persistAll(new ArrayList<Object>(tubes));
            vesselDao.flush();
            vesselDao.clear();
        }

        for (String barcode : barcodes) {
            LabVessel foundTube = vesselDao.findByIdentifier(barcode);
            mapBarcodeToTube.put(barcode, (TwoDBarcodedTube) foundTube);
        }
        return mapBarcodeToTube;
    }

}

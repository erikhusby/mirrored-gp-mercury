package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.easymock.EasyMock;
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

import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.*;

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
    private BettaLimsMessageFactory bettaLimsMessageFactory;

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private MercuryOrSquidRouter mosRouter;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    private ProductOrder testExExOrder;
    private ProductOrder squidProductOrder;
    private String       squidPdoJiraKey;

    private String             exExJiraKey;
    private BettalimsConnector mockConnector;
    private String             testPrefix;
    private String             ligationBarcode;
    private LabEventTest.BuildIndexPlate ligationPlate;

    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        if (bettalimsMessageResource == null) {
            return;
        }

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        utx.begin();

        testExExOrder =
                poDao.findByWorkflowName(WorkflowConfig.WorkflowName.EXOME_EXPRESS.getWorkflowName()).iterator().next();
        exExJiraKey = testExExOrder.getBusinessKey();

        squidProductOrder =
                poDao.findByWorkflowName(WorkflowConfig.WorkflowName.WHOLE_GENOME.getWorkflowName()).iterator().next();
        squidPdoJiraKey = squidProductOrder.getBusinessKey();

        testPrefix = "bcode";
        ligationBarcode = "ligationPlate" + testPrefix;


        ligationPlate = new LabEventTest.BuildIndexPlate(ligationBarcode).invoke(molecularIndexingSchemeDao);
        vesselDao.persist(ligationPlate.getIndexPlate());
        vesselDao.flush();
        vesselDao.clear();

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

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(testExExOrder, "Shearing Bucket", bucketDao, vesselDao);

        List<TwoDBarcodedTube> bucketTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values());

        String rackBarcode = "REXEX" + (new Date()).toString();

        TubeFormation covarisSourceRack = makeTubeFormation(
                                                              new VesselPosition[] {A01, A02, A03, A04, A05, A06, A07,
                                                                                    A08, A09,
                                                                                    A10, A11, A12},
                                                              new TwoDBarcodedTube[] {bucketTubes.get(0),
                                                                                      bucketTubes.get(1),
                                                                                      bucketTubes.get(2),
                                                                                      bucketTubes.get(3),
                                                                                      bucketTubes.get(4),
                                                                                      bucketTubes.get(5),
                                                                                      bucketTubes.get(6),
                                                                                      bucketTubes.get(7),
                                                                                      bucketTubes.get(8),
                                                                                      bucketTubes.get(9),
                                                                                      bucketTubes.get(10),
                                                                                      bucketTubes.get(11)});

        covarisSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(covarisSourceRack);
        vesselDao.flush();
        vesselDao.clear();



        String covarisRackBarCode = "Shearing" + testPrefix;
        PlateTransferEventType covarisLoadEventJaxb =
                bettaLimsMessageFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                covarisRackBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().add(covarisLoadEventJaxb);
        bettaLimsMessageFactory.advanceTime();
        String xmlMessage = BettaLimsMessageFactory.marshal(msg);

        bettalimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate finalItem =
                (StaticPlate) vesselDao.findByIdentifier(covarisLoadEventJaxb.getPlate().getBarcode());

        Assert.assertTrue(finalItem.getEvents().size() > 0);

        PlateTransferEventType ligationCleanupJaxb =
                bettaLimsMessageFactory
                        .buildPlateToPlate(LabEventType.INDEXED_ADAPTER_LIGATION.getName(), ligationBarcode,
                                                  covarisLoadEventJaxb.getPlate().getBarcode());

        BettaLIMSMessage msgLigate = new BettaLIMSMessage();
        msgLigate.getPlateTransferEvent().add(ligationCleanupJaxb);
        bettaLimsMessageFactory.advanceTime();
        String ligateXmlMessage = BettaLimsMessageFactory.marshal(msgLigate);

        bettalimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate ligatedPlate =
                (StaticPlate) vesselDao.findByIdentifier(ligationCleanupJaxb.getPlate().getBarcode());

        Assert.assertTrue(ligatedPlate.getEvents().size() > 1);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressTubeEvent() throws Exception {

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, null, bucketDao, vesselDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        LabEventTest.PreFlightJaxbBuilder jaxbBuilder =
                new LabEventTest.PreFlightJaxbBuilder(bettaLimsMessageFactory, "",
                                                             new ArrayList<String>(mapBarcodeToTube.keySet()));
        jaxbBuilder.invoke();

        for (BettaLIMSMessage msg : jaxbBuilder.getMessageList()) {
            msg.setMode(null);

            String xmlMessage = BettaLimsMessageFactory.marshal(msg);

            bettalimsMessageResource.processMessage(xmlMessage);
            vesselDao.flush();
            vesselDao.clear();
        }

        EasyMock.verify(mockConnector);

        RackOfTubes finalItem = (RackOfTubes) vesselDao.findByIdentifier(jaxbBuilder.getRackBarcode());
        Assert.assertNull(finalItem);
        //        TubeFormation finalTubes = finalItem.getTubeFormations().iterator().next();
        //
        //        Assert.assertTrue(finalTubes.getEvents().size() == 0);
    }

    public static Map<String, TwoDBarcodedTube> buildVesselsForPdo(ProductOrder productOrder, String bucketName,
                                                                   BucketDao bucketDao1, LabVesselDao vesselDao1) {

        Set<LabVessel> tubes = new HashSet<LabVessel>(productOrder.getTotalSampleCount());

        List<String> barcodes = new ArrayList<String>(productOrder.getTotalSampleCount());

        List<BucketEntry> bucketEntries = new ArrayList<BucketEntry>(productOrder.getTotalSampleCount());

        Bucket bucket = null;
        if (bucketName != null) {
            bucket = bucketDao1.findByName(bucketName);
            if (bucket == null) {
                bucket = new Bucket(new WorkflowBucketDef(bucketName));
            }
        }

        for (ProductOrderSample currSample : productOrder.getSamples()) {

            TwoDBarcodedTube newTube = (TwoDBarcodedTube) vesselDao1.findByIdentifier("R" + currSample.getSampleName());
            if (newTube == null) {
                newTube = new TwoDBarcodedTube("R" + currSample.getSampleName());
            }
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
            bucketDao1.persist(bucket);
            bucketDao1.flush();
            bucketDao1.clear();
        } else {
            vesselDao1.persistAll(new ArrayList<Object>(tubes));
            vesselDao1.flush();
            vesselDao1.clear();
        }

        for (String barcode : barcodes) {
            LabVessel foundTube = vesselDao1.findByIdentifier(barcode);
            mapBarcodeToTube.put(barcode, (TwoDBarcodedTube) foundTube);
        }
        return mapBarcodeToTube;
    }

}

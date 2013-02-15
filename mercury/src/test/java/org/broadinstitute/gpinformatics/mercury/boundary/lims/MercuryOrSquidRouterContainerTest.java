package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import junit.framework.Assert;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStoreStub;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.LabEventTestFactory.makeTubeFormation;
import static org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition.*;

/**
 * @author Scott Matthews
 *         Date: 2/7/13
 *         Time: 11:10 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryOrSquidRouterContainerTest extends Arquillian {

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

    private String                       exExJiraKey;
    private BettalimsConnector           mockConnector;
    private String                       testPrefix;
    private String                       ligationBarcode;
    private LabEventTest.BuildIndexPlate ligationPlate;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, Stub.class, WsMessageStoreStub.class);
    }


    @BeforeMethod(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        if (bettalimsMessageResource == null) {
            return;
        }

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
    public void testExomeExpressEvents() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                         //                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"))
                .andThrow(new InformaticsServiceException("This methodShould have been thrown")).anyTimes();

        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        testExExOrder = poDao.findByBusinessKey(exExJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(testExExOrder, "Shearing Bucket", bucketDao, vesselDao);

        List<TwoDBarcodedTube> bucketTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values());

        String rackBarcode = "REXEX" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());


        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =

                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));
        shearingSourceRack.addRackOfTubes(new

                                                  RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96)
                                         );

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().add(shearingEventJaxb);
        bettaLimsMessageFactory.advanceTime();
        String xmlMessage = BettaLimsMessageFactory.marshal(msg);

        bettalimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());
        Assert.assertNotNull(shearPlate);

        // Adapter Ligation (Plate to Plate -- where the the Target is the item to validate, not the source)
        PlateTransferEventType ligationCleanupJaxb =
                bettaLimsMessageFactory
                        .buildPlateToPlate(LabEventType.INDEXED_ADAPTER_LIGATION.getName(), ligationBarcode,
                                                  shearingEventJaxb.getPlate().getBarcode());

        BettaLIMSMessage msgLigate = new BettaLIMSMessage();
        msgLigate.getPlateTransferEvent().add(ligationCleanupJaxb);
        bettaLimsMessageFactory.advanceTime();
        String ligateXmlMessage = BettaLimsMessageFactory.marshal(msgLigate);

        bettalimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate ligatedPlate =
                (StaticPlate) vesselDao.findByIdentifier(ligationCleanupJaxb.getPlate().getBarcode());
        Assert.assertNotNull(ligatedPlate);

        List<String> pondRegBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= 24 / 2; rackPosition++) {
            pondRegBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }

        //Pond Registration (Plate to Rack)
        String pondRegRackBarcode = "PondRegRack" + testPrefix;
        PlateTransferEventType pondRegJaxb =
                bettaLimsMessageFactory.buildPlateToRack(LabEventType.POND_REGISTRATION.getName(),
                                                                shearPlate.getLabel(), pondRegRackBarcode,
                                                                pondRegBarcodes);

        BettaLIMSMessage pondRegMsg = new BettaLIMSMessage();
        pondRegMsg.getPlateTransferEvent().add(pondRegJaxb);
        bettaLimsMessageFactory.advanceTime();
        String pondRegMessage = BettaLimsMessageFactory.marshal(pondRegMsg);

        bettalimsMessageResource.processMessage(pondRegMessage);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());
        Assert.assertNotNull(pondRack);
        TubeFormation pondTube = pondRack.getTubeFormations().iterator().next();

        // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<BettaLimsMessageFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<BettaLimsMessageFactory.CherryPick>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks.add(new BettaLimsMessageFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
                                                                                   stripTubeHolderBarcode,
                                                                                   Character.toString(
                                                                                                             (char) ('A' + rackPosition)) + "01"));
        }

        String stripTubeBarcode = "StripTube" + testPrefix + "1";
        PlateCherryPickEvent stripTubeTransferJaxb =
                bettaLimsMessageFactory.buildCherryPickToStripTube(LabEventType.STRIP_TUBE_B_TRANSFER.getName(),
                                                                          Arrays.asList(pondRegJaxb.getPlate()
                                                                                                   .getBarcode()),
                                                                          Arrays.asList(Arrays.asList(pondRegJaxb
                                                                                                              .getPositionMap()
                                                                                                              .getReceptacle()
                                                                                                              .iterator()
                                                                                                              .next()
                                                                                                              .getBarcode())),
                                                                          stripTubeHolderBarcode,
                                                                          Arrays.asList(stripTubeBarcode),
                                                                          stripTubeCherryPicks);

        BettaLIMSMessage stBMsg = new BettaLIMSMessage();
        stBMsg.getPlateCherryPickEvent().add(stripTubeTransferJaxb);
        bettaLimsMessageFactory.advanceTime();
        String stBMessage = BettaLimsMessageFactory.marshal(stBMsg);

        bettalimsMessageResource.processMessage(stBMessage);
        vesselDao.flush();
        vesselDao.clear();

        StripTube stripTube = (StripTube) vesselDao.findByIdentifier(stripTubeBarcode);
        Assert.assertNotNull(stripTube);

        // FlowcellTransfer      (Strip tube to Flowcell)
        String flowcellBarcode = "Flowcell" + testPrefix;
        PlateTransferEventType flowcellTransferJaxb =
                bettaLimsMessageFactory.buildStripTubeToFlowcell(LabEventType.FLOWCELL_TRANSFER.getName(),
                                                                        stripTubeBarcode, flowcellBarcode);

        BettaLIMSMessage fcellMsg = new BettaLIMSMessage();
        fcellMsg.getPlateTransferEvent().add(flowcellTransferJaxb);
        bettaLimsMessageFactory.advanceTime();
        String fcellMessage = BettaLimsMessageFactory.marshal(fcellMsg);

        bettalimsMessageResource.processMessage(fcellMessage);
        vesselDao.flush();
        vesselDao.clear();

        IlluminaFlowcell flowcell =
                (IlluminaFlowcell) vesselDao.findByIdentifier(flowcellTransferJaxb.getPlate().getBarcode());
        Assert.assertNotNull(flowcell);

        //        //Load Flowcell   (Just FLowcell)
        //        PlateEventType flowcellLoadJaxb =
        //                bettaLimsMessageFactory.buildFlowcellEvent(LabEventType.FLOWCELL_LOADED.getName(),
        //                                                                  flowcellTransferJaxb.getPlate().getBarcode());
        //
        //        BettaLIMSMessage fcellLoadMsg = new BettaLIMSMessage();
        //        fcellLoadMsg.getPlateEvent().add(flowcellLoadJaxb);
        //        bettaLimsMessageFactory.advanceTime();
        //        String fcellLoadMessage = BettaLimsMessageFactory.marshal(fcellLoadMsg);
        //
        //        bettalimsMessageResource.processMessage(fcellLoadMessage);
        //        vesselDao.flush();
        //        vesselDao.clear();
        //
        //        IlluminaFlowcell flowcellLoadEntity =
        //                (IlluminaFlowcell) vesselDao.findByIdentifier(flowcellLoadJaxb.getPlate().getBarcode());
        //        Assert.assertNotNull(flowcellLoadEntity);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressTubeEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, null, bucketDao, vesselDao);

        List<TwoDBarcodedTube> bucketTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values());

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96) );

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageFactory.advanceTime();
        String xmlMessage = BettaLimsMessageFactory.marshal(msg);

        bettalimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        EasyMock.verify(mockConnector);

        Assert.assertNull(shearPlate);

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

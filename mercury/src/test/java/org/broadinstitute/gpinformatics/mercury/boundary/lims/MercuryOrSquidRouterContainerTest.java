package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettalimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.infrastructure.ws.WsMessageStoreStub;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettalimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
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
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowName;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.easymock.EasyMock;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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
import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;

/**
 * @author Scott Matthews
 *         Date: 2/7/13
 *         Time: 11:10 PM
 */
@Test(groups = TestGroups.EXTERNAL_INTEGRATION)
public class MercuryOrSquidRouterContainerTest extends Arquillian {

    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    UserTransaction utx;

    @Inject
    private ProductOrderDao poDao;

    @Inject
    private LabVesselDao vesselDao;

    private BettaLimsMessageTestFactory bettaLimsMessageTestFactory = new BettaLimsMessageTestFactory(true);

    @Inject
    private BettalimsMessageResource bettalimsMessageResource;

    @Inject
    private BucketDao bucketDao;

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
        return DeploymentBuilder.buildMercuryWarWithAlternatives(DEV, WsMessageStoreStub.class);
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
                poDao.findByWorkflowName(WorkflowName.EXOME_EXPRESS.getWorkflowName()).iterator().next();
        exExJiraKey = testExExOrder.getBusinessKey();

        squidProductOrder =
                poDao.findByWorkflowName(WorkflowName.WHOLE_GENOME.getWorkflowName()).iterator().next();
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

    // todo this was changed from SQUID to WORKFLOW_DEPENDENT, need another scenario that tests SQUID
    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testExomeExpressEvents() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
//        .andThrow(new InformaticsServiceException("This methodShould have been thrown")).anyTimes();

        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        testExExOrder = poDao.findByBusinessKey(exExJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(testExExOrder, vesselDao, "Shearing Bucket", bucketDao);

        String rackBarcode = "REXEX" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =

                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));
        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().add(shearingEventJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String xmlMessage = BettaLimsMessageTestFactory.marshal(msg);

        bettalimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());
        Assert.assertNotNull(shearPlate);
//        Assert.assertNull(shearPlate);

        // Adapter Ligation (Plate to Plate -- where the the Target is the item to validate, not the source)
        PlateTransferEventType ligationCleanupJaxb =
                bettaLimsMessageTestFactory
                        .buildPlateToPlate(LabEventType.INDEXED_ADAPTER_LIGATION.getName(), ligationBarcode,
                                                  shearingEventJaxb.getPlate().getBarcode());

        BettaLIMSMessage msgLigate = new BettaLIMSMessage();
        msgLigate.getPlateTransferEvent().add(ligationCleanupJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String ligateXmlMessage = BettaLimsMessageTestFactory.marshal(msgLigate);

        bettalimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate ligatedPlate =
                (StaticPlate) vesselDao.findByIdentifier(ligationCleanupJaxb.getPlate().getBarcode());
        Assert.assertNotNull(ligatedPlate);
//        Assert.assertNull(ligatedPlate);

        EasyMock.verify(mockConnector);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressTubesToPlateEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();
        String xmlMessage = BettaLimsMessageTestFactory.marshal(msg);

        bettalimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        EasyMock.verify(mockConnector);

        Assert.assertNull(shearPlate);

    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressIndexPlateLigationEvent() throws Exception {
        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        /*
            Calling shearing with bypassing the routing call just to get a plate *
         */
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        PlateTransferEventType ligationCleanupJaxb =
                bettaLimsMessageTestFactory
                        .buildPlateToPlate(LabEventType.INDEXED_ADAPTER_LIGATION.getName(), ligationBarcode,
                                                  shearingEventJaxb.getPlate().getBarcode());

        BettaLIMSMessage msgLigate = new BettaLIMSMessage();
        msgLigate.getPlateTransferEvent().add(ligationCleanupJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String ligateXmlMessage = BettaLimsMessageTestFactory.marshal(msgLigate);

        bettalimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        EasyMock.verify(mockConnector);

    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void  testNonExomeExpressPlateToRackEvent() throws Exception {



        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        /*
            Calling shearing with bypassing the routing call just to get a plate *
         */
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= 24 / 2; rackPosition++) {
            pondRegBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }

        String pondRegRackBarcode = "PondRegRack" + testPrefix;
        PlateTransferEventType pondRegJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack(LabEventType.POND_REGISTRATION.getName(),
                                                                shearPlate.getLabel(), pondRegRackBarcode,
                                                                pondRegBarcodes);

        BettaLIMSMessage pondRegMsg = new BettaLIMSMessage();
        pondRegMsg.getPlateTransferEvent().add(pondRegJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String pondRegMessage = BettaLimsMessageTestFactory.marshal(pondRegMsg);

        bettalimsMessageResource.processMessage(pondRegMessage);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());
        Assert.assertNull(pondRack);

        EasyMock.verify(mockConnector);
    }

    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressRackToStripTubeBEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        /*
            Calling shearing with bypassing the routing call just to get a plate *
         */
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= 24 / 2; rackPosition++) {
            pondRegBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }

        String pondRegRackBarcode = "PondRegRack" + testPrefix;
        PlateTransferEventType pondRegJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack(LabEventType.POND_REGISTRATION.getName(),
                                                                shearPlate.getLabel(), pondRegRackBarcode,
                                                                pondRegBarcodes);

        BettaLIMSMessage pondRegMsg = new BettaLIMSMessage();
        pondRegMsg.getPlateTransferEvent().add(pondRegJaxb);
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());


         // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<LabEventFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<LabEventFactory.CherryPick>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks.add(new LabEventFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
                                                                                   stripTubeHolderBarcode,
                                                                                   Character.toString(
                                                                                                             (char) ('A' + rackPosition)) + "01"));
        }

        String stripTubeBarcode = "StripTube" + testPrefix + "1";
        PlateCherryPickEvent stripTubeTransferJaxb =
                bettaLimsMessageTestFactory.buildCherryPickToStripTube(LabEventType.STRIP_TUBE_B_TRANSFER.getName(),
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
        bettaLimsMessageTestFactory.advanceTime();
        String stBMessage = BettaLimsMessageTestFactory.marshal(stBMsg);

        bettalimsMessageResource.processMessage(stBMessage);
        vesselDao.flush();
        vesselDao.clear();

        StripTube stripTube = (StripTube) vesselDao.findByIdentifier(stripTubeBarcode);
        Assert.assertNull(stripTube);

        EasyMock.verify(mockConnector);
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressStripTubeBToFlowcellEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        /*
            Calling shearing with bypassing the routing call just to get a plate *
         */
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= 24 / 2; rackPosition++) {
            pondRegBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }

        String pondRegRackBarcode = "PondRegRack" + testPrefix;
        PlateTransferEventType pondRegJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack(LabEventType.POND_REGISTRATION.getName(),
                                                                shearPlate.getLabel(), pondRegRackBarcode,
                                                                pondRegBarcodes);

        BettaLIMSMessage pondRegMsg = new BettaLIMSMessage();
        pondRegMsg.getPlateTransferEvent().add(pondRegJaxb);
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());

         // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<LabEventFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<LabEventFactory.CherryPick>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks.add(new LabEventFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
                                                                                   stripTubeHolderBarcode,
                                                                                   Character.toString(
                                                                                                             (char) ('A' + rackPosition)) + "01"));
        }

        String stripTubeBarcode = "StripTube" + testPrefix + "1";
        PlateCherryPickEvent stripTubeTransferJaxb =
                bettaLimsMessageTestFactory.buildCherryPickToStripTube(LabEventType.STRIP_TUBE_B_TRANSFER.getName(),
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
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(stBMsg);
        vesselDao.flush();
        vesselDao.clear();

        StripTube stripTube = (StripTube) vesselDao.findByIdentifier(stripTubeBarcode);



        // FlowcellTransfer      (Strip tube to Flowcell)
        String flowcellBarcode = "Flowcell" + testPrefix;
        PlateTransferEventType flowcellTransferJaxb =
                bettaLimsMessageTestFactory.buildStripTubeToFlowcell(LabEventType.FLOWCELL_TRANSFER.getName(),
                                                                        stripTubeBarcode, flowcellBarcode);

        BettaLIMSMessage fcellMsg = new BettaLIMSMessage();
        fcellMsg.getPlateTransferEvent().add(flowcellTransferJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String fcellMessage = BettaLimsMessageTestFactory.marshal(fcellMsg);

        bettalimsMessageResource.processMessage(fcellMessage);
        vesselDao.flush();
        vesselDao.clear();

        IlluminaFlowcell flowcell =
                (IlluminaFlowcell) vesselDao.findByIdentifier(flowcellTransferJaxb.getPlate().getBarcode());
        Assert.assertNull(flowcell);


        EasyMock.verify(mockConnector);
    }


    @Test(groups = TestGroups.EXTERNAL_INTEGRATION)
    public void testNonExomeExpressFlowcellLoadEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettalimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettalimsConnector.BettalimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettalimsMessageResource.setBettalimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, TwoDBarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<VesselPosition>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                                         new ArrayList<TwoDBarcodedTube>(mapBarcodeToTube.values())
                                                 .toArray(new TwoDBarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        /*
            Calling shearing with bypassing the routing call just to get a plate *
         */
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                                                                new ArrayList<String>(mapBarcodeToTube.keySet()),
                                                                postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                                           add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<String>();
        for (int rackPosition = 1; rackPosition <= 24 / 2; rackPosition++) {
            pondRegBarcodes.add("NormCatch" + testPrefix + rackPosition);
        }

        String pondRegRackBarcode = "PondRegRack" + testPrefix;
        PlateTransferEventType pondRegJaxb =
                bettaLimsMessageTestFactory.buildPlateToRack(LabEventType.POND_REGISTRATION.getName(),
                                                                shearPlate.getLabel(), pondRegRackBarcode,
                                                                pondRegBarcodes);

        BettaLIMSMessage pondRegMsg = new BettaLIMSMessage();
        pondRegMsg.getPlateTransferEvent().add(pondRegJaxb);
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());

         // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<LabEventFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<LabEventFactory.CherryPick>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks.add(new LabEventFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
                                                                                   stripTubeHolderBarcode,
                                                                                   Character.toString(
                                                                                                             (char) ('A' + rackPosition)) + "01"));
        }

        String stripTubeBarcode = "StripTube" + testPrefix + "1";
        PlateCherryPickEvent stripTubeTransferJaxb =
                bettaLimsMessageTestFactory.buildCherryPickToStripTube(LabEventType.STRIP_TUBE_B_TRANSFER.getName(),
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
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(stBMsg);
        vesselDao.flush();
        vesselDao.clear();

        StripTube stripTube = (StripTube) vesselDao.findByIdentifier(stripTubeBarcode);



        // FlowcellTransfer      (Strip tube to Flowcell)
        String flowcellBarcode = "Flowcell" + testPrefix;
        PlateTransferEventType flowcellTransferJaxb =
                bettaLimsMessageTestFactory.buildStripTubeToFlowcell(LabEventType.FLOWCELL_TRANSFER.getName(),
                                                                        stripTubeBarcode, flowcellBarcode);

        BettaLIMSMessage fcellMsg = new BettaLIMSMessage();
        fcellMsg.getPlateTransferEvent().add(flowcellTransferJaxb);
        bettaLimsMessageTestFactory.advanceTime();

        bettalimsMessageResource.processMessage(fcellMsg);
        vesselDao.flush();
        vesselDao.clear();

        IlluminaFlowcell flowcell =
                (IlluminaFlowcell) vesselDao.findByIdentifier(flowcellTransferJaxb.getPlate().getBarcode());

        //Load Flowcell   (Just Flowcell)
        ReceptacleEventType flowcellLoadJaxb =
                bettaLimsMessageTestFactory.buildReceptacleEvent(LabEventType.FLOWCELL_LOADED.getName(),
                                                                  flowcellTransferJaxb.getPlate().getBarcode(), LabEventFactory.PHYS_TYPE_FLOWCELL);

        BettaLIMSMessage fcellLoadMsg = new BettaLIMSMessage();
        fcellLoadMsg.getReceptacleEvent().add(flowcellLoadJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String fcellLoadMessage = BettaLimsMessageTestFactory.marshal(fcellLoadMsg);

        bettalimsMessageResource.processMessage(fcellLoadMessage);
        vesselDao.flush();
        vesselDao.clear();

        EasyMock.verify(mockConnector);
    }






    /*
       Helper methods
    */

    /**
     * Assists in creating a set of Mercury Tubes based on an existing Product order.  This method will also, if need
     * be, add the created mercury vessels to a given Bucket to assist
     *
     * TODO SGM:  this could probably be broken up better to define an alternate method to make cleaner
     *
     * @param productOrder  Product order by which to determine the Samples to Create.
     * @param vesselDao1 Assists in finding the created Tubes
     * @param bucketName Optional:  Name of the Bucket to create.  If left null, bucket creation will be avoided
     * @param bucketDao1 Optional:  Dao to assist in finding/persisting to a bucket
     * @return
     */
    private static Map<String, TwoDBarcodedTube> buildVesselsForPdo(@Nonnull ProductOrder productOrder,
                                                                    @Nonnull LabVesselDao vesselDao1,
                                                                    @Nullable String bucketName,
                                                                    @Nullable BucketDao bucketDao1) {

        Set<LabVessel> tubes = new HashSet<LabVessel>(productOrder.getTotalSampleCount());

        List<String> barcodes = new ArrayList<String>(productOrder.getTotalSampleCount());

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
            newTube.addSample(new MercurySample(currSample.getSampleName()));
            tubes.add(newTube);
            barcodes.add(newTube.getLabel());
            if (bucketName != null && bucket.findEntry(newTube) == null) {
                bucket.addEntry(productOrder.getBusinessKey(), newTube);
            }
        }

        //THis will automatically add the batch to the tubes via the internal set methods
        new LabBatch(" ", tubes, LabBatch.LabBatchType.WORKFLOW);

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

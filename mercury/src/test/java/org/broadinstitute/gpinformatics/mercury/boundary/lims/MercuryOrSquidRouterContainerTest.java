package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bettalims.BettaLimsConnector;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.BettaLimsMessageTestFactory;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateCherryPickEvent;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StripTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowBucketDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
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
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.test.dbfree.LabEventTestFactory.makeTubeFormation;

/**
 * @author Scott Matthews
 *         Date: 2/7/13
 *         Time: 11:10 PM
 */
@Test(groups = TestGroups.STUBBY)
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
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private BucketDao bucketDao;

    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao;

    @Inject
    private MolecularIndexDao molecularIndexDao;

    private ProductOrder testExExOrder;
    private ProductOrder squidProductOrder;
    private String squidPdoJiraKey;

    private String exExJiraKey;
    private BettaLimsConnector mockConnector;
    private String testPrefix;
    private String ligationBarcode;
    private StaticPlate ligationPlate;

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar();
    }

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {

        if (utx == null) {
            return;
        }

        if (bettaLimsMessageResource == null) {
            return;
        }

        utx.begin();

        testExExOrder = poDao.findByWorkflow(Workflow.AGILENT_EXOME_EXPRESS).iterator().next();
        exExJiraKey = testExExOrder.getBusinessKey();

        squidProductOrder = poDao.findByWorkflow(Workflow.WHOLE_GENOME).iterator().next();
        squidPdoJiraKey = squidProductOrder.getBusinessKey();

        testPrefix = "bcode";
        ligationBarcode = "ligationPlate" + testPrefix;

        ligationPlate = LabEventTest.buildIndexPlate(molecularIndexingSchemeDao, molecularIndexDao,
                Collections.singletonList(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7),
                Collections.singletonList(ligationBarcode)).get(0);
        vesselDao.persist(ligationPlate);
        vesselDao.flush();
        vesselDao.clear();

    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();

    }

    // todo this was changed from SQUID to WORKFLOW_DEPENDENT, need another scenario that tests SQUID
    @Test(groups = TestGroups.STUBBY)
    public void testExomeExpressEvents() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        testExExOrder = poDao.findByBusinessKey(exExJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(testExExOrder, vesselDao, "Shearing Bucket", bucketDao);

        String rackBarcode = "REXEX" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =

                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));
        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().add(shearingEventJaxb);
        bettaLimsMessageTestFactory.advanceTime();
        String xmlMessage = BettaLimsMessageTestFactory.marshal(msg);

        bettaLimsMessageResource.processMessage(xmlMessage);
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

        bettaLimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate ligatedPlate =
                (StaticPlate) vesselDao.findByIdentifier(ligationCleanupJaxb.getPlate().getBarcode());
        Assert.assertNotNull(ligatedPlate);
//        Assert.assertNull(ligatedPlate);

        EasyMock.verify(mockConnector);

    }

    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressTubesToPlateEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

        shearingSourceRack.addRackOfTubes(new RackOfTubes(rackBarcode, RackOfTubes.RackType.Matrix96));

        vesselDao.persist(shearingSourceRack);
        vesselDao.flush();
        vesselDao.clear();

        // Shearing Rack to Plate
        String postShearingPlateBarCode = "Shearing" + testPrefix;
        PlateTransferEventType shearingEventJaxb =
                bettaLimsMessageTestFactory.buildRackToPlate(LabEventType.SHEARING_TRANSFER.getName(), rackBarcode,
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();
        String xmlMessage = BettaLimsMessageTestFactory.marshal(msg);

        bettaLimsMessageResource.processMessage(xmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        EasyMock.verify(mockConnector);

        Assert.assertNull(shearPlate);

    }

    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressIndexPlateLigationEvent() throws Exception {
        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

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
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettaLimsMessageResource.processMessage(msg);
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

        bettaLimsMessageResource.processMessage(ligateXmlMessage);
        vesselDao.flush();
        vesselDao.clear();

        EasyMock.verify(mockConnector);

    }


    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressPlateToRackEvent() throws Exception {


        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

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
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettaLimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<>();
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

        bettaLimsMessageResource.processMessage(pondRegMessage);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());
        Assert.assertNull(pondRack);

        EasyMock.verify(mockConnector);
    }

    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressRackToStripTubeBEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

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
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettaLimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<>();
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

        bettaLimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());


        // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks
                    .add(new BettaLimsMessageTestFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
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

        bettaLimsMessageResource.processMessage(stBMessage);
        vesselDao.flush();
        vesselDao.clear();

        StripTube stripTube = (StripTube) vesselDao.findByIdentifier(stripTubeBarcode);
        Assert.assertNull(stripTube);

        EasyMock.verify(mockConnector);
    }


    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressStripTubeBToFlowcellEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

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
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettaLimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<>();
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

        bettaLimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());

        // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks
                    .add(new BettaLimsMessageTestFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
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

        bettaLimsMessageResource.processMessage(stBMsg);
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

        bettaLimsMessageResource.processMessage(fcellMessage);
        vesselDao.flush();
        vesselDao.clear();

        IlluminaFlowcell flowcell =
                (IlluminaFlowcell) vesselDao.findByIdentifier(flowcellTransferJaxb.getPlate().getBarcode());
        Assert.assertNull(flowcell);


        EasyMock.verify(mockConnector);
    }


    @Test(groups = TestGroups.STUBBY)
    public void testNonExomeExpressFlowcellLoadEvent() throws Exception {

        mockConnector = EasyMock.createNiceMock(BettaLimsConnector.class);

        EasyMock.expect(mockConnector.sendMessage(EasyMock.anyObject(String.class)))
                .andReturn(new BettaLimsConnector.BettaLimsResponse(200, "Success"));
        EasyMock.replay(mockConnector);
        bettaLimsMessageResource.setBettaLimsConnector(mockConnector);

        squidProductOrder = poDao.findByBusinessKey(squidPdoJiraKey);

        Map<String, BarcodedTube> mapBarcodeToTube =
                buildVesselsForPdo(squidProductOrder, vesselDao, null, bucketDao);

        String rackBarcode = "RSQUID" + (new Date()).toString();
        List<VesselPosition> vesselPositions = new ArrayList<>(mapBarcodeToTube.size());

        for (int sampleIdx = 0; sampleIdx < mapBarcodeToTube.size(); sampleIdx++) {
            vesselPositions.add(VesselPosition.values()[sampleIdx]);
        }

        TubeFormation shearingSourceRack =
                makeTubeFormation(vesselPositions.toArray(new VesselPosition[vesselPositions.size()]),
                        new ArrayList<>(mapBarcodeToTube.values())
                                .toArray(new BarcodedTube[mapBarcodeToTube.size()]));

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
                        new ArrayList<>(mapBarcodeToTube.keySet()),
                        postShearingPlateBarCode);

        BettaLIMSMessage msg = new BettaLIMSMessage();
        msg.getPlateTransferEvent().

                add(shearingEventJaxb);

        bettaLimsMessageTestFactory.advanceTime();

        bettaLimsMessageResource.processMessage(msg);
        vesselDao.flush();
        vesselDao.clear();

        StaticPlate shearPlate =
                (StaticPlate) vesselDao.findByIdentifier(shearingEventJaxb.getPlate().getBarcode());

        /*
            We now have a Plate that we can do a Plate to Rack utilizing the Routing calls
         */

        List<String> pondRegBarcodes = new ArrayList<>();
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

        bettaLimsMessageResource.processMessage(pondRegMsg);
        vesselDao.flush();
        vesselDao.clear();

        RackOfTubes pondRack = (RackOfTubes) vesselDao.findByIdentifier(pondRegJaxb.getPlate().getBarcode());

        // Strip Tube B Transfer  (Rack to Strip tube)
        String stripTubeHolderBarcode = "StripTubeHolder" + testPrefix;
        List<BettaLimsMessageTestFactory.CherryPick> stripTubeCherryPicks =
                new ArrayList<>();
        for (int rackPosition = 0; rackPosition < 8; rackPosition++) {
            stripTubeCherryPicks
                    .add(new BettaLimsMessageTestFactory.CherryPick(pondRegJaxb.getPlate().getBarcode(), "A01",
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

        bettaLimsMessageResource.processMessage(stBMsg);
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

        bettaLimsMessageResource.processMessage(fcellMsg);
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

        bettaLimsMessageResource.processMessage(fcellLoadMessage);
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
     * <p/>
     * TODO SGM:  this could probably be broken up better to define an alternate method to make cleaner
     *
     * @param productOrder Product order by which to determine the Samples to Create.
     * @param vesselDao1   Assists in finding the created Tubes
     * @param bucketName   Optional:  Name of the Bucket to create.  If left null, bucket creation will be avoided
     * @param bucketDao1   Optional:  Dao to assist in finding/persisting to a bucket
     *
     * @return
     */
    private Map<String, BarcodedTube> buildVesselsForPdo(@Nonnull ProductOrder productOrder,
                                                             @Nonnull LabVesselDao vesselDao1,
                                                             @Nullable String bucketName,
                                                             @Nullable BucketDao bucketDao1) {

        Date testSuffix = new Date();
        Set<LabVessel> tubes = new HashSet<>(productOrder.getTotalSampleCount());

        List<String> barcodes = new ArrayList<>(productOrder.getTotalSampleCount());

        Bucket bucket = null;
        if (bucketName != null) {
            bucket = bucketDao1.findByName(bucketName);
            if (bucket == null) {
                bucket = new Bucket(new WorkflowBucketDef(bucketName));
            }
        }

        for (ProductOrderSample currSample : productOrder.getSamples()) {

            BarcodedTube newTube = (BarcodedTube) vesselDao1.findByIdentifier("R" + currSample.getName());
            if (newTube == null) {
                newTube = new BarcodedTube("R" + currSample.getName());
            }
            newTube.addSample(new MercurySample(currSample.getName(), MercurySample.MetadataSource.BSP));
            tubes.add(newTube);
            barcodes.add(newTube.getLabel());
            if (bucketName != null && bucket.findEntry(newTube) == null) {
                bucket.addEntry(productOrder, newTube,
                        org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry.BucketEntryType.PDO_ENTRY);
            }
        }


        //THis will automatically add the batch to the tubes via the internal set methods
        LabBatch labBatch = new LabBatch("testBatch" + testSuffix.getTime(), tubes, LabBatch.LabBatchType.WORKFLOW);
        labBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

//        labBatchEjb.createLabBatchAndRemoveFromBucket(labBatch,"scottmat",bucketName,"HOME", CreateFields.IssueType.AGILENT_EXOME_EXPRESS);
        for (LabVessel currTube : tubes) {
            for (BucketEntry currentEntry : currTube.getBucketEntries()) {
                currentEntry.setStatus(BucketEntry.Status.Archived);
                currentEntry.setLabBatch(labBatch);
            }
        }

        Map<String, BarcodedTube> mapBarcodeToTube =
                new LinkedHashMap<>(productOrder.getTotalSampleCount());

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
            mapBarcodeToTube.put(barcode, (BarcodedTube) foundTube);
        }
        return mapBarcodeToTube;
    }

}

package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientProducer;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.plating.BSPManagerFactoryProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateTransferEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.bucket.BucketBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.bucket.BucketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventHandler;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.test.BettaLimsMessageFactory;
import org.broadinstitute.gpinformatics.mercury.test.LabEventTest;
import org.easymock.EasyMock;
import org.testng.Assert;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 3/14/13
 *         Time: 12:08 AM
 */
public class SolexaRunRoutingTest {

    private static final int NUM_POSITIONS_IN_RACK = 96;

    private static Map<String, ProductOrder> mapKeyToProductOrder = new HashMap<String, ProductOrder>();

    public void testWholeGenomeFlowcell() throws Exception {

        LabBatchEjb labBatchEJB = new LabBatchEjb();
        labBatchEJB.setAthenaClientService(AthenaClientProducer.stubInstance());
        labBatchEJB.setJiraService(JiraServiceProducer.stubInstance());

        LabVesselDao tubeDao = EasyMock.createNiceMock(LabVesselDao.class);
        labBatchEJB.setTubeDAO(tubeDao);

        JiraTicketDao mockJira = EasyMock.createNiceMock(JiraTicketDao.class);
        labBatchEJB.setJiraTicketDao(mockJira);

        LabBatchDAO labBatchDAO = EasyMock.createNiceMock(LabBatchDAO.class);
        labBatchEJB.setLabBatchDao(labBatchDAO);

        ProductOrder productOrder = AthenaClientServiceStub.buildWholeGenomeProductOrder(NUM_POSITIONS_IN_RACK);
        String jiraTicketKey = productOrder.getBusinessKey();

        mapKeyToProductOrder.put(jiraTicketKey, productOrder);

        // starting rack
        Map<String, TwoDBarcodedTube> mapBarcodeToTube = new LinkedHashMap<String, TwoDBarcodedTube>();
        int rackPosition = 1;
        for (ProductOrderSample poSample : productOrder.getSamples()) {
            String barcode = "R" + rackPosition;
            TwoDBarcodedTube bspAliquot = new TwoDBarcodedTube(barcode);
            bspAliquot.addSample(new MercurySample(jiraTicketKey, poSample.getSampleName()));
            mapBarcodeToTube.put(barcode, bspAliquot);
            rackPosition++;
        }
        final LabBatch workflowBatch = new LabBatch("whole Genome Batch",
                                                           new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                                           LabBatch.LabBatchType.WORKFLOW);
        labBatchEJB.createLabBatch(workflowBatch, "scottmat");

        BettaLimsMessageFactory bettaLimsMessageFactory = new BettaLimsMessageFactory();
        LabEventFactory labEventFactory = new LabEventFactory();
        //        labEventFactory.setLabEventRefDataFetcher(labEventRefDataFetcher);

        BucketDao mockBucketDao = EasyMock.createNiceMock(BucketDao.class);
        BucketBean bucketBeanEJB = new BucketBean(labEventFactory, JiraServiceProducer.stubInstance(), labBatchEJB);
        EasyMock.replay(mockBucketDao, tubeDao, mockJira, labBatchDAO);

        LabEventHandler labEventHandler =
                new LabEventHandler(new WorkflowLoader(), AthenaClientProducer
                                                                  .stubInstance(), bucketBeanEJB, mockBucketDao,
                                           new BSPUserList(BSPManagerFactoryProducer
                                                                   .stubInstance()));

        LabEventTest.PreFlightEntityBuilder preFlightEntityBuilder =
                new LabEventTest.PreFlightEntityBuilder(bettaLimsMessageFactory,
                                                               labEventFactory, labEventHandler,
                                                               mapBarcodeToTube, mapKeyToProductOrder).invoke();

        LabEventTest.ShearingEntityBuilder shearingEntityBuilder =
                new LabEventTest.ShearingEntityBuilder(mapBarcodeToTube, preFlightEntityBuilder.getTubeFormation(),
                                                              bettaLimsMessageFactory, labEventFactory, labEventHandler,
                                                              preFlightEntityBuilder.getRackBarcode()).invoke();

        LabEventTest.LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                new LabEventTest.LibraryConstructionEntityBuilder(
                                                                         bettaLimsMessageFactory, labEventFactory,
                                                                         labEventHandler,
                                                                         shearingEntityBuilder
                                                                                 .getShearingCleanupPlate(),
                                                                         shearingEntityBuilder
                                                                                 .getShearCleanPlateBarcode(),
                                                                         shearingEntityBuilder.getShearingPlate(),
                                                                         NUM_POSITIONS_IN_RACK).invoke();

        List<String> sageUnloadTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageUnloadTubeBarcodes.add("SageUnload" + i);
        }
        String sageUnloadBarcode = "SageUnload";
        Map<String, TwoDBarcodedTube> mapBarcodeToSageUnloadTubes = new HashMap<String, TwoDBarcodedTube>();
        RackOfTubes targetRackOfTubes = null;
        for (int i = 0; i < NUM_POSITIONS_IN_RACK / 4; i++) {
            // SageLoading
            String sageCassetteBarcode = "SageCassette" + i;
            PlateTransferEventType sageLoadingJaxb = bettaLimsMessageFactory.buildRackToPlate("SageLoading",
                                                                                                     libraryConstructionEntityBuilder
                                                                                                             .getPondRegRackBarcode(),
                                                                                                     libraryConstructionEntityBuilder
                                                                                                             .getPondRegTubeBarcodes()
                                                                                                             .subList(i * 4,
                                                                                                                             i * 4 + 4),
                                                                                                     sageCassetteBarcode);
            // todo jmt SAGE section
            LabEvent sageLoadingEntity = labEventFactory.buildFromBettaLimsRackToPlateDbFree(sageLoadingJaxb,
                                                                                                    libraryConstructionEntityBuilder
                                                                                                            .getPondRegRack(),
                                                                                                    null);
            labEventHandler.processEvent(sageLoadingEntity);
            StaticPlate sageCassette = (StaticPlate) sageLoadingEntity.getTargetLabVessels().iterator().next();

            // SageLoaded

            // SageUnloading
            PlateTransferEventType sageUnloadingJaxb = bettaLimsMessageFactory.buildPlateToRack("SageUnloading",
                                                                                                       sageCassetteBarcode,
                                                                                                       sageUnloadBarcode,
                                                                                                       sageUnloadTubeBarcodes
                                                                                                               .subList(i * 4,
                                                                                                                               i * 4 + 4));
            LabEvent sageUnloadEntity = labEventFactory.buildFromBettaLimsPlateToRackDbFree(sageUnloadingJaxb,
                                                                                                   sageCassette,
                                                                                                   mapBarcodeToSageUnloadTubes,
                                                                                                   targetRackOfTubes);
            labEventHandler.processEvent(sageUnloadEntity);
            sageUnloadEntity.getTargetLabVessels().iterator().next();
        }

        // SageCleanup
        List<String> sageCleanupTubeBarcodes = new ArrayList<String>();
        for (int i = 1; i <= NUM_POSITIONS_IN_RACK; i++) {
            sageCleanupTubeBarcodes.add("SageCleanup" + i);
        }
        String sageCleanupBarcode = "SageCleanup";
        PlateTransferEventType sageCleanupJaxb =
                bettaLimsMessageFactory.buildRackToRack("SageCleanup", sageUnloadBarcode,
                                                               sageUnloadTubeBarcodes, sageCleanupBarcode,
                                                               sageCleanupTubeBarcodes);
        Map<VesselPosition, TwoDBarcodedTube> mapPositionToTube = new HashMap<VesselPosition, TwoDBarcodedTube>();
        List<TwoDBarcodedTube> sageUnloadTubes = new ArrayList<TwoDBarcodedTube>(mapBarcodeToSageUnloadTubes.values());
        for (int i = 0; i < NUM_POSITIONS_IN_RACK; i++) {
            mapPositionToTube.put(VesselPosition
                                          .getByName(bettaLimsMessageFactory.buildWellName(i + 1)),
                                         sageUnloadTubes.get(i));
        }
        TubeFormation sageUnloadRackRearrayed = new TubeFormation(mapPositionToTube, RackOfTubes.RackType.Matrix96);
        sageUnloadRackRearrayed.addRackOfTubes(new RackOfTubes("sageUnloadRearray", RackOfTubes.RackType.Matrix96));
        LabEvent sageCleanupEntity = labEventFactory.buildFromBettaLimsRackToRackDbFree(sageCleanupJaxb,
                                                                                               sageUnloadRackRearrayed,
                                                                                               new HashMap<String, TwoDBarcodedTube>(),
                                                                                               targetRackOfTubes);
        labEventHandler.processEvent(sageCleanupEntity);
        TubeFormation sageCleanupRack = (TubeFormation) sageCleanupEntity.getTargetLabVessels().iterator().next();
        Assert.assertEquals(sageCleanupRack.getSampleInstances().size(), NUM_POSITIONS_IN_RACK,
                                   "Wrong number of sage cleanup samples");

        LabEventTest.QtpEntityBuilder qtpEntityBuilder =
                new LabEventTest.QtpEntityBuilder(bettaLimsMessageFactory, labEventFactory, labEventHandler,
                                                         sageCleanupRack,
                                                         sageCleanupBarcode, sageCleanupTubeBarcodes,
                                                         mapBarcodeToSageUnloadTubes);

        qtpEntityBuilder.invoke();

        Map.Entry<String, TwoDBarcodedTube> stringTwoDBarcodedTubeEntry = mapBarcodeToTube.entrySet().iterator().next();
        LabEventTest.ListTransfersFromStart transferTraverserCriteria = new LabEventTest.ListTransfersFromStart();
        stringTwoDBarcodedTubeEntry.getValue().evaluateCriteria(transferTraverserCriteria,
                                                                       TransferTraverserCriteria.TraversalDirection.Descendants);
        List<String> labEventNames = transferTraverserCriteria.getLabEventNames();

        IlluminaFlowcell flowcell = qtpEntityBuilder.getIlluminaFlowcell();
        Date runDate = new Date();

        SolexaRunBean runBean =
                new SolexaRunBean(flowcell.getCartridgeBarcode(),
                                         flowcell.getCartridgeBarcode() + IlluminaSequencingRun.RUNFORMAT
                                                                                               .format(runDate),
                                         runDate, "Superman",
                                         File.createTempFile("tempRun" + IlluminaSequencingRun.RUNFORMAT
                                                                                              .format(runDate), ".txt")
                                             .getAbsolutePath(), null);

        IlluminaSequencingRunDao runDao = EasyMock.createMock(IlluminaSequencingRunDao.class);
//        EasyMock.expect()

//        SolexaRunResource runResource = new SolexaRunResource()


    }

}

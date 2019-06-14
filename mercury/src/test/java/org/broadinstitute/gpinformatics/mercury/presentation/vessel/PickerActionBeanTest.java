package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.storage.PickerActionBean;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class PickerActionBeanTest extends BaseEventTest {

    private PickerActionBean actionBean;
    private LabVesselDao labVesselDaoMock;
    private LabBatchDao labBatchMock;

    public static final String BARCODE_SUFFIX = "1";
    private static final BigDecimal BIG_DECIMAL_12_33 = new BigDecimal("12.33");
    private static final BigDecimal BIG_DECIMAL_7_77 = new BigDecimal("7.77");
    private BarcodedTube denatureTube2500 = null;
    private Date runDate;
    private LabBatch fctBatch;
    private LabBatch workflowBatch;
    private ProductOrder productOrder;
    private RackOfTubes denatureTube2500Rack;

    @Override
    @BeforeMethod
    public void setUp() {
        expectedRouting = SystemRouter.System.MERCURY;
        super.setUp();

        actionBean = new PickerActionBean();
        actionBean.setContext(new CoreActionBeanContext());
        labVesselDaoMock = mock(LabVesselDao.class);
        actionBean.setLabVesselDao(labVesselDaoMock);
        labBatchMock = mock(LabBatchDao.class);
        actionBean.setLabBatchDao(labBatchMock);

        productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, BARCODE_SUFFIX);
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                String.valueOf(runDate.getTime()),
                BARCODE_SUFFIX, true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), BARCODE_SUFFIX);
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                        exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                        exomeExpressShearingEntityBuilder.getShearingPlate(), BARCODE_SUFFIX);
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), BARCODE_SUFFIX);
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");
        denatureTube2500 = qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        TubeFormation tubeFormation =
                (TubeFormation) qtpEntityBuilder.getDenatureRack().getContainerRole().getEmbedder();
        denatureTube2500Rack = tubeFormation.getRacksOfTubes().iterator().next();

        fctBatch = new LabBatch("FCT-3", LabBatch.LabBatchType.FCT,
                IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell, denatureTube2500, BIG_DECIMAL_12_33);
        when(labBatchMock.findByListIdentifier(Collections.singletonList(fctBatch.getBatchName()))).thenReturn(
                Collections.singletonList(fctBatch)
        );

        when(labVesselDaoMock.findByListIdentifiers(Collections.singletonList(denatureTube2500.getLabel()))).thenReturn(
                Collections.singletonList((LabVessel)denatureTube2500)
        );

        StorageLocation freezer = new StorageLocation("Freezer", StorageLocation.LocationType.FREEZER, null);
        StorageLocation shelf = new StorageLocation("Shelf 1", StorageLocation.LocationType.SHELF, freezer);
        denatureTube2500Rack.setStorageLocation(shelf);
        denatureTube2500.setStorageLocation(shelf);
        Map<VesselPosition, BarcodedTube> mapPositionToTube = new HashMap<>();
        mapPositionToTube.put(VesselPosition.A01, denatureTube2500);
        LabEvent labEvent = new LabEvent(LabEventType.STORAGE_CHECK_IN, new Date(),
                "UnitTest", 1L, 1L, "UnitTest");
        labEvent.setInPlaceLabVessel(denatureTube2500.getContainers().iterator().next());
        labEvent.setAncillaryInPlaceVessel(denatureTube2500Rack);
        denatureTube2500.getContainers().iterator().next().addInPlaceEvent(labEvent);
    }

    public void pickLabBatch() {
        actionBean.setBarcodes(fctBatch.getBatchName());
        actionBean.setSearchType(PickerActionBean.SearchType.LAB_BATCH);
        actionBean.search();
        ConfigurableList.ResultList resultList = actionBean.getResultList();
        Assert.assertNotNull(resultList);
        ConfigurableList.ResultRow resultRow = resultList.getResultRows().get(0);
        String freezerLocation = resultRow.getRenderableCells().get(0);
        Assert.assertEquals("Freezer > Shelf 1[DenatureRack1]", freezerLocation);
        Assert.assertEquals(resultRow.getRenderableCells().get(3), denatureTube2500.getLabel());
    }

    public void pickLabVessel() {
        actionBean.setBarcodes(denatureTube2500.getLabel());
        actionBean.setSearchType(PickerActionBean.SearchType.LAB_VESSEL_BARCODE);
        actionBean.search();
        ConfigurableList.ResultList resultList = actionBean.getResultList();
        Assert.assertNotNull(resultList);
        ConfigurableList.ResultRow resultRow = resultList.getResultRows().get(0);
        String freezerLocation = resultRow.getRenderableCells().get(0);
        Assert.assertEquals("Freezer > Shelf 1[DenatureRack1]", freezerLocation);
        Assert.assertEquals(resultRow.getRenderableCells().get(3), denatureTube2500.getLabel());
    }
}
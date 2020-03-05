package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.mock.MockRoundtrip;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemOfRecord;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.junit.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE, singleThreaded = true)
public class SrsBatchActionBeanTest extends BaseEventTest {

    private LabVesselDao labVesselDaoMock;
    private LabBatchDao labBatchDaoMock;
    private MercurySampleDao mercurySampleDaoMock;

    private LabBatch workflowBatch;
    private ProductOrder productOrder;

    private Field batchIdField;
    private Field vesselIdField;

    @Override
    @BeforeMethod
    public void setUp() {
        expectedRouting = SystemOfRecord.System.MERCURY;
        super.setUp();

        labVesselDaoMock = mock(LabVesselDao.class);
        labBatchDaoMock = mock(LabBatchDao.class);
        mercurySampleDaoMock = mock(MercurySampleDao.class);

        try {
            batchIdField = LabBatch.class.getDeclaredField("labBatchId");
            batchIdField.setAccessible(true);
            vesselIdField = LabVessel.class.getDeclaredField("labVesselId");
            vesselIdField.setAccessible(true);
        } catch (Exception ex) {
            System.out.println("Failure enabling reflection on labBatchId: " + ex.getMessage());
            ex.printStackTrace();
        }

        productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        workflowBatch = new LabBatch("Exome Express Batch",
                new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        //Build Event History
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, null);

    }

    /**
     * Default handler
     */
    public void testSrsBatchInit() throws Exception {

        // Put a couple dummy batches in place
        List<LabBatch> initialBatches = new ArrayList<>();
        LabBatch emptyBatch01 = new LabBatch("Empty_01", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch02 = new LabBatch("Empty_02", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        batchIdField.set(emptyBatch01, new Long(1L));
        batchIdField.set(emptyBatch02, new Long(2L));
        initialBatches.add(emptyBatch01);
        initialBatches.add(emptyBatch02);

        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        when(labBatchDaoMock.findByType(LabBatch.LabBatchType.SRS)).thenReturn(initialBatches);
        trip.execute();
        SRSBatchActionBean actionBean = trip.getActionBean(SRSBatchActionBean.class);
        trip.getContext().getFilters().get(0).destroy();

        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        Assert.assertTrue("Action bean batchName should be empty", actionBean.getBatchName() == null);
        Assert.assertEquals("Expected 2 batches in action bean", 2, actionBean.getBatchSelectionList().size());

    }

    /**
     * Can't use MockRoundtrip logic, multiple instances fail in same thread
     */
    public void testBatchCreateNoName() throws Exception {

        // Put a couple dummy batches in place
        List<LabBatch> initialBatches = new ArrayList<>();
        LabBatch emptyBatch01 = new LabBatch("Empty_01", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch02 = new LabBatch("Empty_02", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        batchIdField.set(emptyBatch01, new Long(1L));
        batchIdField.set(emptyBatch02, new Long(2L));
        initialBatches.add(emptyBatch01);
        initialBatches.add(emptyBatch02);
        when(labBatchDaoMock.findByType(LabBatch.LabBatchType.SRS)).thenReturn(initialBatches);

        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.execute(SRSBatchActionBean.SAVE_ACTION);
        SRSBatchActionBean actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertTrue("Expected field error on action bean saveNew() with null batchName", actionBean.getValidationErrors().hasFieldErrors());
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        trip.getContext().getFilters().get(0).destroy();

        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        when(labBatchDaoMock.findByName("Empty_01")).thenReturn(emptyBatch01);
        trip.setParameter("batchName", "Empty_01");
        trip.execute(SRSBatchActionBean.SAVE_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertTrue("Expected field error on action bean saveNew() with duplicate batchName", actionBean.getValidationErrors().hasFieldErrors());
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        trip.getContext().getFilters().get(0).destroy();

        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        when(labBatchDaoMock.findByName("Empty_03")).thenReturn(null);
        trip.setParameter("batchName", "Empty_03");
        trip.execute(SRSBatchActionBean.SAVE_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertFalse("No field errors expected on action bean saveNew() with valid batchName", trip.getValidationErrors().hasFieldErrors());
        Assert.assertTrue("Action bean stage EDITING expected", SRSBatchActionBean.Stage.EDITING.equals(actionBean.getStage()));
        Assert.assertEquals("Action bean new batch name should be Empty_03", "Empty_03", actionBean.getLabBatch().getBatchName());
        Assert.assertTrue("Action bean batch vessel list should be empty", actionBean.getLabBatch().getLabBatchStartingVessels().isEmpty());
        Assert.assertEquals("Expected 3 batches in action bean", 2, actionBean.getBatchSelectionList().size());
        Assert.assertEquals("Expected 3 batches in action bean", 2, actionBean.getBatchSelectionList().size());
    }

    public void testToggleSrsBatchStatus() throws Exception {

        // Put a couple dummy batches in place
        List<LabBatch> initialBatches = new ArrayList<>();
        LabBatch emptyBatch01 = new LabBatch("Empty_01", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch02 = new LabBatch("Empty_02", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch03 = new LabBatch("Empty_03", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        batchIdField.set(emptyBatch01, new Long(1L));
        batchIdField.set(emptyBatch02, new Long(2L));
        batchIdField.set(emptyBatch03, new Long(3L));
        initialBatches.add(emptyBatch01);
        initialBatches.add(emptyBatch02);
        initialBatches.add(emptyBatch03);
        when(labBatchDaoMock.findByType(LabBatch.LabBatchType.SRS)).thenReturn(initialBatches);

        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("batchId", null);
        trip.execute(SRSBatchActionBean.TOGGLE_STATUS_ACTION);
        SRSBatchActionBean actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertTrue("Expected error on action bean toggleBatchStatus() with null batch ID", actionBean.getValidationErrors().size() == 1);
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        trip.getContext().getFilters().get(0).destroy();

        when(labBatchDaoMock.findById(LabBatch.class, Long.valueOf(3))).thenReturn(emptyBatch03);
        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("batchId", "3");
        trip.execute(SRSBatchActionBean.TOGGLE_STATUS_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);

        Assert.assertTrue("Expected no errors on action bean toggleBatchStatus()", trip.getValidationErrors().isEmpty());
        Assert.assertTrue("Expected success message on action bean toggleBatchStatus()", actionBean.getStripesMessages().size() == 1);
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        Assert.assertFalse("Expected inactive batch", actionBean.getLabBatch().getActive());
        trip.getContext().getFilters().get(0).destroy();

        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("batchId", "3");
        trip.execute(SRSBatchActionBean.TOGGLE_STATUS_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);

        Assert.assertTrue("Expected no errors on action bean toggleBatchStatus()", actionBean.getValidationErrors().isEmpty());
        Assert.assertTrue("Expected success message on action bean toggleBatchStatus()", actionBean.getStripesMessages().size() == 1);
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        Assert.assertTrue("Expected active batch", actionBean.getLabBatch().getActive());
        trip.getContext().getFilters().get(0).destroy();

    }

    public void testSrsbatchEdit() throws Exception {

        Long batchId2 = new Long(2);
        Long batchId3 = new Long(3);

        // Put a couple dummy batches in place
        List<LabBatch> initialBatches = new ArrayList<>();
        LabBatch emptyBatch01 = new LabBatch("Empty_01", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch02 = new LabBatch("Empty_02", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        LabBatch emptyBatch03 = new LabBatch("Empty_03", Collections.emptySet(), LabBatch.LabBatchType.SRS);
        batchIdField.set(emptyBatch01, new Long(1L));
        batchIdField.set(emptyBatch02, batchId2);
        batchIdField.set(emptyBatch03, batchId3);
        initialBatches.add(emptyBatch01);
        initialBatches.add(emptyBatch02);
        initialBatches.add(emptyBatch03);
        when(labBatchDaoMock.findByType(LabBatch.LabBatchType.SRS)).thenReturn(initialBatches);

        MockRoundtrip trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.execute(SRSBatchActionBean.ADD_SAMPLES_ACTION);
        SRSBatchActionBean actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertTrue("Action bean stage CHOOSING expected", SRSBatchActionBean.Stage.CHOOSING.equals(actionBean.getStage()));
        trip.getContext().getFilters().get(0).destroy();

        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("batchId", batchId2.toString());
        trip.execute(SRSBatchActionBean.ADD_SAMPLES_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);

        Assert.assertFalse("Expected errors on action bean validation when no vessels to add", actionBean.getValidationErrors().isEmpty());
        trip.getContext().getFilters().get(0).destroy();

        int vesselCount = 0;
        StringBuilder barcodes = new StringBuilder();
        StringBuilder samples = new StringBuilder();
        String dupBarcode = null;
        String dupSample = null;
        for (ProductOrderSample pdoSample : productOrder.getSamples()) {
            vesselCount++;
            MercurySample sample = pdoSample.getMercurySample();
            when(mercurySampleDaoMock.findBySampleKey(sample.getSampleKey())).thenReturn(sample);
            samples.append(sample.getSampleKey()).append("\n");
            LabVessel vessel = sample.getLabVessel().iterator().next();
            barcodes.append(vessel.getLabel()).append("\n");
            when(labVesselDaoMock.findByIdentifier(vessel.getLabel())).thenReturn(vessel);
            if (vesselCount == 1) {
                dupBarcode = vessel.getLabel();
                dupSample = sample.getSampleKey();
            }
        }

        when(labBatchDaoMock.findById(LabBatch.class, batchId2)).thenReturn(emptyBatch02);
        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("inputValues", barcodes.toString());
        trip.setParameter("batchId", batchId2.toString());
        trip.execute(SRSBatchActionBean.ADD_BARCODES_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);

        Assert.assertTrue("Expected no errors on action bean add vessels", actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals("Expected " + vesselCount + " batch vessels", actionBean.getLabBatch().getLabBatchStartingVessels().size(), vesselCount);
        trip.getContext().getFilters().get(0).destroy();

        // Test adding a sample which will produce a duplicate vessel
        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("inputValues", dupSample);
        trip.setParameter("batchId", batchId2.toString());
        trip.execute(SRSBatchActionBean.ADD_SAMPLES_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertFalse("Expected errors on action bean add duplicate vessel", actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals("Expected " + vesselCount + " batch vessels", actionBean.getLabBatch().getLabBatchStartingVessels().size(), vesselCount);
        trip.getContext().getFilters().get(0).destroy();

        // Different batch
        when(labBatchDaoMock.findById(LabBatch.class, batchId3)).thenReturn(emptyBatch03);
        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("inputValues", samples.toString());
        trip.setParameter("batchId", batchId3.toString());
        trip.execute(SRSBatchActionBean.ADD_SAMPLES_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);
        Assert.assertTrue("Expected no errors on action bean add vessels", actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals("Expected " + vesselCount + " batch vessels", actionBean.getLabBatch().getLabBatchStartingVessels().size(), vesselCount);
        trip.getContext().getFilters().get(0).destroy();

        // Test adding a vessel which will produce a duplicate sample
        trip = StripesMockTestUtils.createMockRoundtrip(SRSBatchActionBean.class, labVesselDaoMock, labBatchDaoMock, mercurySampleDaoMock);
        trip.setParameter("inputValues", dupBarcode);
        trip.setParameter("batchId", batchId3.toString());
        trip.execute(SRSBatchActionBean.ADD_BARCODES_ACTION);
        actionBean = trip.getActionBean(SRSBatchActionBean.class);

        Assert.assertFalse("Expected errors on action bean add duplicate vessel", actionBean.getValidationErrors().isEmpty());
        Assert.assertEquals("Expected " + vesselCount + " batch vessels", actionBean.getLabBatch().getLabBatchStartingVessels().size(), vesselCount);
        trip.getContext().getFilters().get(0).destroy();

    }
}
package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.easymock.EasyMock.verify;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * DBFree unit test of creation of a flowcell designation ticket ETL
 * Tied to LabBatchStartingVessel
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class FctCreateEtlDbFreeTest {
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private Long entityId = 445566L;
    private Long labBatchId = 112233L;
    private Long designationId = 778899L;
    private String fctName = "FCT-666";
    private LabBatch.LabBatchType fctBatchType = LabBatch.LabBatchType.FCT;
    private LabBatch.LabBatchType miseqBatchType = LabBatch.LabBatchType.MISEQ;
    private String batchVesselLabel = "00012345678";
    private String dilutionVesselLabel = "00087654321";
    private IlluminaFlowcell.FlowcellType flowcellType = IlluminaFlowcell.FlowcellType.HiSeq2500Flowcell;
    private VesselPosition flowcellLane = VesselPosition.LANE1;
    private BigDecimal concentration = new BigDecimal("20.01");
    // Wed, 01 Jun 2016 08:00:00 EDT
    private Date createdDate = new Date(1464782400000L);
    private FctCreateEtl tst;
    private List<FlowcellDesignation> designations = new ArrayList<>();

    private LabBatchDao dao = createMock(LabBatchDao.class);
    private LabBatchStartingVessel labBatchStartingVessel = createMock(LabBatchStartingVessel.class);
    private LabBatch labBatch = createMock(LabBatch.class);
    private LabVessel batchVessel = createMock(LabVessel.class);

    private FlowcellDesignationEjb flowcellDesignationEjb = createMock(FlowcellDesignationEjb.class);

    private Object[] mocks = new Object[]{dao, labBatchStartingVessel, labBatch, batchVessel, flowcellDesignationEjb};

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void beforeMethod() {
        reset(mocks);
        tst = new FctCreateEtl(dao, flowcellDesignationEjb);
    }

    public void testEtlFlags() throws Exception {
        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabBatchStartingVessel.class);
        assertEquals(tst.baseFilename, "fct_create");
        assertEquals(tst.entityId(labBatchStartingVessel), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testNonFlowcellTicketBatch() throws Exception {
        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getLabBatchType()).andReturn(LabBatch.LabBatchType.WORKFLOW);

        replay(mocks);

        String dataRecord = tst.dataRecord(etlDateString, false, labBatchStartingVessel);

        assertNull(dataRecord, "Non-FCT batch should not produce ETL data");

        verify(mocks);
    }

    public void testNonPoolTestFromDesignation() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getLabBatchType()).andReturn(fctBatchType).anyTimes();
        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        // LabBatchStartingVessel will have designation and dilution vessel assigned at FlowcellTransfer
        expect(labBatchStartingVessel.getDilutionVessel()).andReturn(new BarcodedTube(dilutionVesselLabel));

        FlowcellDesignation flowcellDesignation = buildFlowcellDesignation(false);
        designations.clear();
        designations.add(flowcellDesignation);
        expect(labBatchStartingVessel.getFlowcellDesignation()).andReturn( flowcellDesignation ).times(2);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), true, "N");

        verify(mocks);
    }

    public void testPoolTestFromDesignation() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getLabBatchType()).andReturn(fctBatchType).anyTimes();
        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        // LabBatchStartingVessel will have designation and dilution vessel assigned at FlowcellTransfer
        expect(labBatchStartingVessel.getDilutionVessel()).andReturn(new BarcodedTube(dilutionVesselLabel));

        FlowcellDesignation flowcellDesignation = buildFlowcellDesignation(true);
        designations.clear();
        designations.add(flowcellDesignation);

        expect(labBatchStartingVessel.getFlowcellDesignation()).andReturn( flowcellDesignation ).times(2);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), true, "Y");

        verify(mocks);
    }

    /**
     * Tests initial creation of a LabBatchStartingVessel without any flowcell designation assigned
     * @throws Exception On any error
     */
    public void testPoolTestFromHiSeq() throws Exception {

        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);
        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getLabBatchType()).andReturn(fctBatchType).anyTimes();
        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        // Initial create LabBatchStartingVessel will be null, subsequent FlowcellTransfer will assign
        expect(labBatchStartingVessel.getDilutionVessel()).andReturn(null);
        designations.clear();

        expect(labBatchStartingVessel.getFlowcellDesignation()).andReturn( null );
        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), false, "N");

        verify(mocks);
    }

    /**
     * Tests a MiSeq flowcell ticket LabBatchStartingVessel without any flowcell designation assigned
     * @throws Exception On any error
     */
    public void testPoolTestFromMiseq() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getLabBatchType()).andReturn(miseqBatchType).anyTimes();
        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        // MiSeq will always be null
        expect(labBatchStartingVessel.getDilutionVessel()).andReturn(null);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), false, "Y");

        verify(mocks);
    }

    private void verifyRecord(String record, boolean hasDesignation, String testFlag) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], entityId.toString() );
        if( hasDesignation ) {
            assertEquals(parts[i++], designationId.toString());
        } else {
            assertEquals(parts[i++], "" );
        }
        assertEquals(parts[i++], labBatchId.toString() );
        assertEquals(parts[i++], fctName);
        assertTrue(parts[i++].matches("FCT|MISEQ"), "Flowcell ticket batch type must be either 'FCT' or 'MISEQ'");
        assertEquals(parts[i++], batchVesselLabel);
        if( hasDesignation ) {
            assertEquals(parts[i++], dilutionVesselLabel);
        } else {
            assertEquals(parts[i++], "" );
        }
        assertEquals(parts[i++], EtlTestUtilities.format(createdDate));
        assertEquals(parts[i++], flowcellType.getDisplayName());
        assertEquals(parts[i++], flowcellLane.toString().replace("LANE",""));
        assertEquals(parts[i++], concentration.toString());
        assertEquals(parts[i++], testFlag);
        assertEquals(parts.length, i);
    }

    private FlowcellDesignation buildFlowcellDesignation( boolean isPoolTest )
            throws NoSuchFieldException, IllegalAccessException {

        FlowcellDesignation flowcellDesignation = new FlowcellDesignation(batchVessel, labBatch,
                FlowcellDesignation.IndexType.DUAL, isPoolTest /*poolTest*/,
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell,  8, 76, BigDecimal.TEN, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL);

        // Need an ID to spare us from creating a mock
        Field idField = FlowcellDesignation.class.getDeclaredField("designationId" );
        idField.setAccessible(true);
        idField.set(flowcellDesignation, designationId);

        return flowcellDesignation;
    }
}


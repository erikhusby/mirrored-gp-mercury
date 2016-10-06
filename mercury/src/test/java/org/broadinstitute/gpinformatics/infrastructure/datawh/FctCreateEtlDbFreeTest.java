package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FlowcellDesignationEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.FlowcellDesignation;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.easymock.EasyMock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

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
    private long entityId = 445566L;
    private long labBatchId = 112233L;
    private String fctName = "FCT-666";
    private LabBatch.LabBatchType fctBatchType = LabBatch.LabBatchType.FCT;
    private LabBatch.LabBatchType miseqBatchType = LabBatch.LabBatchType.MISEQ;
    private String batchVesselLabel = "00012345678";
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
    private LabEvent labEvent =  new LabEvent(LabEventType.DENATURE_TRANSFER, new Date(), "none", 1L, 2L, "program");
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

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getLabBatchType()).andReturn(miseqBatchType).anyTimes();
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);

        expect(flowcellDesignationEjb.getFlowcellDesignations(EasyMock.anyObject(Collection.class))).
                andReturn(designations);

        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);

        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        designations.clear();
        designations.add(new FlowcellDesignation(labBatchStartingVessel.getLabVessel(), labBatch,
                labEvent, FlowcellDesignation.IndexType.DUAL, false /*poolTest*/,
                IlluminaFlowcell.FlowcellType.MiSeqFlowcell, 1, 76, BigDecimal.TEN, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL));

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), batchVesselLabel, "N");

        verify(mocks);
    }

    public void testPoolTestFromDesignation() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getLabBatchType()).andReturn(fctBatchType).anyTimes();
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);

        expect(flowcellDesignationEjb.getFlowcellDesignations(EasyMock.anyObject(Collection.class))).
                andReturn(designations);

        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);

        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        designations.clear();
        designations.add(new FlowcellDesignation(labBatchStartingVessel.getLabVessel(), labBatch,
                labEvent, FlowcellDesignation.IndexType.DUAL, true /*poolTest*/,
                IlluminaFlowcell.FlowcellType.HiSeqFlowcell,  8, 76, BigDecimal.TEN, true,
                FlowcellDesignation.Status.IN_FCT, FlowcellDesignation.Priority.NORMAL));

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), batchVesselLabel, "Y");

        verify(mocks);
    }

    public void testPoolTestFromHiSeq() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getLabBatchType()).andReturn(fctBatchType).anyTimes();
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);

        expect(flowcellDesignationEjb.getFlowcellDesignations(EasyMock.anyObject(Collection.class))).
                andReturn(designations);

        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);

        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        designations.clear();

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), batchVesselLabel, "N");

        verify(mocks);
    }

    public void testPoolTestFromMiseq() throws Exception {
        expect(dao.findById(LabBatchStartingVessel.class, entityId)).andReturn(labBatchStartingVessel);

        expect(labBatchStartingVessel.getBatchStartingVesselId()).andReturn(entityId);

        expect(labBatchStartingVessel.getLabBatch()).andReturn(labBatch);
        expect(labBatch.getBatchName()).andReturn(fctName);
        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(labBatch.getLabBatchType()).andReturn(miseqBatchType).anyTimes();
        expect(labBatch.getFlowcellType()).andReturn(flowcellType).times(2);
        expect(labBatch.getCreatedOn()).andReturn(createdDate);

        expect(flowcellDesignationEjb.getFlowcellDesignations(EasyMock.anyObject(Collection.class))).
                andReturn(designations);

        expect(labBatchStartingVessel.getLabVessel()).andReturn(batchVessel).anyTimes();
        expect(batchVessel.getLabel()).andReturn(batchVesselLabel);

        expect(labBatchStartingVessel.getVesselPosition()).andReturn(flowcellLane).times(2);
        expect(labBatchStartingVessel.getConcentration()).andReturn(concentration);

        replay(mocks);

        designations.clear();

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        assertEquals(records.size(), 1);

        verifyRecord(records.iterator().next(), batchVesselLabel, "Y");

        verify(mocks);
    }

    private void verifyRecord(String record, String libraryLabel, String testFlag) {
        String[] parts = record.split(",");
        int i = 0;
        assertEquals(parts[i++], etlDateString);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(labBatchId));
        assertEquals(parts[i++], fctName);
        assertTrue(parts[i++].matches("FCT|MISEQ"), "Flowcell ticket batch type must be either 'FCT' or 'MISEQ'");
        assertEquals(parts[i++], libraryLabel);
        assertEquals(parts[i++], EtlTestUtilities.format(createdDate));
        assertEquals(parts[i++], flowcellType.getDisplayName());
        assertEquals(parts[i++], flowcellLane.toString().replace("LANE",""));
        assertEquals(parts[i++], concentration.toString());
        assertEquals(parts[i++], testFlag);
        assertEquals(parts.length, i);
    }
}


package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.*;

/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class EventEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private final long entityId = 1122334455L;
    private final long workflowId = -1234123412341234123L;
    private final long processId = 3412341234123412312L;
    private final String pdoKey = "PDO-0000";
    private final long pdoId = 3344551122L;
    private final String sampleKey = "SMID-000000";
    private final long labBatchId = 4455112233L;
    private final String location = "Machine-XYZ";
    private final long vesselId = 5511223344L;
    private final Date eventDate = new Date(1350000000000L);
    private final LabEventType eventType = LabEventType.PICO_PLATING_BUCKET;
    private LabEventEtl tst;

    private final AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private final LabEventDao dao = createMock(LabEventDao.class);
    private final ProductOrderDao pdoDao = createMock(ProductOrderDao.class);
    private final WorkflowConfigLookup wfLookup = createMock(WorkflowConfigLookup.class);
    private final WorkflowConfigDenorm wfConfig = createMock(WorkflowConfigDenorm.class);
    private final LabEvent obj = createMock(LabEvent.class);
    private final ProductOrder pdo = createMock(ProductOrder.class);
    private final LabVessel vessel = createMock(LabVessel.class);
    private final SampleInstance sampleInst = createMock(SampleInstance.class);
    private final MercurySample sample = createMock(MercurySample.class);
    private final LabBatch labBatch = createMock(LabBatch.class);

    private final Object[] mocks = new Object[]{auditReader, dao, pdoDao, wfLookup, wfConfig, obj, pdo, vessel, sampleInst, sample, labBatch};

    private final Set<LabVessel> vesselList = new HashSet<LabVessel>();
    private final Set<SampleInstance> sampleInstList = new HashSet<SampleInstance>();
    private final Set<LabBatch> workflowLabBatches = new HashSet<LabBatch>();

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);

        vesselList.clear();
        vesselList.add(vessel);
        sampleInstList.clear();
        sampleInstList.add(sampleInst);
        workflowLabBatches.clear();
        workflowLabBatches.add(labBatch);

        tst = new LabEventEtl(wfLookup, dao, pdoDao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        expect(obj.getLabEventId()).andReturn(entityId);
        replay(mocks);

        assertEquals(tst.entityClass, LabEvent.class);
        assertEquals(tst.baseFilename, "event_fact");
        assertEquals(tst.entityId(obj), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecordNoEntity() throws Exception {
        expect(dao.findById(LabEvent.class, -1L)).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testCantMakeEtlRecordNoEventType() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);

        verify(mocks);
    }

    public void testCantMakeEtlRecordNoVessels() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(eventType).times(2);
        expect(obj.getTargetLabVessels()).andReturn(vesselList);
        expect(vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).
                andReturn(new HashSet<SampleInstance>());
        expect(vessel.getLabel()).andReturn("");
        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);

        verify(mocks);
    }

    public void testEtlNoSampleInstances() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(eventType).times(2);
        expect(obj.getTargetLabVessels()).andReturn(vesselList);
        expect(vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).
                andReturn(new HashSet<SampleInstance>());
        String vesselLabel = "03138970";
        expect(vessel.getLabel()).andReturn(vesselLabel);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 0);

        verify(mocks);
    }

    public void testMissingSampleRecord() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(eventType).times(2);
        expect(obj.getTargetLabVessels()).andReturn(vesselList);
        expect(vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstList);
        expect(sampleInst.getProductOrderKey()).andReturn(pdoKey);
        expect(sampleInst.getStartingSample()).andReturn(null);

        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(eventType).times(2);
        expect(obj.getTargetLabVessels()).andReturn(vesselList);
        expect(vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstList);
        expect(sampleInst.getStartingSample()).andReturn(sample);
        expect(sampleInst.getAllWorkflowLabBatches()).andReturn(workflowLabBatches);
        expect(sampleInst.getProductOrderKey()).andReturn(pdoKey).times(2);

        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(sample.getSampleKey()).andReturn(sampleKey);
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getEventDate()).andReturn(eventDate);
        expect(wfLookup.lookupWorkflowConfig(eventType.getName(), pdo, eventDate)).andReturn(wfConfig);
        expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        expect(wfConfig.getProcessId()).andReturn(processId);
        expect(obj.getLabEventId()).andReturn(entityId);
        expect(obj.getEventLocation()).andReturn(location);
        expect(vessel.getLabVesselId()).andReturn(vesselId);
        expect(obj.getEventDate()).andReturn(eventDate);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    public void testInPlaceLabVessel() throws Exception {
        expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        expect(obj.getLabEventType()).andReturn(eventType).times(2);
        expect(obj.getTargetLabVessels()).andReturn(new HashSet<LabVessel>());
        expect(obj.getInPlaceLabVessel()).andReturn(vessel).times(2);
        expect(vessel.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstList);
        expect(sampleInst.getStartingSample()).andReturn(sample);
        expect(sampleInst.getAllWorkflowLabBatches()).andReturn(workflowLabBatches);
        expect(sampleInst.getProductOrderKey()).andReturn(pdoKey).times(2);

        expect(labBatch.getLabBatchId()).andReturn(labBatchId);
        expect(sample.getSampleKey()).andReturn(sampleKey);
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(obj.getEventDate()).andReturn(eventDate);
        expect(wfLookup.lookupWorkflowConfig(eventType.getName(), pdo, eventDate)).andReturn(wfConfig);
        expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        expect(wfConfig.getProcessId()).andReturn(processId);
        expect(obj.getLabEventId()).andReturn(entityId);
        expect(obj.getEventLocation()).andReturn(location);
        expect(vessel.getLabVesselId()).andReturn(vesselId);
        expect(obj.getEventDate()).andReturn(eventDate);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        verify(mocks);
    }

    private void verifyRecord(String record) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], String.valueOf(workflowId));
        assertEquals(parts[i++], String.valueOf(processId));
        assertEquals(parts[i++], String.valueOf(pdoId));
        assertEquals(parts[i++], sampleKey);
        assertEquals(parts[i++], String.valueOf(labBatchId));
        assertEquals(parts[i++], location);
        assertEquals(parts[i++], String.valueOf(vesselId));
        assertEquals(parts[i++], ExtractTransform.secTimestampFormat.format(eventDate));
        assertEquals(parts.length, i);
    }
}


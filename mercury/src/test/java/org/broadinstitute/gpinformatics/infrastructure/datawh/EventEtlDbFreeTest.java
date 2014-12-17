package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactoryTest;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


/**
 * dbfree unit test of entity etl.
 *
 * @author epolk
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class EventEtlDbFreeTest {
    private final String etlDateStr = ExtractTransform.formatTimestamp(new Date());
    private final long entityId = 1122334455L;
    private final long workflowId = -1234123412341234123L;
    private final long processId = 3412341234123412312L;
    private final long pdoId = 3344551122L;
    private final String sampleKey = "SMID-000000";
    private final String labBatchName = "LCSET-123";
    private final String location = "Machine-XYZ";
    private final String programName = "FlowcellLoader";
    private final long vesselId = 5511223344L;
    private final Date eventDate = new Date(1350000000000L);
    private final String workflowName = Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName();
    private LabEventEtl tst;

    private final AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private final LabEventDao dao = EasyMock.createMock(LabEventDao.class);
    private final WorkflowConfigLookup wfLookup = EasyMock.createMock(WorkflowConfigLookup.class);
    private final WorkflowConfigDenorm wfConfig = EasyMock.createMock(WorkflowConfigDenorm.class);
    private final LabEvent obj = EasyMock.createMock(LabEvent.class);
    private final Product product = EasyMock.createMock(Product.class);
    private final ProductOrder pdo = EasyMock.createMock(ProductOrder.class);
    private final ProductOrderSample pdoSample = EasyMock.createMock(ProductOrderSample.class);
    private final LabVessel vessel = EasyMock.createMock(LabVessel.class);
    private final SampleInstanceV2 sampleInst = EasyMock.createMock(SampleInstanceV2.class);
    private final MercurySample sample = EasyMock.createMock(MercurySample.class);
    private final LabBatch labBatch = EasyMock.createMock(LabBatch.class);
    private final SequencingSampleFactEtl sequencingSampleFactEtl = EasyMock.createNiceMock(
            SequencingSampleFactEtl.class);
    private final LabEvent modEvent = EasyMock.createNiceMock(LabEvent.class);
    private final LabVessel denature = EasyMock.createNiceMock(LabVessel.class);
    private final RunCartridge cartridge = EasyMock.createNiceMock(RunCartridge.class);
    private final LabEvent cartridgeEvent = EasyMock.createNiceMock(LabEvent.class);
    private final IlluminaFlowcell flowcell = EasyMock.createNiceMock(IlluminaFlowcell.class);
    private final BucketEntry bucketEntry = EasyMock.createNiceMock(BucketEntry.class);

    private final Object[] mocks = new Object[]{auditReader, dao, wfLookup, wfConfig, obj, product, pdo,
            pdoSample, vessel, sampleInst, sample, labBatch, sequencingSampleFactEtl, modEvent, denature, cartridge,
            cartridgeEvent, flowcell, bucketEntry};

    private final Set<LabVessel> vesselList = new HashSet<>();
    private final Set<SampleInstanceV2> sampleInstList = new HashSet<>();

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        EasyMock.reset(mocks);

        vesselList.clear();
        vesselList.add(vessel);
        sampleInstList.clear();
        sampleInstList.add(sampleInst);

        tst = new LabEventEtl(wfLookup, dao, sequencingSampleFactEtl);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.entityClass, LabEvent.class);
        Assert.assertEquals(tst.baseFilename, "event_fact");
        Assert.assertEquals(tst.entityId(obj), (Long) entityId);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecordNoEntity() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, -1L)).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecordNoEventType() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(null);

        EasyMock.replay(mocks);

        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testNoVessels() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).times(2);
        vesselList.clear();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getInPlaceLabVessel()).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.dataRecords(etlDateStr, false, entityId).size(), 0);

        EasyMock.verify(mocks);
    }

    public void testEtlNoSampleInstances() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(new HashSet<SampleInstanceV2>());

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 0);

        EasyMock.verify(mocks);
    }

    public void testMissingSampleRecord() throws Exception {
        // ETL uses index name instead of a null sample name in its debug UI.
        Set<MolecularIndexReagent> reagents = new HashSet<>(ZimsIlluminaRunFactoryTest.makeTestReagents(1, false));
        String misName = reagents.iterator().next().getMolecularIndexingScheme().getName();

        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(null);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(null);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(null);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(vessel.getIndexes(sampleInst)).andReturn(reagents);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.A_BASE.getName(), null, eventDate)).andReturn(null);
        EasyMock.replay(mocks);

        // Tests the output seen by debug UI.
        List<LabEventEtl.EventFactDto> dtos = tst.makeEventFacts(entityId);
        Assert.assertEquals(dtos.size(), 1);
        Assert.assertFalse(dtos.get(0).canEtl());
        Assert.assertTrue(dtos.get(0).getSampleInstanceIndexes().startsWith(misName));

        EasyMock.verify(mocks);
    }
    public void testPicoPlatingNoBatch() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(null);
        EasyMock.expect(pdo.getProduct()).andReturn(product);
        EasyMock.expect(product.getWorkflow()).andReturn(Workflow.AGILENT_EXOME_EXPRESS);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(), workflowName,
                eventDate)).andReturn(wfConfig);
        EasyMock.expect(wfConfig.isBatchNeeded()).andReturn(false);
        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), LabEventEtl.NONE);

        EasyMock.verify(mocks);
    }

    public void testPicoPlating() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);
        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(), workflowName,
                eventDate)).andReturn(wfConfig);
        EasyMock.expect(labBatch.getLabBatchType()).andReturn(LabBatchType.WORKFLOW);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    public void testSampleImportNoProduct() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.SAMPLE_IMPORT).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(null);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(null);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.SAMPLE_IMPORT.getName(), null, eventDate))
                .andReturn(wfConfig);
        EasyMock.expect(wfConfig.isBatchNeeded()).andReturn(false);
        EasyMock.expect(wfConfig.isProductOrderNeeded()).andReturn(false);
        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
    }

    public void testSampleImport() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.SAMPLE_IMPORT).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(pdo.getProduct()).andReturn(product);
        EasyMock.expect(product.getWorkflow()).andReturn(Workflow.AGILENT_EXOME_EXPRESS);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);
        EasyMock.expect(labBatch.getBatchName()).andReturn(null);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(null);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.SAMPLE_IMPORT.getName(), workflowName, eventDate))
                .andReturn(wfConfig);
        EasyMock.expect(wfConfig.isBatchNeeded()).andReturn(false);
        EasyMock.expect(labBatch.getLabBatchType()).andReturn(LabBatchType.SAMPLES_IMPORT);
        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
    }

    public void testPicoPlatingBucket() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(null);
        EasyMock.expect(pdo.getProduct()).andReturn(product);
        EasyMock.expect(product.getWorkflow()).andReturn(Workflow.AGILENT_EXOME_EXPRESS);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);
        EasyMock.expect(wfConfig.isBatchNeeded()).andReturn(false);

        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();

        EasyMock.replay(mocks);

        List<LabEventEtl.EventFactDto> dtos = tst.makeEventFacts(entityId);
        Assert.assertEquals(dtos.size(), 1);
        Assert.assertTrue(dtos.get(0).canEtl());
        Assert.assertEquals(dtos.get(0).getBatchName(), LabEventEtl.NONE);
        Assert.assertEquals(dtos.get(0).getSample(), sample);

        EasyMock.verify(mocks);
    }

    public void testInPlaceLabVessel() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).times(2);
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(new HashSet<LabVessel>());
        EasyMock.expect(obj.getInPlaceLabVessel()).andReturn(vessel).times(2);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);
        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(sampleInst.getRootOrEarliestMercurySample()).andReturn(sample);
        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(), workflowName,
                eventDate)).andReturn(wfConfig);
        EasyMock.expect(labBatch.getLabBatchType()).andReturn(LabBatchType.WORKFLOW);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next());

        EasyMock.verify(mocks);
    }

    public void testNoFixups() {
        Collection<Long> deletedEntityIds = new ArrayList<>();
        Collection<Long> modifiedEntityIds = new ArrayList<>();
        Collection<Long> addedEntityIds = new ArrayList<>();
        String etlDateStr = "20130623182000";

        tst.processFixups(deletedEntityIds, modifiedEntityIds, etlDateStr);
    }

    @Test(groups = TestGroups.DATABASE_FREE, enabled = true)
    public void testFixups() {
        Long modEventId = 9L;
        Long seqRunId = 8L;
        Long cartridgeEventId = 7L;
        String etlDateStr = "20130623182000";

        Set<Long> deletedEntityIds = new HashSet<>();
        Set<Long> modifiedEntityIds = new HashSet<>();

        modifiedEntityIds.add(modEventId);

        // modEvent is the modified event and it has one vessel, denature.
        // Denature has one descendant vessel, cartridge, which has one event, cartridgeEvent.
        //
        // modEvent should cause cartridgeEvent to be put on the modifiedIds list.

        EasyMock.expect(dao.findById(LabEvent.class, modEventId)).andReturn(modEvent);
        EasyMock.expect(modEvent.getLabEventId()).andReturn(modEventId);
        EasyMock.expect(modEvent.getTargetLabVessels()).andReturn(Collections.<LabVessel>emptySet());
        EasyMock.expect(modEvent.getInPlaceLabVessel()).andReturn(denature);

        Collection<LabVessel> cartridges = new ArrayList<>();
        cartridges.add(cartridge);
        EasyMock.expect(denature.getDescendantVessels()).andReturn(cartridges);

        Set<LabEvent> denatureEvents = new HashSet<>();
        denatureEvents.add(modEvent);
        EasyMock.expect(denature.getEvents()).andReturn(denatureEvents);

        Set<LabEvent> cartridgeEvents = new HashSet<>();
        cartridgeEvents.add(cartridgeEvent);
        EasyMock.expect(cartridge.getEvents()).andReturn(cartridgeEvents);
        EasyMock.expect(cartridgeEvent.getLabEventId()).andReturn(cartridgeEventId);

        EasyMock.expect(denature.getType()).andReturn(LabVessel.ContainerType.TUBE);
        EasyMock.expect(cartridge.getType()).andReturn(LabVessel.ContainerType.FLOWCELL);

        SequencingRun seqRun = new IlluminaSequencingRun(
                flowcell, "runName", "runBarcode", "machine", 1234L, false, new Date(), "/tmp");
        seqRun.setSequencingRunId(seqRunId);
        Set<SequencingRun> seqRuns = new HashSet<>();
        seqRuns.add(seqRun);

        EasyMock.expect(cartridge.getSequencingRuns()).andReturn(seqRuns);

        EasyMock.replay(mocks);

        tst.processFixups(deletedEntityIds, modifiedEntityIds, etlDateStr);

        Assert.assertEquals(deletedEntityIds.size(), 0);
        Assert.assertEquals(modifiedEntityIds.size(), 2);

        // (Does not need to verify mocks.)
    }


    private void verifyRecord(String record) {
        verifyRecord(record, labBatchName);
    }

    private void verifyRecord(String record, String expectedLabBatchName) {
        EtlTestUtilities.verifyRecord(record, etlDateStr,"F", String.valueOf(entityId), String.valueOf(workflowId),
                String.valueOf(processId), String.valueOf(pdoId), sampleKey, String.valueOf(expectedLabBatchName),
                location, String.valueOf(vesselId), ExtractTransform.formatTimestamp(eventDate), programName);
    }
}


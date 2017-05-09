package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
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
    private final String lcsetSampleKey = "SMID-LCSET00";
    private final String labBatchName = "LCSET-123";
    private final String pdoName = "PDO-123";
    private final String location = "Machine-XYZ";
    private final String programName = "FlowcellLoader";
    private final long vesselId = 5511223344L;
    private final Date eventDate = new Date(1413676800000L);
    private final String workflowName = Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName();
    private final String indexingSchemeName = "Illumina_P5-Bilbo_P7-Frodo";
    private final MolecularIndexingScheme indexingScheme = new MolecularIndexingScheme();

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
    private final BucketEntry bucketEntry = EasyMock.createMock(BucketEntry.class);

    private final SequencingSampleFactEtl sequencingSampleFactEtl = EasyMock.createNiceMock(
            SequencingSampleFactEtl.class);
    private final LabEvent modEvent = EasyMock.createNiceMock(LabEvent.class);
    private final LabVessel denature = EasyMock.createNiceMock(LabVessel.class);
    private final RunCartridge cartridge = EasyMock.createNiceMock(RunCartridge.class);
    private final LabEvent cartridgeEvent = EasyMock.createNiceMock(LabEvent.class);
    private final IlluminaFlowcell flowcell = EasyMock.createNiceMock(IlluminaFlowcell.class);

    private final Object[] mocks = new Object[]{auditReader, dao, wfLookup, wfConfig, obj, product, pdo,
            pdoSample, vessel, sampleInst, sample, labBatch, sequencingSampleFactEtl, modEvent, denature, cartridge,
            cartridgeEvent, flowcell, bucketEntry};

    private final Set<LabVessel> vesselList = new HashSet<>();
    private final Set<SampleInstanceV2> sampleInstList = new HashSet<>();
    private final List<ProductOrderSample> pdoSamples = new ArrayList<>();

    private final List<LabBatch> wfLabBatches = new ArrayList<>();
    private final List<BucketEntry> bucketEntries = new ArrayList<>();
    private final Set<MercurySample> mercurySamples = new HashSet<>();

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        EasyMock.reset(mocks);

        vesselList.clear();
        vesselList.add(vessel);
        sampleInstList.clear();
        sampleInstList.add(sampleInst);
        pdoSamples.clear();
        pdoSamples.add(pdoSample);
        wfLabBatches.clear();
        wfLabBatches.add(labBatch);
        indexingScheme.setName(indexingSchemeName);

        bucketEntries.clear();
        bucketEntries.add(bucketEntry);

        mercurySamples.clear();
        mercurySamples.add(sample);

        tst = new LabEventEtl(wfLookup, dao, sequencingSampleFactEtl, new WorkflowLoader().load());
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
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(new HashSet<LabVessel>());
        EasyMock.expect(obj.getInPlaceLabVessel()).andReturn(null);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.A_BASE.getName(),
                null, eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("ABase");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.A_BASE.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        String record = records.iterator().next();
        String expected = etlDateStr + ",F,"
                          + String.valueOf(entityId) + ","
                          + workflowId + "," + processId + ","
                          + LabEventType.A_BASE.getName() + ",,,,NONE,"
                          + location + ",,," + ExtractTransform.formatTimestamp(eventDate)
                          + "," + programName + ",,,E";

        Assert.assertEquals(record, expected, "Record for no-vessel event is not as expected" );

        EasyMock.verify(mocks);
    }

    public void testEtlNoSampleInstances() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).times(2);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(new HashSet<SampleInstanceV2>());
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).times(2);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 0);

        EasyMock.verify(mocks);
    }

    public void testMissingSampleRecord() throws Exception {

        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.A_BASE).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId).anyTimes();
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(null);
        EasyMock.expect(sampleInst.getSingleBatch()).andReturn(labBatch);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(null);
        EasyMock.expect(sampleInst.getAllProductOrderSamples()).andReturn(new ArrayList<ProductOrderSample>());
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.A_BASE.getName(),
                workflowName, eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("ABase");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.A_BASE.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        // Tests the output seen by debug UI.
        List<LabEventEtl.EventFactDto> dtos = tst.makeEventFacts(entityId);
        Assert.assertEquals(dtos.size(), 1);
        // Missing LCSET sample no longer discards row
        Assert.assertTrue(dtos.get(0).canEtl());

        EasyMock.verify(mocks);
    }

    public void testPicoPlatingNoBatch() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();

        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(null);
        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(null);
        EasyMock.expect(sampleInst.getSingleBatch()).andReturn(null);
        EasyMock.expect(sampleInst.getAllProductOrderSamples()).andReturn(pdoSamples);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.getAllBucketEntries()).andReturn(new ArrayList<BucketEntry>());
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).times(2);
        EasyMock.expect(pdoSample.getProductOrder()).andReturn(pdo).times(2);

        EasyMock.expect(pdo.getProduct()).andReturn(product);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getBusinessKey()).andReturn(pdoName);
        EasyMock.expect(pdo.getCreatedDate()).andReturn(eventDate);

        EasyMock.expect(product.getWorkflow()).andReturn(Workflow.AGILENT_EXOME_EXPRESS);

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);

        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), LabEventType.PICO_PLATING_BUCKET.getName(), LabEventEtl.NONE);
    }

    public void testPicoPlating() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();

        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();
        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);
        EasyMock.expect(sampleInst.getProductOrderSampleForSingleBucket()).andReturn(pdoSample);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(lcsetSampleKey);

        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).anyTimes();

        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getBusinessKey()).andReturn(pdoName);

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate).anyTimes();

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(), workflowName,
                eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(),LabEventType.PICO_PLATING_BUCKET.getName() );

        EasyMock.verify(mocks);
    }

    public void testSampleImportNoProduct() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.SAMPLE_IMPORT).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId).anyTimes();
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);
        EasyMock.expect(sampleInst.getProductOrderSampleForSingleBucket()).andReturn(pdoSample);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(lcsetSampleKey);

        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(null);

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).anyTimes();
        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate).anyTimes();

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.SAMPLE_IMPORT.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
    }

    public void testSampleImport() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.SAMPLE_IMPORT).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).anyTimes();
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId).anyTimes();
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);
        EasyMock.expect(sampleInst.getProductOrderSampleForSingleBucket()).andReturn(pdoSample);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(lcsetSampleKey);

        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).anyTimes();

        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getBusinessKey()).andReturn(pdoName);

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate).anyTimes();

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.SAMPLE_IMPORT.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 1);
    }

    public void testPicoPlatingBucket() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventId()).andReturn(entityId).anyTimes();
        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(vesselList);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId)).anyTimes();

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);
        EasyMock.expect(sampleInst.getProductOrderSampleForSingleBucket()).andReturn(pdoSample);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(lcsetSampleKey);

        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);;

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate).anyTimes();

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).anyTimes();

        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getBusinessKey()).andReturn(pdoName);

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        List<LabEventEtl.EventFactDto> dtos = tst.makeEventFacts(entityId);

        EasyMock.verify(mocks);

        Assert.assertEquals(dtos.size(), 1);
        Assert.assertTrue(dtos.get(0).canEtl());
        Assert.assertEquals(dtos.get(0).getBatchName(), labBatchName);
        Assert.assertEquals(dtos.get(0).getPdoSampleId(), sampleKey);
    }

    public void testInPlaceLabVessel() throws Exception {
        EasyMock.expect(dao.findById(LabEvent.class, entityId)).andReturn(obj);

        EasyMock.expect(obj.getLabEventType()).andReturn(LabEventType.PICO_PLATING_BUCKET).anyTimes();
        EasyMock.expect(obj.getTargetLabVessels()).andReturn(new HashSet<LabVessel>());
        EasyMock.expect(obj.getInPlaceLabVessel()).andReturn(vessel).times(2);
        EasyMock.expect(obj.getEventDate()).andReturn(eventDate).times(2);
        EasyMock.expect(obj.getLabEventId()).andReturn(entityId);
        EasyMock.expect(obj.getEventLocation()).andReturn(location);
        EasyMock.expect(obj.getProgramName()).andReturn(programName);

        EasyMock.expect(vessel.getLabVesselId()).andReturn(vesselId);
        EasyMock.expect(vessel.getLabel()).andReturn(String.valueOf(vesselId));
        EasyMock.expect(vessel.getContainerRole()).andReturn(null);
        EasyMock.expect(vessel.getSampleInstancesV2()).andReturn(sampleInstList);

        EasyMock.expect(sampleInst.getSingleBucketEntry()).andReturn(bucketEntry);
        EasyMock.expect(sampleInst.getMolecularIndexingScheme()).andReturn(indexingScheme).times(2);
        EasyMock.expect(sampleInst.isReagentOnly()).andReturn(false);
        EasyMock.expect(sampleInst.getProductOrderSampleForSingleBucket()).andReturn(pdoSample);
        EasyMock.expect(sampleInst.getNearestMercurySampleName()).andReturn(lcsetSampleKey);

        EasyMock.expect(bucketEntry.getLabBatch()).andReturn(labBatch);
        EasyMock.expect(bucketEntry.getProductOrder()).andReturn(pdo);

        EasyMock.expect(sample.getSampleKey()).andReturn(sampleKey);

        EasyMock.expect(pdoSample.getMercurySample()).andReturn(sample).anyTimes();

        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getBusinessKey()).andReturn(pdoName);

        EasyMock.expect(labBatch.getBatchName()).andReturn(labBatchName);
        EasyMock.expect(labBatch.getWorkflowName()).andReturn(workflowName);
        EasyMock.expect(labBatch.getCreatedOn()).andReturn(eventDate).anyTimes();

        EasyMock.expect(wfLookup.lookupWorkflowConfig(LabEventType.PICO_PLATING_BUCKET.getName(),
                Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), eventDate)).andReturn(wfConfig);

        EasyMock.expect(wfConfig.getWorkflowId()).andReturn(workflowId);
        EasyMock.expect(wfConfig.getProductWorkflowName()).andReturn(workflowName);
        EasyMock.expect(wfConfig.getWorkflowProcessName()).andReturn("Pico/Plating");
        EasyMock.expect(wfConfig.getWorkflowStepName()).andReturn(LabEventType.PICO_PLATING_BUCKET.toString());
        EasyMock.expect(wfConfig.getProcessId()).andReturn(processId);

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        Assert.assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), LabEventType.PICO_PLATING_BUCKET.getName() );

        EasyMock.verify(mocks);
    }

    public void testNoFixups() throws Exception {
        Collection<Long> deletedEntityIds = new ArrayList<>();
        Collection<Long> modifiedEntityIds = new ArrayList<>();
        Collection<Long> addedEntityIds = new ArrayList<>();
        String etlDateStr = "20130623182000";

        tst.processFixups(deletedEntityIds, modifiedEntityIds, etlDateStr);
    }

    @Test(groups = TestGroups.DATABASE_FREE, enabled = true)
    public void testFixups() throws Exception {
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


    private void verifyRecord(String record, String eventName) {
        verifyRecord(record, eventName, labBatchName);
    }

    private void verifyRecord(String record, String eventName, String expectedLabBatchName) {
        EtlTestUtilities.verifyRecord( record, etlDateStr, "F", String.valueOf(entityId),
                String.valueOf(workflowId), String.valueOf(processId), eventName, String.valueOf(pdoId),
                sampleKey, expectedLabBatchName.equals(LabEventEtl.NONE)?"":lcsetSampleKey, String.valueOf(expectedLabBatchName), location, String.valueOf(vesselId),
                "", ExtractTransform.formatTimestamp(eventDate), programName, indexingSchemeName, "", "E");
    }
}


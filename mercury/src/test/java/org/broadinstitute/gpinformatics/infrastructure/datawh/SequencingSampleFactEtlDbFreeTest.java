package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.*;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch.LabBatchType;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.*;

import static org.easymock.EasyMock.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE)
public class SequencingSampleFactEtlDbFreeTest {
    private String etlDateStr = ExtractTransform.secTimestampFormat.format(new Date());
    private long entityId = 9988776655L;
    private String runName = "hiseqRun_name_dbfreetest";
    private Date runDate = new Date(1377000000000L);
    private String barcode = "22223333";
    private String flowcellBarcode = "44445555";
    private String machineName = "ABC-DEF";
    private String cartridgeName = "flowcell09u1234-8931";
    private long operator = 5678L;
    private String now = String.valueOf(System.currentTimeMillis());
    private String[] molecularIndex = new String[] {"ATTACCA","GTTACCA","CTTACCA"};
    private String[] molecularIndexSchemeName = new String[] {"abcd-", "bcde-", "cdef-"};
    private long researchProjectId = 33221144L;
    private Set<SampleInstance> sampleInstances = new HashSet<>();
    private List<Reagent> reagents = new ArrayList<>();

    private SequencingRun run;
    private SequencingSampleFactEtl tst;

    private AuditReaderDao auditReader = createMock(AuditReaderDao.class);
    private IlluminaSequencingRunDao dao = createMock(IlluminaSequencingRunDao.class);
    private ProductOrderDao pdoDao = createMock(ProductOrderDao.class);
    private RunCartridge runCartridge = createMock(RunCartridge.class);
    private ResearchProject researchProject = createMock(ResearchProject.class);
    private ProductOrder pdo = createMock(ProductOrder.class);
    private SampleInstance sampleInstance = createMock(SampleInstance.class);
    private SampleInstance sampleInstance2 = createMock(SampleInstance.class);

    private Object[] mocks = new Object[]
            {auditReader, dao, pdoDao, runCartridge, researchProject, pdo, sampleInstance, sampleInstance2};

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void objSetUp() {
        // Fixes up name uniqueness by appending an mSec timestamp.
        for (int i = 0; i < molecularIndexSchemeName.length; ++i) {
            molecularIndexSchemeName[i] += now;
        }
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        reset(mocks);
        reagents.clear();
        sampleInstances.clear();

        sampleInstances.add(sampleInstance);
        run = new SequencingRun(runName, barcode, machineName, operator, false, runDate, runCartridge, "/some/dirname");
        run.setSequencingRunId(entityId);
        reset(runCartridge);

        tst = new SequencingSampleFactEtl(dao, pdoDao);
        tst.setAuditReaderDao(auditReader);
    }

    public void testEtlFlags() throws Exception {
        replay(mocks);

        assertEquals(tst.entityClass, SequencingRun.class);
        assertEquals(tst.baseFilename, "sequencing_sample_fact");
        assertEquals(tst.entityId(run), (Long) entityId);

        verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        expect(dao.findById(SequencingRun.class, -1L)).andReturn(null);
        replay(mocks);

        assertEquals(tst.dataRecords(etlDateStr, false, -1L).size(), 0);

        verify(mocks);
    }

    public void testIncrementalEtl() throws Exception {
        // Sets up the molecular barcode.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[0]));
        MolecularIndexingScheme mis = new MolecularIndexingScheme(positionIndexMap);
        mis.setName(molecularIndexSchemeName[0]);
        reagents.add(new MolecularIndexReagent(mis));

        expect(dao.findById(SequencingRun.class, entityId)).andReturn(run).times(2);
        expect(runCartridge.getCartridgeName()).andReturn(cartridgeName).times(2);
        expect(runCartridge.getCartridgeBarcode()).andReturn(flowcellBarcode).times(2);
        expect(runCartridge.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW))
                .andReturn(sampleInstances).times(2);
        String pdoKey = "PDO-0123";
        expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey).times(2);

        // Only needs this set of expects once for the cache fill.
        long pdoId = 44332211L;
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-0123";
        expect(sampleInstance.getStartingSample())
                .andReturn(new MercurySample(sampleKey)).times(2);
        expect(sampleInstance.getReagents()).andReturn(reagents).times(2);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), molecularIndexSchemeName[0], pdoId, sampleKey);

        // Tests the pdo cache.  Should just skip some of the expects.
        records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), molecularIndexSchemeName[0], pdoId, sampleKey);

        verify(mocks);
    }

    public void testMultiple1() throws Exception {
        // Adds two molecular barcodes but only one reagent.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[0]));
        positionIndexMap.put(IndexPosition.ILLUMINA_P7, new MolecularIndex(molecularIndex[1]));
        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[1]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        expect(dao.findById(SequencingRun.class, entityId)).andReturn(run);
        expect(runCartridge.getCartridgeName()).andReturn(cartridgeName);
        expect(runCartridge.getCartridgeBarcode()).andReturn(flowcellBarcode);
        expect(runCartridge.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstances);
        String pdoKey = "PDO-0012";
        expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey);

        long pdoId = 55443322L;
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-1234";
        expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey));
        expect(sampleInstance.getReagents()).andReturn(reagents);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), molecularIndexSchemeName[1], pdoId, sampleKey);

        verify(mocks);
    }

    public void testMultiple2() throws Exception {
        // Adds two molecular barcodes in two reagents.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[1]));
        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[1]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        positionIndexMap.clear();
        positionIndexMap.put(IndexPosition.ILLUMINA_P7, new MolecularIndex(molecularIndex[2]));
        molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[2]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        expect(dao.findById(SequencingRun.class, entityId)).andReturn(run);
        expect(runCartridge.getCartridgeName()).andReturn(cartridgeName);
        expect(runCartridge.getCartridgeBarcode()).andReturn(flowcellBarcode);
        expect(runCartridge.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstances);
        String pdoKey = "PDO-6543";
        expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey);

        long pdoId = 66554433L;
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-2345";
        expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey));
        expect(sampleInstance.getReagents()).andReturn(reagents);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 1);
        verifyRecord(records.iterator().next(), "MULTIPLE_0", pdoId, sampleKey);

        verify(mocks);
    }

    public void testNone() throws Exception {
        // Adds no molecular barcodes, but has non-barcode reagents.
        reagents.add(new GenericReagent("DMSO", "a whole lot"));
        reagents.add(new GenericReagent("H2O", "Quabbans finest"));

        expect(dao.findById(SequencingRun.class, entityId)).andReturn(run);
        expect(runCartridge.getCartridgeName()).andReturn(cartridgeName);
        expect(runCartridge.getCartridgeBarcode()).andReturn(flowcellBarcode);
        // Adds a second sampleInstance
        sampleInstances.add(sampleInstance2);
        expect(runCartridge.getSampleInstances(SampleType.WITH_PDO, LabBatchType.WORKFLOW)).andReturn(sampleInstances);
        String pdoKey = "PDO-7654";
        expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey);
        expect(sampleInstance2.getProductOrderKey()).andReturn(pdoKey);

        long pdoId = 77665544L;
        expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        expect(pdo.getProductOrderId()).andReturn(pdoId);
        expect(pdo.getResearchProject()).andReturn(researchProject).times(2);
        expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-3456";
        expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey));
        expect(sampleInstance.getReagents()).andReturn(reagents);

        String sampleKey2 = "SM-4567";
        expect(sampleInstance2.getStartingSample()).andReturn(new MercurySample(sampleKey2));
        expect(sampleInstance2.getReagents()).andReturn(reagents);

        replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateStr, false, entityId);
        assertEquals(records.size(), 2);
        // Skips testing sample key this time since record order is not guaranteed.
        verifyRecord(records.iterator().next(), "NONE_0", pdoId, null);
        verifyRecord(records.iterator().next(), "NONE_0", pdoId, null);

        verify(mocks);
    }

    private String[] verifyRecord(String record, String expectedName, long pdoId, String sampleKey) {
        int i = 0;
        String[] parts = record.split(",");
        assertEquals(parts[i++], etlDateStr);
        assertEquals(parts[i++], "F");
        assertEquals(parts[i++], String.valueOf(entityId));
        assertEquals(parts[i++], flowcellBarcode);
        assertEquals(parts[i++], cartridgeName);
        assertEquals(parts[i++], expectedName);
        assertEquals(parts[i++], String.valueOf(pdoId));
        assert(sampleKey == null || parts[i].equals(sampleKey));
        i++;
        assertEquals(parts[i++], String.valueOf(researchProjectId));

        assertEquals(parts.length, i);
        return parts;
    }
}


package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.template.TemplateEngine;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.IlluminaSequencingRunDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme.IndexPosition;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel.SampleType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.limsquery.generated.ReadStructureRequest;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.easymock.EasyMock;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * dbfree unit test of entity etl.
 */

@Test(groups = TestGroups.DATABASE_FREE, enabled = true)
public class SequencingSampleFactEtlDbFreeTest extends BaseEventTest {
    public static final String FCT_TICKET = "FCT-1";
    private final String etlDateString = ExtractTransform.formatTimestamp(new Date());
    private final long entityId = 9988776655L;
    private final String runName = "hiseqRun_name_dbfreetest";
    private final Date runDate = new Date(1377000000000L);
    private final Date tubeCreateDate = new Date(1376000000000L);
    private final String tubeCreateDateFormat = ExtractTransform.formatTimestamp(tubeCreateDate);
    private final String barcode = "22223333";
    private final String tubeBarcode = "44445555";
    private final String machineName = "ABC-DEF";
    private final String cartridgeName = "flowcell09u1234-8931";
    private final long operator = 5678L;
    private final String now = String.valueOf(java.lang.System.currentTimeMillis());
    private final String[] molecularIndex = new String[]{"ATTACCA", "GTTACCA", "CTTACCA"};
    private final String[] molecularIndexSchemeName = new String[]{"abcd-", "bcde-", "cdef-"};
    private final long researchProjectId = 33221144L;
    private final Set<SampleInstance> sampleInstances = new HashSet<>();
    private final List<Reagent> reagents = new ArrayList<>();
    private LabBatch fctBatch;
    private final Map<VesselPosition, LabVessel> laneVesselsAndPositions = new HashMap<>();

    private SequencingRun run;
    private SequencingSampleFactEtl tst;

    private AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private IlluminaSequencingRunDao dao = EasyMock.createMock(IlluminaSequencingRunDao.class);
    private ProductOrderDao pdoDao = EasyMock.createMock(ProductOrderDao.class);
    private RunCartridge runCartridge = EasyMock.createMock(RunCartridge.class);
    private VesselContainer vesselContainer = EasyMock.createMock(VesselContainer.class);
    private ResearchProject researchProject = EasyMock.createMock(ResearchProject.class);
    private ProductOrder pdo = EasyMock.createMock(ProductOrder.class);
    private SampleInstance sampleInstance = EasyMock.createMock(SampleInstance.class);
    private SampleInstance sampleInstance2 = EasyMock.createMock(SampleInstance.class);
    private LabVessel denatureSource = EasyMock.createMock(BarcodedTube.class);

    private Object[] mocks = new Object[]{auditReader, dao, pdoDao, runCartridge, researchProject, pdo,
            sampleInstance, sampleInstance2, vesselContainer, denatureSource};

    private final TemplateEngine templateEngine = new TemplateEngine();
    private LabBatch workflowBatch;

    @BeforeClass(groups = TestGroups.DATABASE_FREE)
    public void objSetUp() {
        // Fixes up name uniqueness by appending an mSec timestamp.
        for (int i = 0; i < molecularIndexSchemeName.length; ++i) {
            molecularIndexSchemeName[i] += now;
        }
    }

    @BeforeMethod(groups = TestGroups.DATABASE_FREE)
    public void setUp() {
        templateEngine.postConstruct();
        super.setUp();

        reagents.clear();
        sampleInstances.clear();

        sampleInstances.add(sampleInstance);
        EasyMock.reset(mocks);

        run = new SequencingRun(runName, barcode, machineName, operator, false, runDate, runCartridge, "/some/dirname");
        run.setSequencingRunId(entityId);

        tst = new SequencingSampleFactEtl(dao, pdoDao);
        tst.setAuditReaderDao(auditReader);

        workflowBatch = new LabBatch("Exome Express Batch", new HashSet<LabVessel>(), LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);

        laneVesselsAndPositions.clear();
        laneVesselsAndPositions.put(VesselPosition.LANE1, denatureSource);
        laneVesselsAndPositions.put(VesselPosition.LANE2, denatureSource);

        fctBatch = new LabBatch("FCT1", Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        EasyMock.reset(mocks);
    }

    public void testEtlFlags() throws Exception {
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.entityClass, SequencingRun.class);
        Assert.assertEquals(tst.baseFilename, "sequencing_sample_fact");
        Assert.assertEquals(tst.entityId(run), (Long) entityId);

        EasyMock.verify(mocks);
    }

    public void testCantMakeEtlRecord() throws Exception {
        EasyMock.expect(dao.findById(SequencingRun.class, -1L)).andReturn(null);
        EasyMock.replay(mocks);

        Assert.assertEquals(tst.dataRecords(etlDateString, false, -1L).size(), 0);

        EasyMock.verify(mocks);
    }

    private void doExpects() {
        EasyMock.expect(dao.findById(SequencingRun.class, entityId)).andReturn(run).anyTimes();
        EasyMock.expect(runCartridge.getCartridgeName()).andReturn(cartridgeName).anyTimes();
        EasyMock.expect(runCartridge.getVesselGeometry()).andReturn(VesselGeometry.FLOWCELL1x2).anyTimes();
        EasyMock.expect(runCartridge.getContainerRole()).andReturn(vesselContainer).anyTimes();

        EasyMock.expect(runCartridge.getAllLabBatches(EasyMock.anyObject(LabBatch.LabBatchType.class))).andReturn(
                Collections.singleton(fctBatch)).anyTimes();
        EasyMock.expect(runCartridge.getNearestTubeAncestorsForLanes()).andReturn(laneVesselsAndPositions).anyTimes();

        EasyMock.expect(denatureSource.getLabel()).andReturn(tubeBarcode).anyTimes();
        EasyMock.expect(denatureSource.getCreatedOn()).andReturn(tubeCreateDate).anyTimes();

        EasyMock.expect(denatureSource.getSampleInstances(EasyMock.anyObject(SampleType.class),
                                                          EasyMock.anyObject(LabBatch.LabBatchType.class)))
                .andReturn(sampleInstances).anyTimes();

        for (SampleInstance sampleInstance1 : sampleInstances) {
            EasyMock.expect(sampleInstance1.getLabBatch()).andReturn(workflowBatch).anyTimes();
        }
    }

    public void testIncrementalEtl() throws Exception {
        // Sets up the molecular barcode.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[0]));
        MolecularIndexingScheme mis = new MolecularIndexingScheme(positionIndexMap);
        mis.setName(molecularIndexSchemeName[0]);
        reagents.add(new MolecularIndexReagent(mis));

        doExpects();

        String pdoKey = "PDO-0123";
        EasyMock.expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey).anyTimes();

        // Only needs this set of expects once for the cache fill.
        long pdoId = 44332211L;
        EasyMock.expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo).anyTimes();
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId).anyTimes();
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId).anyTimes();

        String sampleKey = "SM-0123";
        EasyMock.expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey)).anyTimes();
        EasyMock.expect(sampleInstance.getReagents()).andReturn(reagents).anyTimes();

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, molecularIndexSchemeName[0], pdoId, sampleKey, 2, tubeBarcode,
                             tubeCreateDateFormat, cartridgeName, researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, molecularIndexSchemeName[0], pdoId, sampleKey, 1, tubeBarcode,
                             tubeCreateDateFormat, cartridgeName, researchProjectId, workflowBatch.getBatchName());
            }
        }
        // Tests the pdo cache.  Should just skip some of the expects.
        records = tst.dataRecords(etlDateString, false, entityId);
        Assert.assertEquals(records.size(), 2);

    }

    public void testMultiple1() throws Exception {
        // Adds two molecular barcodes but only one reagent.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[0]));
        positionIndexMap.put(IndexPosition.ILLUMINA_P7, new MolecularIndex(molecularIndex[1]));
        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[1]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        doExpects();

        String pdoKey = "PDO-0012";
        EasyMock.expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey).anyTimes();

        long pdoId = 55443322L;
        EasyMock.expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-1234";
        EasyMock.expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey)).anyTimes();
        EasyMock.expect(sampleInstance.getReagents()).andReturn(reagents).anyTimes();

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, molecularIndexSchemeName[1], pdoId, sampleKey, 2, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, molecularIndexSchemeName[1], pdoId, sampleKey, 1, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            }
        }

    }

    public void testMultiple2() throws Exception {
        // Adds two molecular barcodes in two reagents.
        Map<IndexPosition, MolecularIndex> positionIndexMap = new HashMap<>();
        positionIndexMap.put(IndexPosition.ILLUMINA_P5, new MolecularIndex(molecularIndex[2]));
        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[2]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        positionIndexMap.clear();
        positionIndexMap.put(IndexPosition.ILLUMINA_P7, new MolecularIndex(molecularIndex[0]));
        molecularIndexingScheme = new MolecularIndexingScheme(positionIndexMap);
        molecularIndexingScheme.setName(molecularIndexSchemeName[0]);
        reagents.add(new MolecularIndexReagent(molecularIndexingScheme));

        doExpects();

        String pdoKey = "PDO-6543";
        EasyMock.expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey).anyTimes();

        long pdoId = 66554433L;
        EasyMock.expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        String sampleKey = "SM-2345";
        EasyMock.expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey)).anyTimes();
        EasyMock.expect(sampleInstance.getReagents()).andReturn(reagents).anyTimes();

        EasyMock.replay(mocks);
        String expectedMolecularIndexName = molecularIndexSchemeName[0] + " " + molecularIndexSchemeName[2];
        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, expectedMolecularIndexName, pdoId, sampleKey, 2, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, expectedMolecularIndexName, pdoId, sampleKey, 1, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            }
        }

    }

    public void testGenericIndexAndDedup() throws Exception {
        // Has only non-indexed reagents so molecular indexes are all "NONE"
        reagents.add(new GenericReagent("DMSO", "a whole lot"));
        reagents.add(new GenericReagent("H2O", "Quabbans finest"));
        sampleInstances.add(sampleInstance2);

        doExpects();

        String pdoKey = "PDO-7654";
        EasyMock.expect(sampleInstance.getProductOrderKey()).andReturn(pdoKey).anyTimes();
        EasyMock.expect(sampleInstance2.getProductOrderKey()).andReturn(pdoKey).anyTimes();

        long pdoId = 77665544L;
        EasyMock.expect(pdoDao.findByBusinessKey(pdoKey)).andReturn(pdo).anyTimes();
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId).anyTimes();
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId).anyTimes();

        String sampleKey = "SM-3456";
        EasyMock.expect(sampleInstance.getStartingSample()).andReturn(new MercurySample(sampleKey)).anyTimes();
        EasyMock.expect(sampleInstance.getReagents()).andReturn(reagents).anyTimes();

        EasyMock.expect(sampleInstance2.getStartingSample()).andReturn(new MercurySample(sampleKey)).anyTimes();
        EasyMock.expect(sampleInstance2.getReagents()).andReturn(reagents).anyTimes();

        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        // One tube containing 2 samples is put on each of the 2 lanes, but de-duplication is done based on
        // molecular index, so only one sample per lane will be kept.  It's undefined which sample is kept.
        Assert.assertEquals(records.size(), 2);
        boolean foundLane1 = false;
        boolean foundLane2 = false;
        for (String record : records) {
            int laneNumber = Integer.parseInt(record.split(",")[4]);
            if (laneNumber == 1) {
                foundLane1 = true;
            }
            if (laneNumber == 2) {
                foundLane2 = true;
            }
            verifyRecord(record, "NONE", pdoId, sampleKey, laneNumber, denatureSource.getLabel(),
                         ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                         researchProjectId, workflowBatch.getBatchName());
        }
        Assert.assertTrue(foundLane1);
        Assert.assertTrue(foundLane2);
    }

    public void testWithEventHistory() throws Exception {
        expectedRouting = SystemRouter.System.MERCURY;

        final ProductOrder productOrder = ProductOrderTestFactory.buildExExProductOrder(96);
        Long pdoId = 9202938094820L;
        Date runDate = new Date();
        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        LabBatch workflowBatch = new LabBatch("Exome Express Batch",
                                              new HashSet<LabVessel>(mapBarcodeToTube.values()),
                                              LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(Workflow.AGILENT_EXOME_EXPRESS);
        workflowBatch.setCreatedOn(EX_EX_IN_MERCURY_CALENDAR.getTime());

        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, "1");
        //Build Event History
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                                                                                  String.valueOf(runDate.getTime()),
                                                                                  "1", true);
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                                               picoPlatingEntityBuilder.getNormTubeFormation(),
                                               picoPlatingEntityBuilder.getNormalizationBarcode(), "1");
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder =
                runLibraryConstructionProcess(exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                                              exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                                              exomeExpressShearingEntityBuilder.getShearingPlate(), "1");
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                                          libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                                          libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");
        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                                                          hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                                                          hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                                                          "1");

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);

        LabBatch fct = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);

        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder =
                runHiSeq2500FlowcellProcess(qtpEntityBuilder.getDenatureRack(), "1" + "ADXX", FCT_TICKET,
                                            ProductionFlowcellPath.DILUTION_TO_FLOWCELL, null,
                                            Workflow.AGILENT_EXOME_EXPRESS);
        LabVessel dilutionSource =
                hiSeq2500FlowcellEntityBuilder.getDilutionRack().getContainerRole().getVesselAtPosition(
                        VesselPosition.A01);

        IlluminaFlowcell illuminaFlowcell = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell();


        String machineName = "Superman";

        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
        String flowcellBarcode = illuminaFlowcell.getCartridgeBarcode();

        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode, flowcellBarcode + dateFormat.format(runDate),
                                                  runDate, machineName, runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory runFactory =
                new IlluminaSequencingRunFactory(EasyMock.createMock(JiraCommentUtil.class));
        IlluminaSequencingRun run = runFactory.buildDbFree(runBean, illuminaFlowcell);

        run.setSequencingRunId(entityId);

        ReadStructureRequest readStructureRequest = new ReadStructureRequest();
        readStructureRequest.setRunBarcode(run.getRunBarcode());
        readStructureRequest.setSetupReadStructure("71T8B8B71T");
        readStructureRequest.setActualReadStructure("101T8B8B101T");

        runFactory.storeReadsStructureDBFree(readStructureRequest, run);

        EasyMock.expect(dao.findById(SequencingRun.class, entityId)).andReturn(run);
        EasyMock.expect(pdoDao.findByBusinessKey(EasyMock.anyObject(String.class))).andReturn(pdo);
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId);
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId);

        EasyMock.replay(mocks);
        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);

        Assert.assertEquals(records.size(), 192);

        Map<String, List<String>> mapSampleToRecord = new HashMap<>();

        for (String record : records) {
            String[] recordParts = record.split(",");

            String recordSampleKey = recordParts[7];
            if (StringUtils.isNotBlank(recordSampleKey)) {
                if (!mapSampleToRecord.containsKey(recordSampleKey)) {
                    mapSampleToRecord.put(recordSampleKey, new ArrayList<String>(2));
                }
                mapSampleToRecord.get(recordSampleKey).add(record);
            }
        }


        for (SampleInstance testInstance : dilutionSource.getSampleInstances()) {

            SortedSet<String> names = new TreeSet<>();

            for (Reagent reagent : testInstance.getReagents()) {
                if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                    names.add(((MolecularIndexReagent) reagent).getMolecularIndexingScheme().getName());
                }
            }
            String molecularIndexingSchemeName = (names.size() == 0 ? "NONE" : StringUtils.join(names, " "));

            for (String record : mapSampleToRecord.get(testInstance.getStartingSample().getSampleKey())) {
                if (record.contains(",2,")) {
                    verifyRecord(record, molecularIndexingSchemeName, pdoId,
                                 testInstance.getStartingSample().getSampleKey(), 2, denatureSource.getLabel(),
                                 ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()),
                                 illuminaFlowcell.getLabel(), researchProjectId, workflowBatch.getBatchName());
                } else {
                    verifyRecord(record, molecularIndexingSchemeName, pdoId,
                                 testInstance.getStartingSample().getSampleKey(), 1, denatureSource.getLabel(),
                                 ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()),
                                 illuminaFlowcell.getLabel(), researchProjectId, workflowBatch.getBatchName());
                }
            }
        }

        EasyMock.verify(mocks);
    }

    private String[] verifyRecord(String record, String expectedName, long pdoId, String sampleKey, Integer lane,
                                  String tubeBarcode, String createdDateStr, String cartridgeName,
                                  long researchProjectId, String batchName) {
        int i = 0;
        String[] parts = record.split(",");
        Assert.assertEquals(parts[i++], etlDateString);
        Assert.assertEquals(parts[i++], "F");
        Assert.assertEquals(parts[i++], String.valueOf(entityId));
        Assert.assertEquals(parts[i++], cartridgeName);
        if (lane != null) {
            Assert.assertEquals(parts[i++], String.valueOf(lane));
        } else {
            i++;
        }
        Assert.assertEquals(parts[i++], expectedName);
        Assert.assertEquals(parts[i++], String.valueOf(pdoId));
        Assert.assertEquals(parts[i++], sampleKey);
        Assert.assertEquals(parts[i++], String.valueOf(researchProjectId));
        Assert.assertEquals(parts[i++], tubeBarcode);
        Assert.assertEquals(parts[i++], createdDateStr);
        Assert.assertEquals(parts[i++], batchName);

        Assert.assertEquals(parts.length, i);
        return parts;
    }
}


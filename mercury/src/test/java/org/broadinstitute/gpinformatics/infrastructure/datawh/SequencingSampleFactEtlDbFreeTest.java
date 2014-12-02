package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
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
import org.broadinstitute.gpinformatics.mercury.control.zims.ZimsIlluminaRunFactoryTest;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.GenericReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
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
    private final long researchProjectId = 33221144L;
    private final String batchName = "LCSET-wxyz";
    private final long pdoId = 564738L;
    private final String sampleName = "SM-jklm";
    private final Set<SampleInstanceV2> sampleInstances = new HashSet<>();
    private final List<Reagent> reagents = new ArrayList<>();
    private LabBatch fctBatch;
    private final Map<VesselPosition, LabVessel> laneVesselsAndPositions = new HashMap<>();

    private SequencingRun run;
    private SequencingSampleFactEtl tst;

    private AuditReaderDao auditReader = EasyMock.createMock(AuditReaderDao.class);
    private IlluminaSequencingRunDao dao = EasyMock.createMock(IlluminaSequencingRunDao.class);
    private RunCartridge runCartridge = EasyMock.createMock(RunCartridge.class);
    private VesselContainer vesselContainer = EasyMock.createMock(VesselContainer.class);
    private ResearchProject researchProject = EasyMock.createMock(ResearchProject.class);
    private ProductOrder pdo = EasyMock.createMock(ProductOrder.class);
    private ProductOrderSample pdoSample = EasyMock.createMock(ProductOrderSample.class);
    private MercurySample sample = EasyMock.createMock(MercurySample.class);
    private MercurySample sample2 = EasyMock.createMock(MercurySample.class);
    private SampleInstanceV2 sampleInstance = EasyMock.createMock(SampleInstanceV2.class);
    private SampleInstanceV2 sampleInstance2 = EasyMock.createMock(SampleInstanceV2.class);
    private LabVessel denatureSource = EasyMock.createMock(BarcodedTube.class);

    private Object[] mocks = new Object[]{auditReader, dao, runCartridge, vesselContainer, researchProject, pdo,
            pdoSample, sample, sampleInstance, sampleInstance2, denatureSource};

    private final TemplateEngine templateEngine = new TemplateEngine();
    private LabBatch workflowBatch;

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

        tst = new SequencingSampleFactEtl(dao);
        tst.setAuditReaderDao(auditReader);

        workflowBatch = new LabBatch(batchName, new HashSet<LabVessel>(), LabBatch.LabBatchType.WORKFLOW);
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

        EasyMock.expect(denatureSource.getSampleInstancesV2()).andReturn(sampleInstances).anyTimes();

        EasyMock.expect(pdoSample.getProductOrder()).andReturn(pdo).anyTimes();
        EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId).anyTimes();
        EasyMock.expect(sample.getSampleKey()).andReturn(sampleName).anyTimes();
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId).anyTimes();

        for (SampleInstanceV2 si : sampleInstances) {
            EasyMock.expect(si.getSingleProductOrderSample()).andReturn(pdoSample).anyTimes();
            EasyMock.expect(si.getRootOrEarliestMercurySample()).andReturn(sample).anyTimes();
            EasyMock.expect(si.getSingleInferredBucketedBatch()).andReturn(workflowBatch).anyTimes();
            EasyMock.expect(si.getReagents()).andReturn(reagents).anyTimes();
        }
    }

    public void testIncrementalEtl() throws Exception {
        reagents.addAll(ZimsIlluminaRunFactoryTest.makeTestReagents(1, false));
        String misName = ((MolecularIndexReagent)reagents.get(0)).getMolecularIndexingScheme().getName();

        doExpects();
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, misName, pdoId, sampleName, 2, tubeBarcode,
                             tubeCreateDateFormat, cartridgeName, researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, misName, pdoId, sampleName, 1, tubeBarcode,
                             tubeCreateDateFormat, cartridgeName, researchProjectId, workflowBatch.getBatchName());
            }
        }
        // Tests the pdo cache.  Should just skip some of the expects.
        records = tst.dataRecords(etlDateString, false, entityId);
        Assert.assertEquals(records.size(), 2);

    }

    public void testMultiple1() throws Exception {
        // Adds a second molecular barcode on the one reagent.
        reagents.addAll(ZimsIlluminaRunFactoryTest.makeTestReagents(1, true));
        String misName = ((MolecularIndexReagent)reagents.get(0)).getMolecularIndexingScheme().getName();

        doExpects();
        EasyMock.replay(mocks);

        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, misName, pdoId, sampleName, 2, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, misName, pdoId, sampleName, 1, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            }
        }

    }

    public void testMultiple2() throws Exception {
        // Adds two molecular barcodes in two reagents.
        reagents.addAll(ZimsIlluminaRunFactoryTest.makeTestReagents(2, false));
        String[] misNames = new String[] {
                ((MolecularIndexReagent)reagents.get(0)).getMolecularIndexingScheme().getName(),
                ((MolecularIndexReagent)reagents.get(1)).getMolecularIndexingScheme().getName()
        };

        doExpects();
        EasyMock.replay(mocks);
        Collection<String> records = tst.dataRecords(etlDateString, false, entityId);
        EasyMock.verify(mocks);

        Assert.assertEquals(records.size(), 2);
        for (String record : records) {
            if (record.contains(",2,")) {
                verifyRecord(record, misNames, pdoId, sampleName, 2, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            } else {
                verifyRecord(record, misNames, pdoId, sampleName, 1, denatureSource.getLabel(),
                             ExtractTransform.formatTimestamp(denatureSource.getCreatedOn()), cartridgeName,
                             researchProjectId, workflowBatch.getBatchName());
            }
        }

    }

    public void testGenericIndexAndDedup() throws Exception {
        // Has only non-indexed reagents so molecular indexes are all "NONE"
        reagents.add(new GenericReagent("DMSO", "a whole lot", new Date()));
        reagents.add(new GenericReagent("H2O", "Quabbans finest", new Date()));
//        sampleInstances.add(sampleInstance2);

        doExpects();

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
            verifyRecord(record, "NONE", pdoId, sampleName, laneNumber, denatureSource.getLabel(),
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

        EasyMock.expect(dao.findById(SequencingRun.class, entityId)).andReturn(run).anyTimes();
/*
        EasyMock.expect(runCartridge.getCartridgeName()).andReturn(cartridgeName).anyTimes();
        EasyMock.expect(runCartridge.getVesselGeometry()).andReturn(VesselGeometry.FLOWCELL1x2).anyTimes();
        EasyMock.expect(runCartridge.getContainerRole()).andReturn(vesselContainer).anyTimes();

        EasyMock.expect(runCartridge.getAllLabBatches(EasyMock.anyObject(LabBatch.LabBatchType.class))).andReturn(
                Collections.singleton(fctBatch)).anyTimes();
        EasyMock.expect(runCartridge.getNearestTubeAncestorsForLanes()).andReturn(laneVesselsAndPositions).anyTimes();

        EasyMock.expect(denatureSource.getLabel()).andReturn(tubeBarcode).anyTimes();
        EasyMock.expect(denatureSource.getCreatedOn()).andReturn(tubeCreateDate).anyTimes();

        EasyMock.expect(denatureSource.getSampleInstancesV2()).andReturn(sampleInstances).anyTimes();

        for (SampleInstanceV2 sampleInstance1 : sampleInstances) {
            EasyMock.expect(sampleInstance1.getSingleProductOrderSample()).andReturn(pdoSample).anyTimes();
            EasyMock.expect(pdoSample.getProductOrder()).andReturn(pdo).anyTimes();
            EasyMock.expect(pdo.getProductOrderId()).andReturn(pdoId).anyTimes();
            EasyMock.expect(sampleInstance1.getRootOrEarliestMercurySample()).andReturn(sample).anyTimes();
            EasyMock.expect(sample.getSampleKey()).andReturn("SM-abcd").anyTimes();
            EasyMock.expect(sampleInstance1.getSingleInferredBucketedBatch()).andReturn(workflowBatch).anyTimes();
            EasyMock.expect(workflowBatch.getBatchName()).andReturn("LCSET-jklm").anyTimes();
            EasyMock.expect(sampleInstance1.getReagents()).andReturn(reagents).anyTimes();
        }
*/
        EasyMock.expect(pdo.getResearchProject()).andReturn(researchProject).anyTimes();
        EasyMock.expect(researchProject.getResearchProjectId()).andReturn(researchProjectId).anyTimes();

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

        return verifyRecord(record, new String[]{expectedName}, pdoId, sampleKey, lane, tubeBarcode,
                createdDateStr, cartridgeName, researchProjectId, batchName);
    }

    private String[] verifyRecord(String record, String[] expectedNames, long pdoId, String sampleKey, Integer lane,
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
        // Every expected name must be present in names, in any order.
        String names = parts[i++];
        for (String expectedName : expectedNames) {
            Assert.assertTrue(names.contains(expectedName));
        }
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


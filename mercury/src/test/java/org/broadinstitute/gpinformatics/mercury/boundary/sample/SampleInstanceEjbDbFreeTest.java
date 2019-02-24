package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.products.ProductDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.AnalysisTypeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VarioskanParserTest;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.AnalysisType;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.IceEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor.REQUIRED_VALUE_IS_MISSING;
import static org.broadinstitute.gpinformatics.mercury.test.LabEventTest.FCT_TICKET;
import static org.mockito.Matchers.anyString;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleInstanceEjbDbFreeTest extends BaseEventTest {
    private final boolean OVERWRITE = true;

    private LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
    private SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
    private MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
    private MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
    private ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
    private SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
    private JiraService jiraService = Mockito.mock(JiraService.class);
    private ReferenceSequenceDao referenceSequenceDao = Mockito.mock(ReferenceSequenceDao.class);
    private AnalysisTypeDao analysisTypeDao = Mockito.mock(AnalysisTypeDao.class);
    private ProductDao productDao = Mockito.mock(ProductDao.class);

    enum TestType {EXTERNAL_LIBRARY, WALKUP}

    @Test
    public void testWalkupSequencing() throws Exception {
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.WALKUP);
        MessageCollection messages = new MessageCollection();
        sampleInstanceEjb.verifyAndPersistSubmission(new WalkUpSequencing() {{
            setLibraryName("TEST_LIBRARY");
            setTubeBarcode("TEST_BARCODE");
            setReadType("TEST_READ_TYPE");
            setAnalysisType("TEST");
            setLabName("TEST lab");
            setBaitSetName("TEST_BAIT");
            setReadType("Paried End");
        }}, messages);

        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors(), "; "));
    }

    @Test
    public void testExternalLibrary() throws Exception {
        String file = "testdata/externalLibDbFreeSuccess.xls";
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.EXTERNAL_LIBRARY);
        MessageCollection messageCollection = new MessageCollection();
        ExternalLibraryProcessor processor = new ExternalLibraryProcessor();

        // Uploads the spreadsheet.
        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                VarioskanParserTest.getSpreadsheet(file), OVERWRITE, processor, messageCollection, null);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getInfos().iterator().next()
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));

        // Checks SampleInstanceEntities that were created.
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), ". "));
        Assert.assertTrue(messageCollection.getInfos().get(0)
                .startsWith(String.format(SampleInstanceEjb.IS_SUCCESS, 2)),
                StringUtils.join(messageCollection.getInfos(), "; "));
        Assert.assertEquals(entities.size(), 2);

        Map<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();
        for (int i = 0; i < entities.size(); ++i) {
            SampleInstanceEntity entity = entities.get(i);

            Assert.assertTrue(OrmUtil.proxySafeIsInstance(entity.getLabVessel(), BarcodedTube.class));
            BarcodedTube tube = OrmUtil.proxySafeCast(entity.getLabVessel(), BarcodedTube.class);
            mapBarcodeToTube.put(tube.getLabel(), tube);
            Assert.assertEquals(tube.getLabel(), "JT041431");

            String libraryName = select(i, "Jon_Test_3a", "Jon_Test_4b");
            Assert.assertEquals(entity.getSampleLibraryName(), libraryName);
            MercurySample mercurySample = entity.getMercurySample();
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            Assert.assertNotNull(mercurySample.getSampleKey(), select(i, "SM-JT12", "SM-JT23"));
            Assert.assertEquals(entity.getRootSample().getSampleKey(), select(i, "SM-46IRUT1", "SM-46IRUT2"));

            Assert.assertEquals(entity.getMolecularIndexingScheme().getName(),
                    select(i, "Illumina_P5-Nijow_P7-Waren","Illumina_P5-Piwan_P7-Bidih"));

            Assert.assertEquals(entity.getReagentDesign().getName(), "NewtonCheh_NatPepMDC_12genes3regions_Sep2011");

            Assert.assertEquals(entity.getAggregationParticle(), select(i, "1", ""));

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.BSP);
            SampleData sampleData = mercurySample.getSampleData();

            Assert.assertEquals(sampleData.getCollaboratorsSampleName(),
                    select(i, "COLLAB-JT04121", "COLLAB-JT04122"));
            Assert.assertEquals(sampleData.getCollaboratorParticipantId(),
                    select(i, "COLLAB-P-JT04121", "COLLAB-P-JT04122"));

            Assert.assertEquals(sampleData.getGender(), "");
            Assert.assertEquals(sampleData.getOrganism(), "Homo Sapiens");

            Assert.assertEquals(entity.getAnalysisType().getBusinessKey(), "HybridSelection.Resequencing");

            Assert.assertEquals(entity.getReadLength(), Arrays.asList(440, 101).get(i));

            Assert.assertEquals(entity.getUmisPresent(), new Boolean[]{null, Boolean.TRUE}[i]);

            Assert.assertEquals(tube.getVolume(), new BigDecimal("0.60"));
            List<LabMetric> metrics = tube.getNearestMetricsOfType(LabMetric.MetricType.FINAL_LIBRARY_SIZE);
            Assert.assertEquals(metrics.size(), 1);
            Assert.assertEquals(metrics.get(0).getValue(), new BigDecimal("2"));
            Assert.assertEquals(tube.getConcentration(), new BigDecimal("4.44"));

            Assert.assertEquals(entity.getInsertSize(), select(i, null, "31-31"));

            Assert.assertEquals(entity.getReferenceSequence().getName(),
                    select(i, "Homo_sapiens_assembly19", "Homo_sapiens_assembly38"));

            Assert.assertEquals(entity.getSequencerModel().getTechnology(),
                    select(i, "HiSeq X 10", "HiSeq 2500 Rapid Run"));

            Assert.assertEquals(entity.getAggregationDataType(), select(i, "", "Exome"));
        }
        assertSampleInstanceEntitiesPresent(mapBarcodeToTube.values(), entities);

        // Runs the workflow: PDO creation, LCSET creation, bucketing, pico plating, shearing, LC,
        // HybSelect, QTP, sequencing.
        String workflow = Workflow.ICE_EXOME_EXPRESS;
        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(0, "PDO-TEST123");
        Assert.assertTrue(entities.size() <= NUM_POSITIONS_IN_RACK,
                entities.size() + " samples is more than a single rack " + NUM_POSITIONS_IN_RACK);
        for (SampleInstanceEntity entity : entities) {
            ProductOrderSample productOrderSample = new ProductOrderSample(entity.getMercurySample().getSampleKey());
            productOrder.addSample(productOrderSample);
        }
        productOrder.setJiraTicketKey(productOrder.getJiraTicketKey());
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder.getProduct().setWorkflowName(workflow);
        expectedRouting = SystemRouter.System.MERCURY;

        LabBatch workflowBatch = new LabBatch("a batch", new HashSet<>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(workflow);

        String lcsetSuffix = "1";
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, lcsetSuffix);
        Assert.assertEquals(productOrder.getSamples().size(), entities.size());

        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                "P", lcsetSuffix, true);

        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), lcsetSuffix);
        Assert.assertEquals(exomeExpressShearingEntityBuilder.getShearingCleanupPlate().getSampleInstancesV2().size(),
                entities.size());
        Assert.assertEquals(exomeExpressShearingEntityBuilder.getShearingCleanupPlate().getContainerRole()
                .getSampleInstancesAtPositionV2(VesselPosition.A01).size(), entities.size());

        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(),
                lcsetSuffix, mapBarcodeToTube.size());
        Assert.assertFalse(libraryConstructionEntityBuilder.getPondRegTubeBarcodes().isEmpty());
        Assert.assertEquals(libraryConstructionEntityBuilder.getPondRegRack().getContainerRole()
                .getSampleInstancesAtPositionV2(VesselPosition.A01).size(), entities.size());
        IceEntityBuilder iceEntityBuilder = runIceProcess(
                Collections.singletonList(libraryConstructionEntityBuilder.getPondRegRack()), "1");

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(iceEntityBuilder.getCatchEnrichRack(),
                iceEntityBuilder.getCatchEnrichBarcodes(), iceEntityBuilder.getMapBarcodeToCatchEnrichTubes(), "1");

        LabVessel denatureSource = qtpEntityBuilder.getDenatureRack().getContainerRole()
                .getVesselAtPosition(VesselPosition.A01);
        new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET, ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                "designation", workflow);

        Date runDate = new Date();
        String runDateString = (new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN)).format(runDate);
        File runPath = File.createTempFile("tempRun" + runDateString, ".txt");
        String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();

        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode, flowcellBarcode + runDateString,
                runDate, "Superman", runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory illuminaSequencingRunFactory = new IlluminaSequencingRunFactory(
                EasyMock.createNiceMock(JiraCommentUtil.class));

        IlluminaSequencingRun run = illuminaSequencingRunFactory.buildDbFree(runBean,
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        List<String> flowcellSamples = new ArrayList<>();
        for (SampleInstanceV2 sampleInstanceV2 : run.getSampleCartridge().getSampleInstancesV2()) {
            flowcellSamples.add(sampleInstanceV2.getNearestMercurySampleName());
        }
        Assert.assertTrue(flowcellSamples.containsAll(processor.getSampleNames()));
    }

    @Test
    public void testNullSpreadsheet() throws Exception {
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.EXTERNAL_LIBRARY);
        MessageCollection messageCollection = new MessageCollection();

        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                new ByteArrayInputStream(new byte[]{0}),
                OVERWRITE, new ExternalLibraryProcessor(), messageCollection, null);

        Assert.assertEquals(messageCollection.getErrors().size(), 1,
                StringUtils.join(messageCollection.getErrors(), "; "));
        Assert.assertTrue(messageCollection.getErrors().get(0).startsWith("Cannot process spreadsheet:"),
                StringUtils.join(messageCollection.getErrors(), "; "));

        Assert.assertTrue(CollectionUtils.isEmpty(entities));
    }


    @Test
    public void testExternalLibraryFail() throws Exception {
        final String filename = "testdata/externalLibDbFreeFail.xls";
        SampleInstanceEjb sampleInstanceEjb = setMocks(TestType.EXTERNAL_LIBRARY);
        MessageCollection messageCollection = new MessageCollection();
        ExternalLibraryProcessor processor = new ExternalLibraryProcessor();
        List<SampleInstanceEntity> entities = sampleInstanceEjb.doExternalUpload(
                new ByteArrayInputStream(IOUtils.toByteArray(VarioskanParserTest.getSpreadsheet(filename))),
                true, processor, messageCollection, null);

        // Should be no sampleInstanceEntities.
        Assert.assertEquals(entities.size(), 0);
        // Checks the error messages for expected problems.
        List<String> errors = new ArrayList<>(messageCollection.getErrors());
        List<String> warnings = new ArrayList<>(messageCollection.getWarnings());

        List<String> expectedErrors = Arrays.asList(
                "Row #3 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                        ExternalLibraryProcessor.Headers.COLLABORATOR_SAMPLE_ID.getText(), 2, "SM-748OO"),
                String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                        ExternalLibraryProcessor.Headers.INDIVIDUAL_NAME.getText(), 2, "SM-748OO"),
                String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                        ExternalLibraryProcessor.Headers.SEX.getText(), 2, "SM-748OO"),
                String.format(SampleInstanceEjb.INCONSISTENT_SAMPLE_DATA, 3,
                        ExternalLibraryProcessor.Headers.ROOT_SAMPLE_NAME.getText(), 2, "SM-748OO"),
                String.format(SampleInstanceEjb.NONEXISTENT, 3,
                        ExternalLibraryProcessor.Headers.ROOT_SAMPLE_NAME.getText(), "SM-UNKNOWN", "Mercury"),
                String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, 3,
                        ExternalLibraryProcessor.Headers.MOLECULAR_BARCODE_NAME.getText(), "01509634244"),
                String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 3,
                        ExternalLibraryProcessor.Headers.VOLUME.getText(), 2, "01509634244"),
                String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 3,
                        ExternalLibraryProcessor.Headers.FRAGMENT_SIZE.getText(), 2, "01509634244"),
                String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 3,
                        ExternalLibraryProcessor.Headers.CONCENTRATION.getText(), 2, "01509634244"),

                String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, 4,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                String.format(SampleInstanceEjb.MISSING, 4, ExternalLibraryProcessor.Headers.FRAGMENT_SIZE.getText()),
                "Row #4 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.REFERENCE_SEQUENCE.getText()),
                "Row #4 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.SEQUENCING_TECHNOLOGY.getText()),
                String.format(SampleInstanceEjb.UNKNOWN, 4,
                        ExternalLibraryProcessor.Headers.AGGREGATION_DATA_TYPE.getText(), "Mercury"),

                "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.TUBE_BARCODE.getText()),
                "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.REFERENCE_SEQUENCE.getText()),
                "Row #5 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.SEQUENCING_TECHNOLOGY.getText()),

                "Row #6 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.LIBRARY_NAME.getText()),
                "Row #6 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                String.format(SampleInstanceEjb.MISSING, 6, ExternalLibraryProcessor.Headers.VOLUME.getText()),
                String.format(SampleInstanceEjb.BAD_RANGE, 6,
                        ExternalLibraryProcessor.Headers.INSERT_SIZE_RANGE.getText()),

                String.format(SampleInstanceEjb.UNKNOWN, 7,
                        ExternalLibraryProcessor.Headers.SEQUENCING_TECHNOLOGY.getText(), "Mercury"),

                String.format(SampleInstanceEjb.DUPLICATE, 8, ExternalLibraryProcessor.Headers.LIBRARY_NAME.getText()),
                String.format(SampleInstanceEjb.DUPLICATE_IN_TUBE, 8,
                        ExternalLibraryProcessor.Headers.MOLECULAR_BARCODE_NAME.getText(), "01509634249"),
                String.format(SampleInstanceEjb.INCONSISTENT_TUBE, 8,
                        ExternalLibraryProcessor.Headers.VOLUME.getText(), 6, "01509634249"),
                "Row #8 " + String.format(REQUIRED_VALUE_IS_MISSING,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                String.format(SampleInstanceEjb.BAD_RANGE, 8,
                        ExternalLibraryProcessor.Headers.INSERT_SIZE_RANGE.getText()),

                String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, 9,
                        ExternalLibraryProcessor.Headers.READ_LENGTH.getText()),
                String.format(SampleInstanceEjb.NONNEGATIVE_INTEGER, 9,
                        ExternalLibraryProcessor.Headers.FRAGMENT_SIZE.getText()),
                String.format(SampleInstanceEjb.NONNEGATIVE_DECIMAL, 9,
                        ExternalLibraryProcessor.Headers.VOLUME.getText()),
                String.format(SampleInstanceEjb.UNKNOWN, 9,
                        ExternalLibraryProcessor.Headers.MOLECULAR_BARCODE_NAME.getText(), "Mercury")
                );

        Collection<String> unexpectedErrors = CollectionUtils.subtract(errors, expectedErrors);
        Collection<String> missingErrors = CollectionUtils.subtract(expectedErrors, errors);

        List<String> expectedWarnings = Arrays.asList(
                String.format(SampleInstanceEjb.DUPLICATE_S_M, 3, "SM-748OO", "Illumina_P5-Nijow_P7-Waren"),
                String.format(SampleInstanceEjb.DUPLICATE_S_M, 4, "SM-748OO", "Illumina_P5-Nijow_P7-Waren"),
                String.format(SampleInstanceEjb.DUPLICATE_S_M, 6, "SM-748OO", "Illumina_P5-Nijow_P7-Waren"),
                String.format(SampleInstanceEjb.DUPLICATE_S_M, 8, "SM-748OO", "Illumina_P5-Nijow_P7-Waren"));

        Collection<String> unexpectedWarnings = CollectionUtils.subtract(warnings, expectedWarnings);
        Collection<String> missingWarnings = CollectionUtils.subtract(expectedWarnings, warnings);

        String msg = (unexpectedErrors.isEmpty() ? "" :
                " Unexpected Errors: " +  StringUtils.join(unexpectedErrors, " ; ")) +
                (missingErrors.isEmpty() ? "" : " Missing Errors: " + StringUtils.join(missingErrors, " ; ")) +
                (unexpectedWarnings.isEmpty() ? "" :
                        " Unexpected Warnings: " + StringUtils.join(unexpectedWarnings, " ; ")) +
                (missingWarnings.isEmpty() ? "" : " Missing Warnings: " + StringUtils.join(missingWarnings, " ; "));

        Assert.assertTrue(msg.isEmpty(), msg);
    }


    private boolean errorIfMissing(List<String> errors, String filename, String expected) {
        for (Iterator<String> iterator = errors.iterator(); iterator.hasNext(); ) {
            String error = iterator.next();
            if (error.startsWith(expected)) {
                iterator.remove();
                return true;
            }
        }
        Assert.fail(filename + " error message \"" + expected + "\" is missing from the remaining errors: " +
                StringUtils.join(errors, "; "));
        return false;
    }

    private <VESSEL extends LabVessel> void assertSampleInstanceEntitiesPresent(Collection<VESSEL> labVessels,
            Collection<SampleInstanceEntity> entities) {
        List<String> list = new ArrayList<>();
        for (VESSEL labVessel : labVessels) {
            for (SampleInstanceV2 sampleInstanceV2 : labVessel.getSampleInstancesV2()) {
                list.add(sampleInstanceV2.getEarliestMercurySampleName());
            }
        }
        for (SampleInstanceEntity entity : entities) {
            Assert.assertTrue(list.contains(entity.getMercurySample().getSampleKey()));
        }
    }

    private String select(int idx, String... params) {
        Assert.assertTrue(params.length > idx, idx + " cannot select " + StringUtils.join(params, ", "));
        return Arrays.asList(params).get(idx);
    }

    /** Sets up the mocks for all test cases. */
    private SampleInstanceEjb setMocks(TestType testType) throws Exception {

        // BarcodedTubes
        Mockito.when(labVesselDao.findByBarcodes(Mockito.anyList())).thenAnswer(new Answer<Map<String, LabVessel>>() {
            @Override
            public Map<String, LabVessel> answer(InvocationOnMock invocation) throws Throwable {
                Map<String, LabVessel> map = new HashMap<>();
                for (String barcode : (List<String>) invocation.getArguments()[0]) {
                    map.put(barcode, new BarcodedTube(barcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }
                return map;
            }
        });

        // Mercury samples
        final Map<String, Set<Metadata>> metadataMap = new HashMap<>();
        final Map<String, BspSampleData> bspSampleData = new HashMap<>();
        int bspIdx = 0;
        int mercuryIdx = 0;
        for (String sampleName : Arrays.asList("SM-JT12", "SM-JT23", "SM-46IRUT1", "SM-46IRUT2",
                "Lib-MOCK.FSK1.A", "4442SFF6", "4442SFF7", "4442SFP6", "4442SFP7",
                "4076255991TEST", "4076255992TEST", "4076255993TEST")) {

            if (sampleName.startsWith("SM-")) {
                // BSP samples
                Map<BSPSampleSearchColumn, String> map = new HashMap<>();
                map.put(BSPSampleSearchColumn.ROOT_SAMPLE, select(bspIdx, "SM-46IRUT1", "SM-46IRUT2", null, null));
                map.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, select(bspIdx, "COLLAB-JT04121",
                        "COLLAB-JT04122", "COLLAB-JT04121", "COLLAB-JT04122"));
                map.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, select(bspIdx, "COLLAB-P-JT04121",
                        "COLLAB-P-JT04122", "COLLAB-P-JT04121", "COLLAB-P-JT04122"));
                map.put(BSPSampleSearchColumn.PARTICIPANT_ID, select(bspIdx, "PT-JT1", "PT-JT2", "PT-JT1", "PT-JT2"));
                map.put(BSPSampleSearchColumn.SPECIES, "Homo Sapiens");
                map.put(BSPSampleSearchColumn.LSID, select(bspIdx, "broadinstitute.org:bsp.dev.sample:JT1",
                        "broadinstitute.org:bsp.dev.sample:JT2", "root1", "root2"));
                bspSampleData.put(sampleName, new BspSampleData(map));
                ++bspIdx;

            } else {
                // non-BSP samples
                Set<Metadata> metadata = new HashSet<>();
                metadata.add(new Metadata(Metadata.Key.SAMPLE_ID, sampleName));
                metadata.add(new Metadata(Metadata.Key.PATIENT_ID, select(mercuryIdx,
                        "MOCK1", "", "", "Patient X", "Patient Y", "hh", "ii", "jj")));
                metadata.add(new Metadata(Metadata.Key.SPECIES, "S"));
                metadata.add(new Metadata(Metadata.Key.GENDER, select(mercuryIdx,
                        "M", "F", "", "F", "", "M", "F", "M")));
                metadata.add(new Metadata(Metadata.Key.MATERIAL_TYPE, MaterialType.DNA.getDisplayName()));
                metadataMap.put(sampleName, metadata);
                ++mercuryIdx;
            }
        }

        Mockito.when(mercurySampleDao.findMapIdToMercurySample(Mockito.anySet())).thenAnswer(
                new Answer<Map<String, MercurySample>>() {
                    @Override
                    public Map<String, MercurySample> answer(InvocationOnMock invocation) throws Throwable {
                        Map<String, MercurySample> map = new HashMap<>();
                        for (String name : (Set<String>) invocation.getArguments()[0]) {
                            if (metadataMap.containsKey(name)) {
                                map.put(name, new MercurySample(name, metadataMap.get(name)));
                            } else if (bspSampleData.containsKey(name)) {
                                map.put(name, new MercurySample(name, bspSampleData.get(name)));
                            } else if (!name.contains("UNKNOWN")) {
                                // Root samples will have no metadata.
                                map.put(name, new MercurySample(name, name.startsWith("SM-") ?
                                        MercurySample.MetadataSource.BSP : MercurySample.MetadataSource.MERCURY));
                            }
                        }
                        return map;
                    }
                });

        Mockito.when(sampleDataFetcher.fetchSampleData(Mockito.anyCollection())).thenAnswer(
                new Answer<Map<String, SampleData>>() {
                    @Override
                    public Map<String, SampleData> answer(InvocationOnMock invocation) throws Throwable {
                        Map<String, SampleData> map = new HashMap<>();
                        for (String name : (Collection<String>) invocation.getArguments()[0]) {
                            if (bspSampleData.containsKey(name)) {
                                map.put(name, bspSampleData.get(name));
                            }
                        }
                        return map;
                    }
                });

        Mockito.when(analysisTypeDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<AnalysisType>() {
            @Override
            public AnalysisType answer(InvocationOnMock invocation) throws Throwable {
                String name = (String)invocation.getArguments()[0];
                return (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) ?
                        null : new AnalysisType(name);
            }
        });

        // ReferenceSequences
        Mockito.when(referenceSequenceDao.findCurrent(Mockito.anyString())).thenAnswer(
                new Answer<ReferenceSequence>() {
                    @Override
                    public ReferenceSequence answer(InvocationOnMock invocation) throws Throwable {
                        String name = (String)invocation.getArguments()[0];
                        return (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) ?
                                null : new ReferenceSequence(name, "");
                    }
                });

        Mockito.when(referenceSequenceDao.findByBusinessKey(Mockito.anyString())).thenAnswer(
                new Answer<ReferenceSequence>() {
                    @Override
                    public ReferenceSequence answer(InvocationOnMock invocation) throws Throwable {
                        String businessKey = (String)invocation.getArguments()[0];
                        return StringUtils.isBlank(businessKey) ? null : new ReferenceSequence(
                                StringUtils.substringBeforeLast(businessKey, "|"),
                                StringUtils.substringAfterLast(businessKey, "|"));

                    }
                });

        // MolecularIndexingScheme
        Mockito.when(molecularIndexingSchemeDao.findByName(Mockito.anyString())).thenAnswer(
                new Answer<MolecularIndexingScheme>() {
                    @Override
                    public MolecularIndexingScheme answer(InvocationOnMock invocation) throws Throwable {
                        String name = (String)invocation.getArguments()[0];
                        MolecularIndexingScheme molecularIndexingScheme = null;
                        if (StringUtils.isNotBlank(name) && name.startsWith("Illumina_")) {
                            molecularIndexingScheme = new MolecularIndexingScheme();
                            molecularIndexingScheme.setName(name);
                        }
                        return molecularIndexingScheme;
                    }
                });

        // ReagentDesign
        Mockito.when(reagentDesignDao.findByBusinessKey(Mockito.anyString())).thenAnswer(new Answer<ReagentDesign>() {
            @Override
            public ReagentDesign answer(InvocationOnMock invocation) throws Throwable {
                String name = (String)invocation.getArguments()[0];
                ReagentDesign reagentDesign = null;
                if (StringUtils.isBlank(name) || name.equalsIgnoreCase("unknown")) {
                    return null;
                }
                reagentDesign = new ReagentDesign();
                reagentDesign.setDesignName(name);
                return reagentDesign;
            }
        });

        // Product.aggregationDataType
        Mockito.when(productDao.findAggregationDataTypes()).thenAnswer(new Answer<List<String>>() {
            @Override
            public List<String> answer(InvocationOnMock invocation) throws Throwable {
                return Arrays.asList("10X_WGS", "16S", "CustomHybSel", "Custom_Selection", "Exome", "ExomePlus",
                        "Jump", "PCR", "RNA", "RRBS", "ShortRangePCR", "WGS");
            }
        });

        // SampleInstanceEntity
        Mockito.when(sampleInstanceEntityDao.findByName(anyString())).thenReturn(null);

        return new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService,
                reagentDesignDao, labVesselDao, mercurySampleDao, sampleInstanceEntityDao,
                analysisTypeDao, sampleDataFetcher, referenceSequenceDao, productDao);
    }

}

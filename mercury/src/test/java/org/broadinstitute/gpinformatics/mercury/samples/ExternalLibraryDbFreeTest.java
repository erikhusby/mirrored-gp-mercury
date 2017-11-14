package org.broadinstitute.gpinformatics.mercury.samples;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.SampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.run.IlluminaSequencingRunFactory;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessor;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNonPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
import org.broadinstitute.gpinformatics.mercury.control.vessel.JiraCommentUtil;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean;
import org.broadinstitute.gpinformatics.mercury.test.BaseEventTest;
import org.broadinstitute.gpinformatics.mercury.test.builders.ExomeExpressShearingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HiSeq2500FlowcellEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.HybridSelectionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.LibraryConstructionEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.PicoPlatingEntityBuilder;
import org.broadinstitute.gpinformatics.mercury.test.builders.ProductionFlowcellPath;
import org.broadinstitute.gpinformatics.mercury.test.builders.QtpEntityBuilder;
import org.easymock.EasyMock;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.test.LabEventTest.FCT_TICKET;

@Test(groups = TestGroups.DATABASE_FREE)
public class ExternalLibraryDbFreeTest extends BaseEventTest {
    /**
     * Database Free tests for EZPass Kiosk library.
     */
    @Test
    public void testEZPassExternalLibraries() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryEZPassTest.xlsx");
        ExternalLibraryProcessorEzPass processor = new ExternalLibraryProcessorEzPass("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        setParams(processor, true, ExternalLibraryUploadActionBean.EZPASS_KIOSK);
        testExomeExpressDBFree(processor);
    }

    /**
     * Database Free tests for Multi Organism External Libraries
     */
    @Test
    public void testExternalMultiOrganismLibrary() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryMultiOrganismTest.xlsx");
        ExternalLibraryProcessorPooledMultiOrganism processor =
                new ExternalLibraryProcessorPooledMultiOrganism("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        setParams(processor, true, ExternalLibraryUploadActionBean.MULTI_ORG);
    }

    /**
     * Database Free tests for Pooled External Libraries
     */
    @Test
    public void testExternalPooledLibrary() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryPooledTest.xlsx");
        ExternalLibraryProcessorPooled processor = new ExternalLibraryProcessorPooled("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        setParams(processor, true, ExternalLibraryUploadActionBean.POOLED);
    }

    /**
     * Database Free tests for NON Pooled External Libraries
     */
    @Test
    public void testExternalNonPooledLibrary() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryNONPooledTest.xlsx");
        ExternalLibraryProcessorNonPooled processor = new ExternalLibraryProcessorNonPooled("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        setParams(processor, true, ExternalLibraryUploadActionBean.NON_POOLED);
    }

    /**
     *  Run the test against the Ejb and check the results.
     */
    private void setParams(ExternalLibraryProcessor processor, boolean overwrite, String spreadsheetType) {
        String molSeq = processor.getMolecularBarcodeSequence().get(0);
        String projectTitle = processor.getProjectTitle().get(0);
        String productType = processor.getDataAnalysisType().get(0);
        String irb = spreadsheetType.equals(ExternalLibraryUploadActionBean.EZPASS_KIOSK) ?
                "" : processor.getIrbNumber().get(0);
        SampleInstanceEjb sampleInstanceEjb = setExternalLibraryMocks(molSeq, projectTitle, productType, irb);

        MessageCollection messageCollection = new MessageCollection();
        sampleInstanceEjb.verifyAndPersistExternalLibrary(processor, messageCollection, overwrite);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " ; "));
    }

    /**
     * This is where we setup the initial Mocks for External Library testing.
     */
    private SampleInstanceEjb setExternalLibraryMocks(String misName, String projectTitle, String productKey,
            String irbNumber) {

        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);
        MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        JiraService jiraService = new JiraServiceStub();
        ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
        SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);

        Mockito.when(molecularIndexingSchemeDao.findByName(misName)).thenReturn(new MolecularIndexingScheme());

        List<String> barcodes = new ArrayList<>();
        final String tubeBarcode = "TEST";
        barcodes.add(tubeBarcode);
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        Mockito.when(labVesselDao.findByBarcodes(barcodes)).thenReturn(new HashMap<String, LabVessel>() {{
            put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
        }});

        Product product = new Product();
        ResearchProject researchProject = new ResearchProject();
        researchProject.setTitle("TEST");
        product.setAnalysisTypeKey(productKey);
        ProductOrder productOrder = new ProductOrder(projectTitle,null,null);
        productOrder.setProduct(product);
        ResearchProjectIRB researchProjectIRB = new ResearchProjectIRB(researchProject,
                ResearchProjectIRB.IrbType.BROAD, irbNumber);
        researchProject.addIrbNumber(researchProjectIRB);
        productOrder.setResearchProject(researchProject);
        Mockito.when(productOrderDao.findByTitle(projectTitle)).thenReturn(productOrder);

        return new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService, reagentDesignDao, labVesselDao,
                mercurySampleDao, sampleInstanceEntityDao, productOrderDao, sampleKitRequestDao, sampleDataFetcher);
    }

    /**
     * Database Free tests for Pooled Tubes.
     */
    @Test
    public void testPooledTubes() throws InvalidFormatException, IOException, ValidationException {
        // Parses the spreadSheet.
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/PooledTubesTest.xlsx");
        final VesselPooledTubesProcessor spreadSheetProcessor = new VesselPooledTubesProcessor("Sheet1");
        MessageCollection messageCollection = new MessageCollection();
        messageCollection.addErrors(PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream,
                spreadSheetProcessor));
        Assert.assertEquals(messageCollection.getErrors().size(), 0,
                StringUtils.join(messageCollection.getErrors(), " ;; "));

        // Defines the mocks.
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
        MercurySampleDao mercurySampleDao =  Mockito.mock(MercurySampleDao.class);
        ReagentDesignDao reagentDesignDao =  Mockito.mock(ReagentDesignDao.class);
        SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
        JiraService jiraService = Mockito.mock(JiraService.class);

        // Sets up the mocks' replies using the spreadsheet values.
        final List<String> barcodes = Arrays.asList(spreadSheetProcessor.getBarcodes().get(0));
        Assert.assertEquals(spreadSheetProcessor.getBarcodes().get(0), spreadSheetProcessor.getBarcodes().get(1));
        Mockito.when(labVesselDao.findByBarcodes(Collections.singletonList(barcodes.get(0))))
                .thenReturn(new HashMap<String, LabVessel>() {{
            put(barcodes.get(0), new BarcodedTube(barcodes.get(0),BarcodedTube.BarcodedTubeType.MatrixTube075));
        }});

        final MercurySample[] mercurySamples = {
                new MercurySample(spreadSheetProcessor.getBroadSampleId().get(0),MercurySample.MetadataSource.MERCURY),
                new MercurySample(spreadSheetProcessor.getBroadSampleId().get(1),MercurySample.MetadataSource.MERCURY)};
        Mockito.when(mercurySampleDao.findMapIdToMercurySample(new HashSet<>(spreadSheetProcessor.getBroadSampleId())))
                .thenReturn(new HashMap<String, MercurySample>() {{
                    put(mercurySamples[0].getSampleKey(), mercurySamples[0]);
                    put(mercurySamples[1].getSampleKey(), mercurySamples[1]);
                }});

        final MercurySample[] rootSamples = {
                new MercurySample(spreadSheetProcessor.getRootSampleId().get(0),MercurySample.MetadataSource.MERCURY),
                new MercurySample(spreadSheetProcessor.getRootSampleId().get(1),MercurySample.MetadataSource.MERCURY)};
        Mockito.when(mercurySampleDao.findMapIdToMercurySample(new HashSet<>(spreadSheetProcessor.getRootSampleId())))
                .thenReturn(new HashMap<String, MercurySample>() {{
                    put(rootSamples[0].getSampleKey(), rootSamples[0]);
                    put(rootSamples[1].getSampleKey(), rootSamples[1]);
                }});

        Mockito.when(sampleDataFetcher.fetchSampleData(new HashSet<>(spreadSheetProcessor.getBroadSampleId())))
                .thenReturn(new HashMap<String, SampleData>(){{
                    for (int i = 0; i < spreadSheetProcessor.getBroadSampleId().size(); ++i) {
                        Map<BSPSampleSearchColumn, String> map = new HashMap<>();
                        // Just have it parrot back what's in the spreadsheet, instead of using real BSP values.
                        map.put(BSPSampleSearchColumn.ROOT_SAMPLE, spreadSheetProcessor.getRootSampleId().get(i));
                        map.put(BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
                                spreadSheetProcessor.getCollaboratorSampleId().get(i));
                        map.put(BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                                spreadSheetProcessor.getCollaboratorParticipantId().get(i));
                        map.put(BSPSampleSearchColumn.PARTICIPANT_ID,
                                spreadSheetProcessor.getBroadParticipantId().get(i));
                        map.put(BSPSampleSearchColumn.GENDER, spreadSheetProcessor.getGender().get(i));
                        map.put(BSPSampleSearchColumn.SPECIES, spreadSheetProcessor.getSpecies().get(i));
                        map.put(BSPSampleSearchColumn.LSID, spreadSheetProcessor.getLsid().get(i));
                        put(spreadSheetProcessor.getBroadSampleId().get(i), new BspSampleData(map));
                    }
                }});

        String jiraIssueKey = spreadSheetProcessor.getExperiment().get(0);
        Assert.assertEquals(spreadSheetProcessor.getExperiment().get(0), spreadSheetProcessor.getExperiment().get(1));
        JiraIssue jiraIssue = new JiraIssue(jiraIssueKey, jiraService);
        List<String> conditionKeys = new ArrayList<String>() {{
            addAll(spreadSheetProcessor.getConditions().get(0));
            addAll(spreadSheetProcessor.getConditions().get(1));
        }};
        jiraIssue.setConditions(conditionKeys, conditionKeys);
        Mockito.when(jiraService.getIssueInfo(jiraIssueKey, (String[]) null)).thenReturn(jiraIssue);

        MolecularIndexingScheme[] molecularIndexingSchemes = {new MolecularIndexingScheme(),
                new MolecularIndexingScheme()};
        for (int i = 0; i < molecularIndexingSchemes.length; ++i) {
            molecularIndexingSchemes[i].setName(spreadSheetProcessor.getMolecularIndexingScheme().get(i));

            Mockito.when(molecularIndexingSchemeDao.findByName(molecularIndexingSchemes[i].getName()))
                    .thenReturn(molecularIndexingSchemes[i]);
        }

        Assert.assertEquals(spreadSheetProcessor.getBait().get(0), spreadSheetProcessor.getBait().get(1));

        ReagentDesign reagentDesign = new ReagentDesign();
        reagentDesign.setDesignName(spreadSheetProcessor.getBait().get(0));
        Mockito.when(reagentDesignDao.findByBusinessKey(reagentDesign.getDesignName())).thenReturn(reagentDesign);

        // It should find no existing libraries.
        Mockito.when(sampleInstanceEntityDao.findByName(Mockito.anyString())).thenReturn(null);

        SampleInstanceEjb sampleInstanceEjb = new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService,
                reagentDesignDao, labVesselDao, mercurySampleDao, sampleInstanceEntityDao,
                null, null, sampleDataFetcher);

        sampleInstanceEjb.verifyAndPersistPooledTubeSpreadsheet(spreadSheetProcessor, messageCollection, true);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), ". "));
        Assert.assertTrue(messageCollection.getInfos().get(0).contains(String.format(SampleInstanceEjb.SUCCESS_MESSAGE,
                spreadSheetProcessor.getSingleSampleLibraryName().size())),
                StringUtils.join(messageCollection.getInfos(), " ;; "));
    }

    @Test
    public void testExomeExpressDBFree(ExternalLibraryProcessor processor) throws IOException {
        Date runDate = new Date();
        String pdo = "PDO-TEST123";
        String lcsetSuffix = "1";
        Workflow workflow = Workflow.AGILENT_EXOME_EXPRESS;

        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1,pdo);
        for(String sampleId : processor.getSingleSampleLibraryName()) {
            ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);
            productOrder.addSample(productOrderSample);
        }
        productOrder.setJiraTicketKey(pdo);
        productOrder.setOrderStatus(ProductOrder.OrderStatus.Submitted);
        productOrder.getProduct().setWorkflow(workflow);
        expectedRouting = SystemRouter.System.MERCURY;

        Map<String, BarcodedTube> mapBarcodeToTube = createInitialRack(productOrder, "R");
        //Batch
        LabBatch workflowBatch = new LabBatch("whole Genome Batch", new HashSet<LabVessel>(mapBarcodeToTube.values()),
                LabBatch.LabBatchType.WORKFLOW);
        workflowBatch.setWorkflow(workflow);
        //Bucket
        bucketBatchAndDrain(mapBarcodeToTube, productOrder, workflowBatch, lcsetSuffix);
        //Plating
        PicoPlatingEntityBuilder picoPlatingEntityBuilder = runPicoPlatingProcess(mapBarcodeToTube,
                "P", lcsetSuffix, true);
        //Shearing
        ExomeExpressShearingEntityBuilder exomeExpressShearingEntityBuilder =
                runExomeExpressShearingProcess(picoPlatingEntityBuilder.getNormBarcodeToTubeMap(),
                        picoPlatingEntityBuilder.getNormTubeFormation(),
                        picoPlatingEntityBuilder.getNormalizationBarcode(), lcsetSuffix);
        //Library
        LibraryConstructionEntityBuilder libraryConstructionEntityBuilder = runLibraryConstructionProcess(
                exomeExpressShearingEntityBuilder.getShearingCleanupPlate(),
                exomeExpressShearingEntityBuilder.getShearCleanPlateBarcode(),
                exomeExpressShearingEntityBuilder.getShearingPlate(),
                lcsetSuffix,
                mapBarcodeToTube.size());
        //Hyb
        HybridSelectionEntityBuilder hybridSelectionEntityBuilder =
                runHybridSelectionProcess(libraryConstructionEntityBuilder.getPondRegRack(),
                        libraryConstructionEntityBuilder.getPondRegRackBarcode(),
                        libraryConstructionEntityBuilder.getPondRegTubeBarcodes(), "1");

        Map<VesselPosition, BarcodedTube> mapBarcodeToDaughterTube = new EnumMap<>(VesselPosition.class);

        QtpEntityBuilder qtpEntityBuilder = runQtpProcess(hybridSelectionEntityBuilder.getNormCatchRack(),
                hybridSelectionEntityBuilder.getNormCatchBarcodes(),
                hybridSelectionEntityBuilder.getMapBarcodeToNormCatchTubes(),
                "1");

        LabVessel denatureSource =
                qtpEntityBuilder.getDenatureRack().getContainerRole().getVesselAtPosition(VesselPosition.A01);
        LabBatch fctBatch = new LabBatch(FCT_TICKET, Collections.singleton(denatureSource), LabBatch.LabBatchType.FCT);
        HiSeq2500FlowcellEntityBuilder hiSeq2500FlowcellEntityBuilder = runHiSeq2500FlowcellProcess(
                qtpEntityBuilder.getDenatureRack(), "1", FCT_TICKET, ProductionFlowcellPath.DILUTION_TO_FLOWCELL,
                "designation", workflow);
        SimpleDateFormat dateFormat = new SimpleDateFormat(IlluminaSequencingRun.RUN_FORMAT_PATTERN);

        File runPath = File.createTempFile("tempRun" + dateFormat.format(runDate), ".txt");
        String flowcellBarcode = hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell().getCartridgeBarcode();

        String machineName = "Superman";
        SolexaRunBean runBean = new SolexaRunBean(flowcellBarcode,
                flowcellBarcode + dateFormat.format(runDate),
                runDate, machineName,
                runPath.getAbsolutePath(), null);

        IlluminaSequencingRunFactory illuminaSequencingRunFactory =
                new IlluminaSequencingRunFactory(EasyMock.createNiceMock(JiraCommentUtil.class));

        // Register run
        IlluminaSequencingRun run = illuminaSequencingRunFactory.buildDbFree(runBean,
                hiSeq2500FlowcellEntityBuilder.getIlluminaFlowcell());

        List<String> sampleNames = new ArrayList<>();
        for (SampleInstanceV2 sampleInstanceV2 : run.getSampleCartridge().getSampleInstancesV2()) {
            sampleNames.add(sampleInstanceV2.getNearestMercurySampleName());
        }
        Assert.assertTrue(sampleNames.contains("SM-AYA5T"));
    }
}

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
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.boundary.lims.SystemRouter;
import org.broadinstitute.gpinformatics.mercury.boundary.run.SolexaRunBean;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ExternalLibrarySampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
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
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
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
import java.util.Set;

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
        ExternalLibrarySampleInstanceEjb externalLibrarySampleInstanceEjb = new ExternalLibrarySampleInstanceEjb();
        String irb = "";
        String molSeq = processor.getMolecularBarcodeSequence().isEmpty() ?
                "" : processor.getMolecularBarcodeSequence().get(0);
        String projectTitle = processor.getProjectTitle().get(0);
        String productType = processor.getDataAnalysisType().get(0);
        if(!spreadsheetType.equals(ExternalLibraryUploadActionBean.EZPASS_KIOSK)) {
            irb = processor.getIrbNumber().get(0);
        }
        setExternalLibraryMocks(molSeq,projectTitle,productType, irb,externalLibrarySampleInstanceEjb);

        MessageCollection messageCollection = new MessageCollection();
        externalLibrarySampleInstanceEjb.verifyExternalLibrary(processor, messageCollection, overwrite,
                spreadsheetType);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " ; "));
    }

    /**
     * This is where we setup the initial Mocks for External Library testing.
     */
    private void setExternalLibraryMocks(String molIndex, String projectTitle, String productKey, String irbNumber,
            ExternalLibrarySampleInstanceEjb externalLibrarySampleInstanceEjb) {
        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);
        externalLibrarySampleInstanceEjb.setSampleKitRequestDao(sampleKitRequestDao);
        externalLibrarySampleInstanceEjb.setMercurySampleDao(mercurySampleDao);
        if (StringUtils.isNotBlank(molIndex)) {
            MolecularIndexDao molecularIndexDao = Mockito.mock(MolecularIndexDao.class);
            MolecularIndex molecularIndex = new MolecularIndex(molIndex);
            MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme();
            molecularIndexingScheme.setName("TEST");
            Set<MolecularIndexingScheme> molecularIndexingSchemes =
                    new HashSet<>(Arrays.asList(molecularIndexingScheme));
            molecularIndex.setMolecularIndexingSchemes(molecularIndexingSchemes);

            Mockito.when(molecularIndexDao.findBySequence(molIndex)).thenReturn(molecularIndex);
            externalLibrarySampleInstanceEjb.setMolecularIndexDao(molecularIndexDao);
        }
        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        ProductOrder productOrder = new ProductOrder(projectTitle,null,null);

        List<String> barcodes = new ArrayList<>();
        final String tubeBarcode = "TEST";
        barcodes.add(tubeBarcode);
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        Mockito.when(labVesselDao.findByBarcodes(barcodes)).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});
        externalLibrarySampleInstanceEjb.setLabVesselDao(labVesselDao);

        Product product = new Product();
        ResearchProject researchProject = new ResearchProject();
        researchProject.setTitle("TEST");
        product.setAnalysisTypeKey(productKey);
        productOrder.setProduct(product);
        ResearchProjectIRB researchProjectIRB = new ResearchProjectIRB(researchProject,
                ResearchProjectIRB.IrbType.BROAD, irbNumber);
        researchProject.addIrbNumber(researchProjectIRB);
        productOrder.setResearchProject(researchProject);
        Mockito.when(productOrderDao.findByTitle(projectTitle)).thenReturn(productOrder);
        externalLibrarySampleInstanceEjb.setProductOrderDao(productOrderDao);
        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        externalLibrarySampleInstanceEjb.setSampleInstanceEntityDao(sampleInstanceEntityDao);
    }

    /**
     * Database Free tests for Pooled Tubes.
     */
    @Test
    public void testPooledTubes() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/PooledTubesTest.xlsx");
        VesselPooledTubesProcessor spreadSheetProcessor = new VesselPooledTubesProcessor("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, spreadSheetProcessor);
        MessageCollection messageCollection = new MessageCollection();
        ExternalLibrarySampleInstanceEjb actionBean = new ExternalLibrarySampleInstanceEjb();
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
        MercurySampleDao mercurySampleDao =  Mockito.mock(MercurySampleDao.class);
        ReagentDesignDao reagentDesignDao =  Mockito.mock(ReagentDesignDao.class);
        JiraService jiraService = Mockito.mock(JiraService.class);

        final String tubeBarcode = spreadSheetProcessor.getBarcodes().get(0);
        List<String> tubes = new ArrayList<>();
        List<MercurySample> mercurySamples = new ArrayList<>();
        tubes.add(tubeBarcode);
        tubes.add(tubeBarcode);
        String molIndex_1 = spreadSheetProcessor.getMolecularIndexingScheme().get(0);
        String molIndex_2 = spreadSheetProcessor.getMolecularIndexingScheme().get(1);
        String jiraIssueKey =  spreadSheetProcessor.getExperiment().get(0);
        String sampleId_1 =  spreadSheetProcessor.getBroadSampleId().get(0);
        String sampleId_2 = spreadSheetProcessor.getBroadSampleId().get(1);
        String reagent = spreadSheetProcessor.getBait().get(0);

        JiraIssue jiraIssue = new JiraIssue(jiraIssueKey, jiraService);
        MolecularIndexingScheme molecularIndexingScheme_1 = new MolecularIndexingScheme();
        molecularIndexingScheme_1.setName(molIndex_1);
        MolecularIndexingScheme molecularIndexingScheme_2 = new MolecularIndexingScheme();
        molecularIndexingScheme_1.setName(molIndex_2);
        ReagentDesign reagentDesign = new ReagentDesign();
        reagentDesign.setDesignName(reagent);
        MercurySample mercurySample_1 = new MercurySample(sampleId_1,MercurySample.MetadataSource.MERCURY);
        MercurySample mercurySample_2 = new MercurySample(sampleId_2,MercurySample.MetadataSource.MERCURY);
        mercurySamples.add(mercurySample_1);
        mercurySamples.add(mercurySample_2);
        actionBean.setRootSamples(mercurySamples);

        Mockito.when(labVesselDao.findByBarcodes(tubes)).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});
        Mockito.when(molecularIndexingSchemeDao.findByName(molIndex_1)).thenReturn(molecularIndexingScheme_1);
        Mockito.when(molecularIndexingSchemeDao.findByName(molIndex_2)).thenReturn(molecularIndexingScheme_2);
        Mockito.when(reagentDesignDao.findByBusinessKey(reagent)).thenReturn(reagentDesign);
        Mockito.when(jiraService.getIssueInfo(jiraIssueKey)).thenReturn(jiraIssue);
        Mockito.when(mercurySampleDao.findBySampleKey(sampleId_1)).thenReturn(mercurySample_1);
        Mockito.when(mercurySampleDao.findBySampleKey(sampleId_2)).thenReturn(mercurySample_2);

        actionBean.setLabVesselDao(labVesselDao);
        actionBean.setSampleInstanceEntityDao(sampleInstanceEntityDao);
        actionBean.setMolecularIndexingSchemeDao(molecularIndexingSchemeDao);
        actionBean.setMercurySampleDao(mercurySampleDao);
        actionBean.setReagentDesignDao(reagentDesignDao);
        actionBean.setJiraService(jiraService);
        actionBean.verifyPooledTubes(spreadSheetProcessor, messageCollection, true);

        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), ". "));
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

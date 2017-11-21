package org.broadinstitute.gpinformatics.mercury.boundary.sample;

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
import org.broadinstitute.gpinformatics.mercury.control.dao.analysis.ReferenceSequenceDao;
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
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.analysis.ReferenceSequence;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaSequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceEntity;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.WalkUpSequencing;
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
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.mercury.test.LabEventTest.FCT_TICKET;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleInstanceEjbDbFreeTest extends BaseEventTest {

    @Test
    public void testWalkupSequencing() {
        WalkUpSequencing walkUpSequencing = new WalkUpSequencing();
        walkUpSequencing.setLibraryName("TEST_LIBRARY");
        walkUpSequencing.setTubeBarcode("TEST_TUBE");
        walkUpSequencing.setReadType("TEST_READ_TYPE");
        walkUpSequencing.setAnalysisType("TEST");
        walkUpSequencing.setEmailAddress("TEST@TEST.COM");
        walkUpSequencing.setLabName("TEST lab");
        walkUpSequencing.setBaitSetName("TEST_BAIT");
        walkUpSequencing.setReadType("Paried End");
        SampleInstanceEjb sampleInstanceEjb = setWalkupSequencingMocks(walkUpSequencing);
        MessageCollection messages = new MessageCollection();
        sampleInstanceEjb.verifyAndPersistSubmission(walkUpSequencing, messages);

        Assert.assertFalse(messages.hasErrors(), StringUtils.join(messages.getErrors(), " ; "));
    }

    private SampleInstanceEjb setWalkupSequencingMocks(WalkUpSequencing walkUpSequencing) {
        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);

        ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
        ReagentDesign reagentDesign = new ReagentDesign();

        List<String> barcodes = new ArrayList<>();
        final String tubeBarcode = walkUpSequencing.getTubeBarcode();
        barcodes.add(tubeBarcode);
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        Mockito.when(labVesselDao.findByBarcodes(barcodes)).thenReturn(
                new HashMap<String, LabVessel>() {{
                    put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});

        reagentDesign.setDesignName(walkUpSequencing.getBaitSetName());
        Mockito.when(reagentDesignDao.findByBusinessKey(walkUpSequencing.getBaitSetName())).thenReturn(reagentDesign);

        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);

        MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
        JiraService jiraService = Mockito.mock(JiraService.class);
        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
        ReferenceSequenceDao referenceSequenceDao = Mockito.mock(ReferenceSequenceDao.class);

        return new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService, reagentDesignDao, labVesselDao,
                mercurySampleDao, sampleInstanceEntityDao, productOrderDao, sampleKitRequestDao,
                sampleDataFetcher, referenceSequenceDao);
    }

    @Test
    public void parseExternalLibarayEZFail() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryEZFailTest.xlsx");
        ExternalLibraryProcessorEzPass processor = new ExternalLibraryProcessorEzPass("Sheet1");
        try {
            PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
            Assert.fail("Should have thrown exception.");
        } catch (ValidationException e) {
            int count = 0;
            for (String msg : e.getValidationMessages()) {
                if (msg.startsWith("Duplicate header: \"Virtual GSSR ID\"") ||
                        msg.startsWith("Duplicate header: \"SQUID Project\"") ||
                        msg.startsWith("Unknown header: \"Sample No.\"")) {
                    ++count;
                }
            }
            Assert.assertEquals(count, 3);
        }
    }

    @Test
    public void testExternalLibraryEZPass() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryEZPassTest.xlsx");
        ExternalLibraryProcessorEzPass processor = new ExternalLibraryProcessorEzPass("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        List<SampleInstanceEntity> sampleInstanceEntities = runVerifyAndPersist(processor, true);
        Assert.assertEquals(sampleInstanceEntities.size(), 1);
        Assert.assertTrue(sampleInstanceEntities.get(0).getPooled());
        runExomeExpressDBFree(processor);
    }

    @Test
    public void testExternalLibraryMultiOrganism() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryMultiOrganismTest.xlsx");
        ExternalLibraryProcessorPooledMultiOrganism processor =
                new ExternalLibraryProcessorPooledMultiOrganism("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        List<SampleInstanceEntity> entities = runVerifyAndPersist(processor, true);
        Assert.assertEquals(entities.size(), 3);

        for (int i = 0; i < entities.size(); ++i) {
            String libraryName = Arrays.asList("4076255991TEST", "4076255992TEST", "4076255993TEST").get(i);
            Assert.assertEquals(entities.get(i).getSampleLibraryName(), libraryName);
            MercurySample mercurySample = entities.get(i).getMercurySample();
            Assert.assertNotNull(mercurySample, libraryName);
            Assert.assertEquals(mercurySample.getSampleKey(), libraryName);

            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            boolean found = false;
            for (Metadata metadata : mercurySample.getMetadata()) {
                if (metadata.getKey() == Metadata.Key.SAMPLE_ID) {
                    Assert.assertEquals(metadata.getStringValue(), "SampleColab2" + i);
                    found = true;
                }
            }
            Assert.assertTrue(found, libraryName);

            LabVessel tube = entities.get(i).getLabVessel();
            Assert.assertEquals(tube.getLabel(), libraryName);
            Assert.assertEquals(tube.getVolume(),
                    Arrays.asList(new BigDecimal("443.00"), new BigDecimal("444.00"), new BigDecimal("445.00")).get(i));
            Assert.assertEquals(tube.getConcentration(),
                    Arrays.asList(new BigDecimal("67.00"), new BigDecimal("68.00"), new BigDecimal("69.00")).get(i));
            Assert.assertEquals(tube.getMetrics().size(), 1);
            LabMetric labMetric = tube.getMetrics().iterator().next();
            Assert.assertEquals(labMetric.getName(), LabMetric.MetricType.FINAL_LIBRARY_SIZE);
            Assert.assertEquals(labMetric.getValue(), new BigDecimal("626.00"));
            Assert.assertTrue(tube.getMercurySamples().contains(mercurySample), libraryName);

            SampleKitRequest sampleKitRequest = entities.get(i).getSampleKitRequest();
            Assert.assertEquals(sampleKitRequest.getCollaboratorName(), "Charlie Delta");
            Assert.assertEquals(sampleKitRequest.getOrganization(), "Epsilon");
            Assert.assertEquals(sampleKitRequest.getAddress(), "Fox Trot");
            Assert.assertEquals(sampleKitRequest.getCity(), "Cambridge");
            Assert.assertEquals(sampleKitRequest.getState(), "MA");
            Assert.assertEquals(sampleKitRequest.getPostalCode(), "02142");
            Assert.assertEquals(sampleKitRequest.getCountry(), "USA");
            Assert.assertEquals(sampleKitRequest.getPhone(), "617-423-1234");
            Assert.assertEquals(sampleKitRequest.getEmail(), "none@none.com");
            Assert.assertEquals(sampleKitRequest.getCommonName(), "(leave blank for multi-organism submission)");
            Assert.assertEquals(sampleKitRequest.getGenus(), "see Organism column for genus");
            Assert.assertEquals(sampleKitRequest.getSpecies(), "see Organism column for species");
            Assert.assertEquals(sampleKitRequest.getIrbApprovalRequired(), "Y");

            Assert.assertEquals(entities.get(i).getLibraryType(), "WholeGenomeShotgun");
            Assert.assertEquals(entities.get(i).getProductOrder().getTitle(), "Microsporidia_RNASeq_Sanscrainte");
        }
        Assert.assertNull(entities.get(0).getMolecularIndexingScheme());
        Assert.assertNull(entities.get(1).getMolecularIndexingScheme());
        Assert.assertEquals(entities.get(2).getMolecularIndexingScheme().getName(), "Illumina_P5-Lanah_P7-Cehih");
    }

    @Test
    public void testExternalLibraryPooled() throws InvalidFormatException, IOException, ValidationException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryPooledTest.xlsx");
        ExternalLibraryProcessorPooled processor = new ExternalLibraryProcessorPooled("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(inputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        List<SampleInstanceEntity> entities = runVerifyAndPersist(processor, true);
        Assert.assertEquals(entities.size(), 2);
        Assert.assertTrue(entities.get(0).getPooled());
        Assert.assertTrue(entities.get(1).getPooled());

        for (int i = 0; i < 2; ++i) {
            MercurySample mercurySample = entities.get(i).getMercurySample();
            Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
            int count = 0;
            for (Metadata metadata : mercurySample.getMetadata()) {
                if (metadata.getKey() == Metadata.Key.SAMPLE_ID) {
                    Assert.assertEquals(metadata.getStringValue(), i == 0 ? "DDDSS2244" : "DDDSS2245");
                    ++count;
                }
                if (metadata.getKey() == Metadata.Key.BROAD_PARTICIPANT_ID) {
                    Assert.assertEquals(metadata.getStringValue(), i == 0 ? "Patient X" : "Patient Y");
                    ++count;
                }
            }
            Assert.assertEquals(count, 2, "at " + i);
        }
    }

    @Test
    public void testExternalLibraryNonPooled() throws InvalidFormatException, IOException, ValidationException {
        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryNONPooledTest.xlsx");
        ExternalLibraryProcessorNonPooled processor = new ExternalLibraryProcessorNonPooled("Sheet1");
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, processor);
        Assert.assertEquals(processor.getMessages().size(), 0, StringUtils.join(processor.getMessages()));
        List<SampleInstanceEntity> sampleInstanceEntities = runVerifyAndPersist(processor, true);
        Assert.assertEquals(sampleInstanceEntities.size(), 2);
    }

    /** Returns the list element or null if the list or element doesn't exist */
    private <T> T get(List<T> list, int index) {
        return (org.apache.commons.collections4.CollectionUtils.isNotEmpty(list) && list.size() > index) ? list.get(index) : null;
    }

    private List<SampleInstanceEntity> runVerifyAndPersist(ExternalLibraryProcessor processor, boolean overwrite) {
        SampleInstanceEjb sampleInstanceEjb = setExternalLibraryMocks(processor);

        MessageCollection messageCollection = new MessageCollection();
        List<SampleInstanceEntity> sampleInstanceEntities = sampleInstanceEjb.verifyAndPersistSpreadsheet(
                processor, messageCollection, overwrite);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), " ; "));
        return sampleInstanceEntities;
    }

    private SampleInstanceEjb setExternalLibraryMocks(ExternalLibraryProcessor processor) {

        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);
        MolecularIndexingSchemeDao molecularIndexingSchemeDao = Mockito.mock(MolecularIndexingSchemeDao.class);
        ProductOrderDao productOrderDao = Mockito.mock(ProductOrderDao.class);
        SampleInstanceEntityDao sampleInstanceEntityDao = Mockito.mock(SampleInstanceEntityDao.class);
        JiraService jiraService = new JiraServiceStub();
        ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
        SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
        ReferenceSequenceDao referenceSequenceDao = Mockito.mock(ReferenceSequenceDao.class);

        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme();
        molecularIndexingScheme.setName("Illumina_P5-Lanah_P7-Cehih");
        Mockito.when(molecularIndexingSchemeDao.findByName(Mockito.startsWith("Illumina_P5-")))
                .thenReturn(molecularIndexingScheme);
        Mockito.when(molecularIndexingSchemeDao.findByName("")).thenReturn(null);

        List<String> barcodes = new ArrayList<>();
        final String tubeBarcode = "TEST";
        barcodes.add(tubeBarcode);
        LabVesselDao labVesselDao = Mockito.mock(LabVesselDao.class);
        Mockito.when(labVesselDao.findByBarcodes(barcodes)).thenReturn(new HashMap<String, LabVessel>() {{
            put(tubeBarcode, new BarcodedTube(tubeBarcode, BarcodedTube.BarcodedTubeType.MatrixTube075));
        }});

        String projectTitle = get(processor.getProjectTitle(), 0);
        Product product = new Product();
        ResearchProject researchProject = new ResearchProject();
        researchProject.setTitle("TEST");
        product.setAnalysisTypeKey(get(processor.getDataAnalysisType(), 0));
        ProductOrder productOrder = new ProductOrder(projectTitle, null, null);
        productOrder.setProduct(product);
        ResearchProjectIRB researchProjectIRB = new ResearchProjectIRB(researchProject,
                ResearchProjectIRB.IrbType.BROAD, get(processor.getIrbNumber(), 0));
        researchProject.addIrbNumber(researchProjectIRB);
        productOrder.setResearchProject(researchProject);
        Mockito.when(productOrderDao.findByTitle(projectTitle)).thenReturn(productOrder);

        Mockito.when(referenceSequenceDao.findCurrent(Mockito.anyString()))
                .thenReturn(new ReferenceSequence(get(processor.getReferenceSequence(), 0), ""));

        return new SampleInstanceEjb(molecularIndexingSchemeDao, jiraService, reagentDesignDao, labVesselDao,
                mercurySampleDao, sampleInstanceEntityDao, productOrderDao, sampleKitRequestDao, sampleDataFetcher,
                referenceSequenceDao);
    }

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
        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        ReagentDesignDao reagentDesignDao = Mockito.mock(ReagentDesignDao.class);
        SampleDataFetcher sampleDataFetcher = Mockito.mock(SampleDataFetcher.class);
        JiraService jiraService = Mockito.mock(JiraService.class);
        ReferenceSequenceDao referenceSequenceDao = Mockito.mock(ReferenceSequenceDao.class);

        // Sets up the mocks' replies using the spreadsheet values.
        final List<String> barcodes = Arrays.asList(spreadSheetProcessor.getBarcodes().get(0));
        Assert.assertEquals(spreadSheetProcessor.getBarcodes().get(0), spreadSheetProcessor.getBarcodes().get(1));
        Mockito.when(labVesselDao.findByBarcodes(Collections.singletonList(barcodes.get(0))))
                .thenReturn(new HashMap<String, LabVessel>() {{
                    put(barcodes.get(0),
                            new BarcodedTube(barcodes.get(0), BarcodedTube.BarcodedTubeType.MatrixTube075));
                }});

        final MercurySample[] mercurySamples = {
                new MercurySample(spreadSheetProcessor.getBroadSampleId().get(0), MercurySample.MetadataSource.MERCURY),
                new MercurySample(spreadSheetProcessor.getBroadSampleId().get(1),
                        MercurySample.MetadataSource.MERCURY)};
        Mockito.when(mercurySampleDao.findMapIdToMercurySample(new HashSet<>(spreadSheetProcessor.getBroadSampleId())))
                .thenReturn(new HashMap<String, MercurySample>() {{
                    put(mercurySamples[0].getSampleKey(), mercurySamples[0]);
                    put(mercurySamples[1].getSampleKey(), mercurySamples[1]);
                }});

        final MercurySample[] rootSamples = {
                new MercurySample(spreadSheetProcessor.getRootSampleId().get(0), MercurySample.MetadataSource.MERCURY),
                new MercurySample(spreadSheetProcessor.getRootSampleId().get(1), MercurySample.MetadataSource.MERCURY)};
        Mockito.when(mercurySampleDao.findMapIdToMercurySample(new HashSet<>(spreadSheetProcessor.getRootSampleId())))
                .thenReturn(new HashMap<String, MercurySample>() {{
                    put(rootSamples[0].getSampleKey(), rootSamples[0]);
                    put(rootSamples[1].getSampleKey(), rootSamples[1]);
                }});

        Mockito.when(sampleDataFetcher.fetchSampleData(new HashSet<>(spreadSheetProcessor.getBroadSampleId())))
                .thenReturn(new HashMap<String, SampleData>() {{
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
                null, null, sampleDataFetcher, referenceSequenceDao);
        List<SampleInstanceEntity> entities = sampleInstanceEjb.verifyAndPersistSpreadsheet(
                spreadSheetProcessor, messageCollection, true);
        Assert.assertFalse(messageCollection.hasErrors(), StringUtils.join(messageCollection.getErrors(), ". "));
        Assert.assertTrue(messageCollection.getInfos().get(0).contains(String.format(SampleInstanceEjb.IS_SUCCESS,
                spreadSheetProcessor.getSingleSampleLibraryName().size())),
                StringUtils.join(messageCollection.getInfos(), " ;; "));
        Assert.assertEquals(entities.size(), 2);
        Assert.assertEquals(entities.get(0).getSampleLibraryName(), "Jon Test 3a");
        Assert.assertEquals(entities.get(1).getSampleLibraryName(), "Jon Test 4b");
        Assert.assertEquals(entities.get(0).getLabVessel().getLabel(), "JT041431");
        Assert.assertEquals(entities.get(1).getLabVessel().getLabel(), "JT041431");
        Assert.assertEquals(entities.get(0).getLabVessel().getVolume(), new BigDecimal("0.60"));
        Assert.assertEquals(entities.get(1).getLabVessel().getVolume(), new BigDecimal("0.60"));
        Assert.assertEquals(entities.get(0).getLabVessel().getMetrics().iterator().next().getName(),
                LabMetric.MetricType.FINAL_LIBRARY_SIZE);
        Assert.assertEquals(entities.get(0).getLabVessel().getMetrics().iterator().next().getValue(),
                new BigDecimal("2.00"));
        Assert.assertEquals(entities.get(0).getMercurySample().getSampleKey(), "SM-JT12");
        Assert.assertEquals(entities.get(1).getMercurySample().getSampleKey(), "SM-JT23");
        Assert.assertEquals(entities.get(0).getRootSample().getSampleKey(), "SM-46IRUT1");
        Assert.assertEquals(entities.get(1).getRootSample().getSampleKey(), "SM-46IRUT2");
        Assert.assertEquals(entities.get(0).getMolecularIndexingScheme().getName(), "Illumina_P5-Nijow_P7-Waren");
        Assert.assertEquals(entities.get(1).getMolecularIndexingScheme().getName(), "Illumina_P5-Piwan_P7-Bidih");
        Assert.assertEquals(entities.get(0).getReagentDesign().getDesignName(),
                "NewtonCheh_NatPepMDC_12genes3regions_Sep2011");
        Assert.assertEquals(entities.get(1).getReagentDesign().getDesignName(),
                "NewtonCheh_NatPepMDC_12genes3regions_Sep2011");
        Assert.assertEquals(entities.get(0).getExperiment(), "DEV-7501");
        Assert.assertEquals(entities.get(1).getExperiment(), "DEV-7501");
    }

    private void runExomeExpressDBFree(ExternalLibraryProcessor processor) throws IOException {
        Date runDate = new Date();
        String pdo = "PDO-TEST123";
        String lcsetSuffix = "1";
        Workflow workflow = Workflow.AGILENT_EXOME_EXPRESS;

        ProductOrder productOrder = ProductOrderTestFactory.createDummyProductOrder(1, pdo);
        for (String sampleId : processor.getSingleSampleLibraryName()) {
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
        Assert.assertTrue(sampleNames.contains(processor.getSingleSampleLibraryName().get(0)));
    }
}

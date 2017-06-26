package org.broadinstitute.gpinformatics.mercury.samples;

import com.google.inject.Inject;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ExternalLibrarySampleInstanceEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.MolecularIndexingSchemeDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.reagent.ReagentDesignDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleInstanceEntityDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.SampleKitRequestDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryMapped;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorEzPass;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorNonPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooled;
import org.broadinstitute.gpinformatics.mercury.control.sample.ExternalLibraryProcessorPooledMultiOrganism;
import org.broadinstitute.gpinformatics.mercury.control.vessel.VesselPooledTubesProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.ReagentDesign;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.sample.ExternalLibraryUploadActionBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Test(groups = TestGroups.DATABASE_FREE)
public class ExternalSampleLibrariesTest {

    @Inject
    ExternalLibraryUploadActionBean externalLibraryUploadActionBean;


    /**
     * Database Free tests for EZPass Kiosk library.
     */
    public void testEZPassExternalLibraries() throws InvalidFormatException, IOException, ValidationException {

        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryEZPassTest.xlsx");

        ExternalLibraryProcessorEzPass spreadSheetProcessor = new ExternalLibraryProcessorEzPass("Sheet1");
        spreadSheetProcessor.setHeaderRowIndex(externalLibraryUploadActionBean.ezPassRowOffset);
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, spreadSheetProcessor);
        ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
        externalLibraryMapped.mapEzPass(spreadSheetProcessor);
        setParams(externalLibraryMapped,"EZPass upload", true, externalLibraryUploadActionBean.EZPASS_KIOSK);
    }



    /**
     * Database Free tests for Multi Organism External Libraries
     */
    public void testExternalMultiOrganismLibrary() throws InvalidFormatException, IOException, ValidationException {

        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryMultiOrganismTest.xlsx");

        ExternalLibraryProcessorPooledMultiOrganism spreadSheetProcessor = new ExternalLibraryProcessorPooledMultiOrganism("Sheet1");
        spreadSheetProcessor.setHeaderRowIndex(externalLibraryUploadActionBean.externalLibraryRowOffset);
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, spreadSheetProcessor);
        ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
        externalLibraryMapped.mapPooledMultiOrg(spreadSheetProcessor);
        setParams(externalLibraryMapped,"Multi Organism uploads", true, externalLibraryUploadActionBean.MULTI_ORG);
    }

    /**
     * Database Free tests for Pooled External Libraries
     */
    public void testExternalPooledLibrary() throws InvalidFormatException, IOException, ValidationException {

        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryPooledTest.xlsx");

        ExternalLibraryProcessorPooled spreadSheetProcessor = new ExternalLibraryProcessorPooled("Sheet1");
        spreadSheetProcessor.setHeaderRowIndex(externalLibraryUploadActionBean.externalLibraryRowOffset);
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, spreadSheetProcessor);
        ExternalLibraryMapped externalLibraryMapped = new ExternalLibraryMapped();
        externalLibraryMapped.mapPooled(spreadSheetProcessor);
        setParams(externalLibraryMapped,"pooled uploads", true, externalLibraryUploadActionBean.POOLED);
    }


    /**
     * Database Free tests for NON Pooled External Libraries
     */
    public void testExternalNonPooledLibrary() throws InvalidFormatException, IOException, ValidationException {

        InputStream testSpreadSheetInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(
                "testdata/ExternalLibraryNONPooledTest.xlsx");

        ExternalLibraryProcessorNonPooled spreadSheetProcessor = new ExternalLibraryProcessorNonPooled("Sheet1");
        spreadSheetProcessor.setHeaderRowIndex(externalLibraryUploadActionBean.externalLibraryRowOffset);
        PoiSpreadsheetParser.processSingleWorksheet(testSpreadSheetInputStream, spreadSheetProcessor);
        ExternalLibraryMapped externalLibraryMapped  = new ExternalLibraryMapped();
        externalLibraryMapped.mapNonPooled(spreadSheetProcessor);
        setParams(externalLibraryMapped,"non-pooled uploads", true, externalLibraryUploadActionBean.NON_POOLED);
    }

    /**
     *  Run the test against the Ejb and check the results.
     */
    public void setParams(ExternalLibraryMapped externalLibraryMapped, String title, boolean overwrite, String spreadsheetType) {

        ExternalLibrarySampleInstanceEjb externalLibrarySampleInstanceEjb = new ExternalLibrarySampleInstanceEjb();
        MessageCollection messageCollection = new MessageCollection();
        String irb = "";
        String molSeq = externalLibraryMapped.getMolecularBarcodeSequence().get(0);
        String projectTitle = externalLibraryMapped.getProjectTitle().get(0);
        String productType = externalLibraryMapped.getDataAnalysisType().get(0);
        if(!spreadsheetType.equals(externalLibraryUploadActionBean.EZPASS_KIOSK)) {
            irb = externalLibraryMapped.getIrbNumber().get(0);
        }
        setExternalLibraryMocks(molSeq,projectTitle,productType, irb,externalLibrarySampleInstanceEjb);
        externalLibrarySampleInstanceEjb.verifyExternalLibrary(externalLibraryMapped, messageCollection, overwrite, spreadsheetType);
        Assert.assertFalse(messageCollection.hasErrors(), "Unable to parse and verify External Library " + title);

    }

    /**
     * This is where we setup the initial Mocks for External Library testing.
     */
    public void setExternalLibraryMocks(String molIndex, String projectTitle, String productKey, String irbNumber,
                                        ExternalLibrarySampleInstanceEjb externalLibrarySampleInstanceEjb)
    {

        MercurySampleDao mercurySampleDao = Mockito.mock(MercurySampleDao.class);
        MolecularIndexDao molecularIndexDao = Mockito.mock(MolecularIndexDao.class);
        SampleKitRequestDao sampleKitRequestDao = Mockito.mock(SampleKitRequestDao.class);

        externalLibrarySampleInstanceEjb.setSampleKitRequestDao(sampleKitRequestDao);
        externalLibrarySampleInstanceEjb.setMercurySampleDao(mercurySampleDao);

        MolecularIndex molecularIndex = new MolecularIndex(molIndex);

        MolecularIndexingScheme molecularIndexingScheme = new MolecularIndexingScheme();
        molecularIndexingScheme.setName("TEST");
        Set<MolecularIndexingScheme> molecularIndexingSchemes= new HashSet<>(Arrays.asList(molecularIndexingScheme));
        molecularIndex.setMolecularIndexingSchemes(molecularIndexingSchemes);

        Mockito.when(molecularIndexDao.findBySequence(molIndex)).thenReturn(molecularIndex);
        externalLibrarySampleInstanceEjb.setMolecularIndexDao(molecularIndexDao);

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
        ResearchProjectIRB researchProjectIRB = new ResearchProjectIRB(researchProject, ResearchProjectIRB.IrbType.BROAD,irbNumber);
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


        final String tubeBarcode = "JT041431";
        List<String> tubes = new ArrayList<>();
        List<MercurySample> mercurySamples = new ArrayList<>();
        tubes.add("JT041431");
        tubes.add("JT041431");
        String molIndex_1 = "Illumina_P5-Nijow_P7-Waren";
        String molIndex_2 = "Illumina_P5-Piwan_P7-Bidih";
        String jiraIssueKey = "DEV-7501";
        String sampleId_1 = "SM-JT12";
        String sampleId_2 = "SM-JT23";
        String reagent = "NewtonCheh_NatPepMDC_12genes3regions_Sep2011";

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
        Mockito.when(jiraService.getIssueInfo(jiraIssueKey,null)).thenReturn(jiraIssue);
        Mockito.when(mercurySampleDao.findBySampleKey(sampleId_1)).thenReturn(mercurySample_1);
        Mockito.when(mercurySampleDao.findBySampleKey(sampleId_2)).thenReturn(mercurySample_2);

        actionBean.setLabVesselDao(labVesselDao);
        actionBean.setSampleInstanceEntityDao(sampleInstanceEntityDao);
        actionBean.setMolecularIndexingSchemeDao(molecularIndexingSchemeDao);
        actionBean.setMercurySampleDao(mercurySampleDao);
        actionBean.setReagentDesignDao(reagentDesignDao);
        actionBean.setJiraService(jiraService);
        actionBean.verifyPooledTubes(spreadSheetProcessor, messageCollection, true);

        Assert.assertFalse(messageCollection.hasErrors(), "Unable to parse and verify Pooled Tube uploads.");

    }

}

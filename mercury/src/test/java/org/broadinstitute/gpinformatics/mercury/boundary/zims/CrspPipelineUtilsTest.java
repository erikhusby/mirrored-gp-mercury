package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.infrastructure.test.dbfree.ProductOrderTestFactory;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Test(groups = TestGroups.DATABASE_FREE)
public class CrspPipelineUtilsTest {

    private static final String BUICK_COLLECTION_DATE = "some date as a string";

    private static final String BUICK_VISIT = "Some visit info";

    private ProductOrderSample crspSample;

    private ProductOrderSample nonCrspSampleWithGSSRMetadata;

    private ProductOrderSample nonCrspSampleWithUnknownMetadata;

    private ProductOrderSample nonCrspSampleWithBSPMetadata;

    private CrspPipelineUtils crspPipelineAPIUtils = new CrspPipelineUtils(Deployment.DEV);

    private SampleData sampleDataWithNonBspSample = new MercurySampleData("Not from BSP", Collections.<Metadata>emptySet());

    private ProductOrderSample createPdoSample(MercurySample.MetadataSource metadataSource, String sampleName) {
        ProductOrder pdo = ProductOrderTestFactory.buildProductOrder(0, "SM-", Workflow.ICE_CRSP);
        ProductOrderSample pdoSample = new ProductOrderSample(sampleName);
        MercurySample mercurySample = new MercurySample(pdoSample.getSampleKey(), metadataSource);

        Set<Metadata> metadata = new HashSet<>();
        metadata.add(new Metadata(Metadata.Key.BUICK_COLLECTION_DATE, BUICK_COLLECTION_DATE));
        metadata.add(new Metadata(Metadata.Key.BUICK_VISIT, BUICK_VISIT));
        MercurySampleData mercurySampleData = new MercurySampleData(pdoSample.getSampleKey(), metadata);

        pdoSample.setMercurySample(mercurySample);
        pdoSample.setSampleData(mercurySampleData);
        pdo.addSample(pdoSample);
        return pdoSample;
    }

    private void setRegulatoryDesignation(ProductOrderSample pdoSample,ResearchProject.RegulatoryDesignation regulatoryDesignation) {
        pdoSample.getProductOrder().getResearchProject().setRegulatoryDesignation(regulatoryDesignation);
    }

    @BeforeMethod
    public void setUp() {
        crspSample = createPdoSample(MercurySample.MetadataSource.MERCURY,"sample1");
        setRegulatoryDesignation(crspSample, ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);
        nonCrspSampleWithGSSRMetadata = createPdoSample(null,"123.5");
        setRegulatoryDesignation(nonCrspSampleWithGSSRMetadata, ResearchProject.RegulatoryDesignation.RESEARCH_ONLY);
        nonCrspSampleWithUnknownMetadata = createPdoSample(null,"Crazy Sample!");
        nonCrspSampleWithBSPMetadata = createPdoSample(MercurySample.MetadataSource.BSP,"SM-WHATEVER");
    }

    private Set<SampleInstanceV2> createSampleInstances(ProductOrderSample...pdoSamples) {
        Set<SampleInstanceV2> sampleInstances = new HashSet<>();
        for (ProductOrderSample pdoSample : pdoSamples) {
            sampleInstances.add(new TestSampleInstance(pdoSample).getSampleInstance());
        }
        return sampleInstances;
    }

    public void testGSSRMetadatasource() {
        Assert.assertEquals(MercurySample.GSSR_METADATA_SOURCE,
                            nonCrspSampleWithGSSRMetadata.getMercurySample().getMetadataSourceForPipelineAPI());
    }

    public void testOtherMetadatasource() {
        Assert.assertEquals(MercurySample.OTHER_METADATA_SOURCE,
                            nonCrspSampleWithUnknownMetadata.getMercurySample().getMetadataSourceForPipelineAPI());
    }

    public void testBSPMetadatasource() {
        Assert.assertEquals(MercurySample.BSP_METADATA_SOURCE,
                            nonCrspSampleWithBSPMetadata.getMercurySample().getMetadataSourceForPipelineAPI());
    }

    public void testMercuryMetadatasource() {
        Assert.assertEquals(MercurySample.MERCURY_METADATA_SOURCE,
                            crspSample.getMercurySample().getMetadataSourceForPipelineAPI());
    }

    public void testAllSamplesAreForCrsp() {
        boolean hasAllCrspSamples = crspPipelineAPIUtils.areAllSamplesForCrsp(createSampleInstances(
                crspSample), false);

        Assert.assertTrue(hasAllCrspSamples);
    }

    public void testNoSamplesAreForCrsp() {
        boolean hasAllCrspSamples = crspPipelineAPIUtils.areAllSamplesForCrsp(createSampleInstances(
                nonCrspSampleWithGSSRMetadata), false);

        Assert.assertFalse(hasAllCrspSamples);
    }

    public void testMixOfCrspAndNonCrspSamplesShouldThrowException() {
        Set<SampleInstanceV2> sampleInstances = createSampleInstances(nonCrspSampleWithGSSRMetadata, crspSample);
        try {
            crspPipelineAPIUtils.areAllSamplesForCrsp(sampleInstances, false);
            Assert.fail("Mixture of samples");
        }
        catch(RuntimeException e) {
            Assert.assertTrue(e.getMessage().contains("Samples contain a mix of CRSP and non-CRSP samples"));
        }
    }

    public void testGetCrspLSIDForBSPSampleId() {
        Assert.assertEquals("org.broadinstitute:crsp:5FGZM",
                            crspPipelineAPIUtils.getCrspLSIDForBSPSampleId("SM-5FGZM"));
    }

    public void testSetFieldsForCrspThrowsExceptionForNonBspSampleInProduction() {
        try {
            new CrspPipelineUtils(Deployment.PROD).setFieldsForCrsp(new LibraryBean(), sampleDataWithNonBspSample,
                    "bait");
            Assert.fail("Should have thrown an exception because " + sampleDataWithNonBspSample.getSampleId() + " is not a bsp sample");
        }
        catch(RuntimeException ignored){}
    }

    public void testSetFieldsSetsTestTypeToSomatic() {
        LibraryBean libraryBean = new LibraryBean();
        SampleData sampleData = new MercurySampleData("sampleId", Collections.<Metadata>emptySet());

        crspPipelineAPIUtils.setFieldsForCrsp(libraryBean, sampleData, "bait");

        Assert.assertEquals(libraryBean.getTestType(),"Somatic");
    }

    public void testSetFieldsForCrspDoesNotThrowExceptionForNonBspSampleInDevelopment() {
        new CrspPipelineUtils(Deployment.DEV).setFieldsForCrsp(new LibraryBean(), sampleDataWithNonBspSample, "bait");
    }

    public void testBuickCollectionAndVisitDateFields() {
        LibraryBean libraryBean = new LibraryBean();
        new CrspPipelineUtils(Deployment.DEV).setFieldsForCrsp(libraryBean, crspSample.getSampleData(), "bait");

        Assert.assertEquals(libraryBean.getBuickVisit(),BUICK_VISIT);
        Assert.assertEquals(libraryBean.getBuickCollectionDate(),BUICK_COLLECTION_DATE);
    }

    private class TestSampleInstance {

        private final ProductOrderSample pdoSample;

        public TestSampleInstance(ProductOrderSample pdoSample) {
            this.pdoSample = pdoSample;
        }

        public SampleInstanceV2 getSampleInstance() {
            LabVessel tube = new BarcodedTube("000000");
            LabBatch labBatch = new LabBatch("Test", Collections.singleton(tube), LabBatch.LabBatchType.WORKFLOW);
            BucketEntry bucketEntry = new BucketEntry(tube,pdoSample.getProductOrder(), BucketEntry.BucketEntryType.PDO_ENTRY);
            labBatch.addBucketEntry(bucketEntry);
            tube.addSample(pdoSample.getMercurySample());
            tube.addBucketEntry(bucketEntry);
            return new SampleInstanceV2(tube);
        }
    }
}

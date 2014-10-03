package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
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

    private ProductOrderSample CrspSample;

    private ProductOrderSample nonCrspSampleWithGSSRMetadata;

    private ProductOrderSample nonCrspSampleWithUnknownMetadata;

    private ProductOrderSample nonCrspSampleWithBSPMetadata;

    private CrspPipelineUtils crspPipelineAPIUtils = new CrspPipelineUtils();

    private SampleData sampleDataWithNonBspSample = new MercurySampleData("Not from BSP", Collections.<Metadata>emptySet());

    private ProductOrderSample createPdoSample(MercurySample.MetadataSource metadataSource,
                                               String sampleName) {
        ProductOrder pdo = new ProductOrder();
        ProductOrderSample pdoSample = new ProductOrderSample(sampleName);
        pdoSample.setMercurySample(new MercurySample(pdoSample.getSampleKey(),metadataSource));
        pdo.addSample(pdoSample);
        ResearchProject crspProject = new ResearchProject();
        pdo.setResearchProject(crspProject);
        return pdoSample;
    }

    private void setRegulatoryDesignation(ProductOrderSample pdoSample,ResearchProject.RegulatoryDesignation regulatoryDesignation) {
        pdoSample.getProductOrder().getResearchProject().setRegulatoryDesignation(regulatoryDesignation);
    }

    @BeforeMethod
    public void setUp() {
        CrspSample = createPdoSample(MercurySample.MetadataSource.MERCURY,"sample1");
        setRegulatoryDesignation(CrspSample, ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP);
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
                            CrspSample.getMercurySample().getMetadataSourceForPipelineAPI());
    }

    public void testAllSamplesAreForCrsp() {
        boolean hasAllCrspSamples = new CrspPipelineUtils().areAllSamplesForCrsp(createSampleInstances(
                CrspSample));

        Assert.assertTrue(hasAllCrspSamples);
    }

    public void testNoSamplesAreForCrsp() {
        boolean hasAllCrspSamples = new CrspPipelineUtils().areAllSamplesForCrsp(createSampleInstances(
                nonCrspSampleWithGSSRMetadata));

        Assert.assertFalse(hasAllCrspSamples);
    }

    public void testMixOfCrspAndNonCrspSamplesShouldThrowException() {
        Set<SampleInstanceV2> sampleInstances = createSampleInstances(nonCrspSampleWithGSSRMetadata, CrspSample);
        try {
            new CrspPipelineUtils().areAllSamplesForCrsp(sampleInstances);
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
            crspPipelineAPIUtils.setFieldsForCrsp(new LibraryBean(), sampleDataWithNonBspSample,null,null, Deployment.PROD);
            Assert.fail("Should have thrown an exception because " + sampleDataWithNonBspSample.getSampleId() + " is not a bsp sample");
        }
        catch(RuntimeException ignored) {}
    }

    public void testSetFieldsForCrspDoesNotThrowExceptionForNonBspSampleInDevelopment() {
        crspPipelineAPIUtils.setFieldsForCrsp(new LibraryBean(),sampleDataWithNonBspSample,null,null, Deployment.DEV);
    }


    private class TestSampleInstance {

        private final ProductOrderSample pdoSample;

        public TestSampleInstance(ProductOrderSample pdoSample) {
            this.pdoSample = pdoSample;
        }

        public SampleInstanceV2 getSampleInstance() {
            LabVessel tube = new BarcodedTube("000000");
            BucketEntry bucketEntry = new BucketEntry(tube,pdoSample.getProductOrder(), BucketEntry.BucketEntryType.PDO_ENTRY);
            tube.addSample(pdoSample.getMercurySample());
            tube.addBucketEntry(bucketEntry);
            return new SampleInstanceV2(tube);
        }
    }
}

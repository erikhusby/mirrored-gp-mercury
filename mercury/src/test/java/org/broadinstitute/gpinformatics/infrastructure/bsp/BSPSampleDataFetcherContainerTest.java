package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.testng.Assert;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

public class BSPSampleDataFetcherContainerTest extends Arquillian {

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private BSPSampleDataFetcher bspSampleDataFetcher;


    @Test(enabled = true)
    public void testFFPE() {
        BSPSampleDTO ffpe = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-16BL4");
        BSPSampleDTO paraffin = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-2UVBU");
        BSPSampleDTO notFFPE = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-3HM8");

        Assert.assertNotNull(ffpe);
        Assert.assertNotNull(paraffin);
        Assert.assertNotNull(notFFPE);
        List<BSPSampleDTO> dtoList = Arrays.asList(ffpe, paraffin, notFFPE);

        bspSampleDataFetcher.fetchFFPEDerived(dtoList);

        Assert.assertTrue(ffpe.getFfpeStatus());
        Assert.assertTrue(paraffin.getFfpeStatus());
        Assert.assertFalse(notFFPE.getFfpeStatus());
    }

    @Test(enabled = true)
    public void testSamplePlastic() {
        BSPSampleDTO rootSample = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-12LY");
        BSPSampleDTO aliquotSample = bspSampleDataFetcher.fetchSingleSampleFromBSP("SM-3HM8");

        Assert.assertNotNull(rootSample);
        Assert.assertNotNull(aliquotSample);
        List<BSPSampleDTO> dtoList = Arrays.asList(rootSample, aliquotSample);

        bspSampleDataFetcher.fetchSamplePlastic(dtoList);

        Assert.assertNotNull(rootSample.getPlasticBarcodes());
        Assert.assertNotNull(aliquotSample.getPlasticBarcodes());
    }
}

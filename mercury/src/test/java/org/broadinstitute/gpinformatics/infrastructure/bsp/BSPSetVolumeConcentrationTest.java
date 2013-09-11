package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSetVolumeConcentrationTest extends Arquillian {

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private BSPConfig bspConfig;

    @Inject
    @SuppressWarnings("redundantImplemen")
    private BSPSampleSearchService bspSampleSearchService;

    @Test()
    public void testSetVolumeAndConcentration() {
        BSPSampleDataFetcher dataFetcher = new BSPSampleDataFetcher(bspSampleSearchService, bspConfig);
        String TEST_SAMPLE_ID = "SM-1234";
        BSPSampleDTO bspSampleDTO = dataFetcher.fetchSingleSampleFromBSP(TEST_SAMPLE_ID);

        Double currentVolume = bspSampleDTO.getVolume();
        Double currentConcentration = bspSampleDTO.getConcentration();
        BigDecimal newVolume = BigDecimal.valueOf((Math.random() * 50) + 1);
        BigDecimal newConcentration = BigDecimal.valueOf((Math.random() * 50) + 1);

        Assert.assertNotEquals(currentVolume, newVolume);
        Assert.assertNotEquals(currentConcentration, newConcentration);

        BSPSetVolumeConcentrationImpl bspSetVolumeConcentration = new BSPSetVolumeConcentrationImpl(bspConfig);
        bspSetVolumeConcentration
                .setVolumeAndConcentration(TEST_SAMPLE_ID, newVolume, newConcentration);
        String[] result = bspSetVolumeConcentration.getResult();
        Assert.assertTrue(result.length > 0);
        Assert.assertFalse(StringUtils.isBlank(result[0]));
        Assert.assertTrue(bspSetVolumeConcentration.isValidResult());

        bspSampleDTO = dataFetcher.fetchSingleSampleFromBSP(TEST_SAMPLE_ID);
        currentVolume = bspSampleDTO.getVolume();
        currentConcentration = bspSampleDTO.getConcentration();

        // Numbers returned from BSP are rounded differently so I need to lower the precision of the returned value.
        Double scaledVolume = newVolume.setScale(5, RoundingMode.HALF_UP).doubleValue();
        Double scaledConcentration = newConcentration.setScale(5, RoundingMode.HALF_UP).doubleValue();

        Assert.assertEquals(scaledVolume, currentVolume);
        Assert.assertEquals(scaledConcentration, currentConcentration);
    }
}

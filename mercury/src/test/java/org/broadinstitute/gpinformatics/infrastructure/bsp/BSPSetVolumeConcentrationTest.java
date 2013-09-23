package org.broadinstitute.gpinformatics.infrastructure.bsp;

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
    private static final double ERROR_BAND = 0.00001;

    @Deployment
    public static WebArchive getDeployment() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private BSPSampleSearchService bspSampleSearchService;

    @Test()
    public void testSetVolumeAndConcentration() {
        BSPSampleDataFetcher dataFetcher = new BSPSampleDataFetcher(bspSampleSearchService, bspConfig);
        BSPSetVolumeConcentrationImpl bspSetVolumeConcentration = new BSPSetVolumeConcentrationImpl(bspConfig);

        String TEST_SAMPLE_ID = "SM-1234";
        BigDecimal[] newVolume = new BigDecimal[] {getRandomBigDecimal(), new BigDecimal("43.068215")};
        BigDecimal[] newConcentration = new BigDecimal[] {getRandomBigDecimal(),  new BigDecimal("43.068225")};

        for (int i = 0; i < newVolume.length; ++i) {
            String result = bspSetVolumeConcentration.setVolumeAndConcentration(
                    TEST_SAMPLE_ID, newVolume[i], newConcentration[i]);
            Assert.assertEquals(result, BSPSetVolumeConcentration.RESULT_OK);

            BSPSampleDTO bspSampleDTO = dataFetcher.fetchSingleSampleFromBSP(TEST_SAMPLE_ID);
            Double currentVolume = bspSampleDTO.getVolume();
            Double currentConcentration = bspSampleDTO.getConcentration();

            Assert.assertTrue(scaleResult(newVolume[i]) - currentVolume <= ERROR_BAND);
            Assert.assertTrue(scaleResult(newConcentration[i]) - currentConcentration <= ERROR_BAND);
        }
    }

    /**
     * get a random number  for use in test.
     *
     * @return
     */
    private BigDecimal getRandomBigDecimal() {
        return BigDecimal.valueOf((Math.random() * 50) + 1);
    }

    private Double scaleResult(BigDecimal bigDecimal) {
        return bigDecimal.setScale(5, RoundingMode.HALF_UP).doubleValue();
    }
}

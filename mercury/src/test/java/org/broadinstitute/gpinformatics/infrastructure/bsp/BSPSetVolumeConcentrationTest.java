package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;
import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.EXTERNAL_INTEGRATION;


@Test(groups = EXTERNAL_INTEGRATION)
public class BSPSetVolumeConcentrationTest  {
    private static final double ERROR_BAND = 0.00001;

    private BSPConfig bspConfig=BSPConfig.produce(DEV);

    private BSPSampleSearchService bspSampleSearchService=new BSPSampleSearchServiceImpl(bspConfig);

    public void testSetVolumeAndConcentration() {
        BSPSampleDataFetcher dataFetcher = new BSPSampleDataFetcherImpl(bspSampleSearchService, bspConfig);
        BSPSetVolumeConcentrationImpl bspSetVolumeConcentration = new BSPSetVolumeConcentrationImpl(bspConfig);

        String testSampleId = "SM-1234";
        BigDecimal[] newVolume = {getRandomBigDecimal(), new BigDecimal("43.068215")};
        BigDecimal[] newConcentration = {getRandomBigDecimal(),  new BigDecimal("43.068225")};
        BigDecimal[] newReceptacleWeight = {getRandomBigDecimal(),  new BigDecimal("43.068235")};

        for (int i = 0; i < newVolume.length; ++i) {
            String result = bspSetVolumeConcentration.setVolumeAndConcentration(
                    testSampleId, newVolume[i], newConcentration[i], newReceptacleWeight[i],
                    BSPSetVolumeConcentration.TerminateAction.LEAVE_CURRENT_STATE);
            Assert.assertEquals(result, BSPSetVolumeConcentration.RESULT_OK);

            SampleData bspSampleData = dataFetcher.fetchSingleSampleFromBSP(testSampleId);
            Double currentVolume = bspSampleData.getVolume();
            Double currentConcentration = bspSampleData.getConcentration();

            String errorString = "%s differs from expected value of %f by %f (original value was %f).";

            Double scaledValue = scaleResult(newVolume[i]);
            double valueDifference = Math.abs(scaledValue - currentVolume);
            Assert.assertTrue(valueDifference <= ERROR_BAND,
                    String.format(errorString, "Volume", scaledValue, valueDifference, currentVolume));

            scaledValue = scaleResult(newConcentration[i]);
            valueDifference = scaledValue - currentConcentration;
            Assert.assertTrue(valueDifference <= ERROR_BAND,
                    String.format(errorString, "Concentration", scaledValue, valueDifference, currentConcentration));
        }

        // Test terminating depleted a sample.
        String testSampleId2 = "SM-11HX";
        String result = bspSetVolumeConcentration.setVolumeAndConcentration(
                testSampleId2, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                BSPSetVolumeConcentration.TerminateAction.TERMINATE_DEPLETED);
        Assert.assertEquals(result, BSPSetVolumeConcentration.RESULT_OK);

        SampleData bspSampleData = dataFetcher.fetchSingleSampleFromBSP(testSampleId2);
        Double currentVolume = bspSampleData.getVolume();
        Assert.assertTrue(currentVolume == 0);
    }

    /**
     * get a random number  for use in test.
     *
     */
    private BigDecimal getRandomBigDecimal() {
        return BigDecimal.valueOf((Math.random() * 50.0) + 1.0);
    }

    private Double scaleResult(BigDecimal bigDecimal) {
        return bigDecimal.setScale(6, RoundingMode.HALF_UP).doubleValue();
    }
}

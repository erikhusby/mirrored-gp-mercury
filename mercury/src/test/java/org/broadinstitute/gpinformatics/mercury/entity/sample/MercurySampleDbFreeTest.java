package org.broadinstitute.gpinformatics.mercury.entity.sample;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;
import static org.testng.Assert.*;

@Test(singleThreaded = true, groups = DATABASE_FREE)
public class MercurySampleDbFreeTest {

    public static final String RANDOM_SAMPLE_BARCODE = "SM-12345";
    public static final String METADATA_SOURCE_AVAILABLITY_DATA_PROVIDER = "metadataSourceAvailablityDataProvider";

    private enum ExpectedClinicalResult {
        CLINICAL, NON_CLINICAL;
    }

    @Test(groups = DATABASE_FREE)
    public void testIsClinicalSample() {
        // Test research only sample
        checkExpectedIsClinicalSampleResult(
                createMercurySampleForClinicalTest(MercurySample.MetadataSource.BSP, null),
                ExpectedClinicalResult.NON_CLINICAL);

        // Test mercury sample with non-clinical contract client
        checkExpectedIsClinicalSampleResult(
                createMercurySampleForClinicalTest(MercurySample.MetadataSource.MERCURY, ContractClient.KCO),
                ExpectedClinicalResult.NON_CLINICAL);

        // Test Crsp portal source
        checkExpectedIsClinicalSampleResult(
                createMercurySampleForClinicalTest(MercurySample.MetadataSource.CRSP_PORTAL, null),
                ExpectedClinicalResult.CLINICAL);

        // Test Mercury with Clinical contract client
        checkExpectedIsClinicalSampleResult(
                createMercurySampleForClinicalTest(MercurySample.MetadataSource.MERCURY, ContractClient.EXTERNAL_CLINICAL_PHARMA),
                ExpectedClinicalResult.CLINICAL);

        // Test mercury with no contract client
        checkExpectedIsClinicalSampleResult(
                createMercurySampleForClinicalTest(MercurySample.MetadataSource.MERCURY, null),
                ExpectedClinicalResult.NON_CLINICAL);
    }

    private void checkExpectedIsClinicalSampleResult(MercurySample mercurySample, ExpectedClinicalResult expectedClinicalResult) {
        boolean isClinical = expectedClinicalResult == ExpectedClinicalResult.CLINICAL;
        Assert.assertEquals(mercurySample.isClinicalSample(), isClinical);
    }

    private MercurySample createMercurySampleForClinicalTest(MercurySample.MetadataSource metadataSource,
                                                             ContractClient contractClient) {

        MercurySample mercurySample;

        if (contractClient != null) {
            HashSet<Metadata> metadata = new HashSet<>();
            metadata.add(new Metadata(Metadata.Key.CLIENT, contractClient.name()));
            mercurySample = new MercurySample(RANDOM_SAMPLE_BARCODE, metadata);
        } else {
            mercurySample = new MercurySample(RANDOM_SAMPLE_BARCODE, metadataSource);
        }

        return mercurySample;
    }

    /**
     * Used to verify that both the expected rules for Pipeline API have not been changed unexpectedly, as well as
     * verifying that any new metadata sources have had an option implemented.,
     */
    @Test(groups = DATABASE_FREE)
    public void testGetMetadataSourceForPipelineAPI() {
        List<MercurySample> mercurySamples = new ArrayList<>();

        mercurySamples.add(new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.BSP));
        mercurySamples.add(new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.CRSP_PORTAL));
        mercurySamples.add(new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.MERCURY));
        mercurySamples.add(new MercurySample(RANDOM_SAMPLE_BARCODE, null, true));

        for (MercurySample mercurySample : mercurySamples) {
            try {
                List<MercurySample.MetadataSource> expectedSources = null;
                switch (mercurySample.getMetadataSourceForPipelineAPI()) {
                    case "BSP":
                        expectedSources = Collections.singletonList(MercurySample.MetadataSource.BSP);
                        break;
                    case "MERCURY":
                        expectedSources = Arrays.asList(MercurySample.MetadataSource.MERCURY, MercurySample.MetadataSource.CRSP_PORTAL);
                        break;
                }
                Assert.assertNotNull(expectedSources);
                Assert.assertTrue(expectedSources.contains(mercurySample.getMetadataSource()));
            } catch (RuntimeException e) {
                Assert.assertNull(mercurySample.getMetadataSource(), "Unexpected metadata source result");
            }
        }
    }

    @Test(groups = DATABASE_FREE)
    public void testChangeMetadataSourceToCrspPortal() {
        // Verify the change occurs properly.
        MercurySample mercurySample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.MERCURY);
        Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.MERCURY);

        mercurySample.changeMetadataSourceToCrspPortal();

        Assert.assertEquals(mercurySample.getMetadataSource(), MercurySample.MetadataSource.CRSP_PORTAL);
    }

    /**
     * From MetadataSource enum
     */
    @Test(groups = DATABASE_FREE, dataProvider = METADATA_SOURCE_AVAILABLITY_DATA_PROVIDER)
    public void testMetaDataSourceSpecificAvailabilityCheck(MercurySample mercurySample,
                                                            ProductOrderSample productOrderSample,
                                                            boolean expectedResult) {

        Assert.assertEquals(expectedResult,
                mercurySample.getMetadataSource().sourceSpecificAvailabilityCheck(productOrderSample));
    }

    @DataProvider(name = METADATA_SOURCE_AVAILABLITY_DATA_PROVIDER)
    public Iterator<Object[]> metadataSourceAvailablityDataProvider() {
        List<Object[]> tests = new ArrayList<>();

        // One of each of the mercury samples, they can be reused.
        MercurySample bspSample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.BSP);
        MercurySample mercurySample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.MERCURY);
        MercurySample crspSample = new MercurySample(RANDOM_SAMPLE_BARCODE, MercurySample.MetadataSource.CRSP_PORTAL);

        // PDO Samples should be able to be reused
        ProductOrderSample receivedAccessioned = Mockito.mock(ProductOrderSample.class);
        Mockito.when(receivedAccessioned.isSampleReceived()).thenReturn(true);
        Mockito.when(receivedAccessioned.isSampleAccessioned()).thenReturn(true);

        ProductOrderSample notReceivedNotAccessioned = Mockito.mock(ProductOrderSample.class);
        Mockito.when(notReceivedNotAccessioned.isSampleReceived()).thenReturn(false);
        Mockito.when(notReceivedNotAccessioned.isSampleAccessioned()).thenReturn(false);

        ProductOrderSample receivedNotAccessioned = Mockito.mock(ProductOrderSample.class);
        Mockito.when(receivedNotAccessioned.isSampleReceived()).thenReturn(true);
        Mockito.when(receivedNotAccessioned.isSampleAccessioned()).thenReturn(false);

        ProductOrderSample notReceivedAccessioned = Mockito.mock(ProductOrderSample.class);
        Mockito.when(notReceivedAccessioned.isSampleReceived()).thenReturn(false);
        Mockito.when(notReceivedAccessioned.isSampleAccessioned()).thenReturn(true);

        // Tests
        // Parameters:  MercurySample mercurySample, ProductOrderSample productOrderSample, boolean expectedResult

        // BSP Sample which is received
        tests.add(new Object[] { bspSample, receivedAccessioned, true });
        tests.add(new Object[] { bspSample, receivedNotAccessioned, true });
        // BSP Sample which is NOT received
        tests.add(new Object[] { bspSample, notReceivedAccessioned, false });
        tests.add(new Object[] { bspSample, notReceivedNotAccessioned, false });

        // Mercury Sample which is Accessioned but NOT received
        tests.add(new Object[] { mercurySample, notReceivedAccessioned, false });
        // Mercury Sample which is NOT Accessioned but received
        tests.add(new Object[] { mercurySample, receivedNotAccessioned, false });
        // Mercury Sample which is neither accessioned nor received
        tests.add(new Object[] { mercurySample, notReceivedNotAccessioned, false });
        // Mercury sample which is both accessioned and received
        tests.add(new Object[] { mercurySample, receivedAccessioned, true });

        // Crsp Sample which is Accessioned but NOT received
        tests.add(new Object[] { crspSample, notReceivedAccessioned, false });
        // Crsp Sample which is NOT Accessioned but received
        tests.add(new Object[] { crspSample, receivedNotAccessioned, false });
        // Crsp Sample which is neither accessioned nor received
        tests.add(new Object[] { crspSample, notReceivedNotAccessioned, false });
        // Crsp sample which is both accessioned and received
        tests.add(new Object[] { crspSample, receivedAccessioned, true });

        return tests.iterator();
    }
}
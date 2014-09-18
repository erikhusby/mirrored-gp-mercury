package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SampleDataFetcher}. These are effectively interaction tests that make sure that queries are
 * dispatched properly based on the sample data source as specified by the {@link MercurySample}s for the sample IDs.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleDataFetcherTest {

    /**
     * A non-BSP sample (e.g., GSSR) for which there is not a {@link MercurySample}.
     */
    public static final String GSSR_ONLY_SAMPLE_ID = "123.0";

    /**
     * A non-BSP sample (e.g., GSSR) for which there is not a {@link MercurySample}.
     */
    public static final String GSSR_SAMPLE_ID = "456.0";

    /**
     * The stock ID for SM-BSP1.
     */
    private static final String BSP_STOCK_ID = "SM-BSP0";

    /**
     * A sample for which BSP is the metadata source for.
     */
    private static final String BSP_ONLY_SAMPLE_ID = "SM-BSP1";
    private static final String BSP_ONLY_BARE_SAMPLE_ID = "BSP1";

    /**
     * A sample for which BSP is the metadata source for.
     */
    private static final String BSP_SAMPLE_ID = "SM-BSP2";
    private static final String BSP_BARE_SAMPLE_ID = "BSP2";

    /**
     * A sample that was exported from BSP for which Mercury is the metadata source for.
     */
    private static final String CLINICAL_SAMPLE_ID = "SM-MERC1";
    private static final String FUBAR_SAMPLE_ID = "SM-FUBAR";

    private BSPSampleDTO bspOnlySampleData;
    private BSPSampleDTO bspSampleData;
    private MercurySampleData clinicalSampleData;

    private MercurySampleDao mockMercurySampleDao;
    private BSPSampleDataFetcher mockBspSampleDataFetcher;
    private MercurySampleDataFetcher mockMercurySampleDataFetcher;
    private SampleDataFetcher sampleDataFetcher;

    private MercurySample gssrMercurySample;
    private MercurySample bspMercurySample;
    private MercurySample clinicalMercurySample;

    @BeforeMethod
    public void setUp() {
        gssrMercurySample = new MercurySample(GSSR_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        bspMercurySample = new MercurySample(BSP_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        clinicalMercurySample = new MercurySample(CLINICAL_SAMPLE_ID, MercurySample.MetadataSource.MERCURY);
        bspOnlySampleData = new BSPSampleDTO();
        bspSampleData = new BSPSampleDTO();
        clinicalSampleData = new MercurySampleData();

        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
        /*
         * findMapIdToListMercurySample returns a map with an entry for every sample ID key. If no MercurySamples are
         * found, the entry's value is an empty collection. The stub behavior here configures the mock to respond
         * correctly for any input, assuming that there are no MercurySamples for any sample ID.
         *
         * Where needed, different behavior can be stubbed for specific sample IDs.
         */
        when(mockMercurySampleDao.findMapIdToListMercurySample(anyCollectionOf(String.class)))
                .thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                @SuppressWarnings("unchecked")
                Collection<String> sampleKeys = (Collection<String>) invocation.getArguments()[0];

                /*
                 * This won't generally be null. However, when doing further stubbing for specific sample IDs, use of
                 * argument matchers will cause null to be passed in. Simply returning null in these cases seems to be
                 * in the spirit of stubbing with argument matchers.
                 */
                if (sampleKeys == null) {
                    return null;
                }

                Map<String, List<MercurySample>> result = new HashMap<>();
                for (String sampleKey : sampleKeys) {
                    result.put(sampleKey, Collections.<MercurySample>emptyList());
                }
                return result;
            }
        });

        mockBspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        mockMercurySampleDataFetcher = Mockito.mock(MercurySampleDataFetcher.class);

        sampleDataFetcher =
                new SampleDataFetcher(mockMercurySampleDao, mockBspSampleDataFetcher, mockMercurySampleDataFetcher);
    }

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(String).
     */

    public void fetch_single_GSSR_sample_without_MercurySample_should_query_nothing() {
        SampleData sampleData = sampleDataFetcher.fetchSampleData(GSSR_ONLY_SAMPLE_ID);

        assertThat(sampleData, nullValue());
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_single_GSSR_sample_with_MercurySample_should_query_nothing() {
        configureMercurySampleDao(GSSR_SAMPLE_ID, gssrMercurySample);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(GSSR_SAMPLE_ID);

        assertThat(sampleData, nullValue());
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_single_BSP_sample_without_MercurySample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_SAMPLE_ID, bspOnlySampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_ONLY_SAMPLE_ID);

        assertThat(sampleData, equalTo((SampleData) bspOnlySampleData));
    }

    public void fetch_single_BSP_sample_with_MercurySample_should_query_BSP() {
        configureMercurySampleDao(BSP_SAMPLE_ID, bspMercurySample);
        configureBspFetcher(BSP_SAMPLE_ID, bspSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_SAMPLE_ID);

        assertThat(sampleData, equalTo((SampleData) bspSampleData));
    }

    public void fetch_single_clinical_sample_with_MercurySample_should_query_Mercury() {
        configureMercurySampleDao(CLINICAL_SAMPLE_ID, clinicalMercurySample);
        configureMercurySampleDataFetcher(CLINICAL_SAMPLE_ID, clinicalSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(CLINICAL_SAMPLE_ID);

        assertThat(sampleData, equalTo((SampleData) clinicalSampleData));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    // TODO: fetch_single_clinical_sample_without_MercurySample_should_..._throw_exception?_return_no_data?
    public void fetch_single_clinical_sample_without_MercurySample_should___() {

    }

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(Collection<String>).
     */

    public void fetch_GSSR_samples_without_MercurySample_should_query_nothing() {
        Map<String, SampleData> sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(GSSR_ONLY_SAMPLE_ID));

        assertThat(sampleDataBySampleId.size(), equalTo(0));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_GSSR_samples_with_MercurySample_should_query_nothing() {
        configureMercurySampleDao(GSSR_SAMPLE_ID, gssrMercurySample);

        Map<String, SampleData> sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(GSSR_SAMPLE_ID));

        assertThat(sampleDataBySampleId.size(), equalTo(0));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_BSP_samples_without_MercurySample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_SAMPLE_ID, bspOnlySampleData);

        Map<String, SampleData> sampleData =
                sampleDataFetcher.fetchSampleData(Collections.singleton(BSP_ONLY_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo((SampleData) bspOnlySampleData));
    }

    public void fetch_BSP_samples_with_MercurySample_should_query_BSP() {
        configureMercurySampleDao(BSP_SAMPLE_ID, bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        configureBspFetcher(BSP_SAMPLE_ID, bspSampleData);

        Map<String, SampleData> sampleData =
                sampleDataFetcher.fetchSampleData(Collections.singleton(BSP_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo((SampleData) bspSampleData));
    }

    public void fetch_clinical_samples_with_MercurySample_should_query_Mercury() {
        configureMercurySampleDao(CLINICAL_SAMPLE_ID, clinicalMercurySample);
        configureMercurySampleDataFetcher(CLINICAL_SAMPLE_ID, clinicalSampleData);

        Map<String, SampleData> sampleData =
                sampleDataFetcher.fetchSampleData(Collections.singleton(CLINICAL_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID), Matchers.equalTo((SampleData) clinicalSampleData));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_mixed_samples_should_query_appropriate_sources() {
        // @formatter:off
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(containsInAnyOrder(
                    GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID, CLINICAL_SAMPLE_ID))))
            .thenReturn(ImmutableMap.of(
                    GSSR_ONLY_SAMPLE_ID, Collections.<MercurySample>emptyList(),
                    GSSR_SAMPLE_ID, Collections.singletonList(gssrMercurySample),
                    BSP_ONLY_SAMPLE_ID, Collections.<MercurySample>emptyList(),
                    BSP_SAMPLE_ID, Collections.singletonList(bspMercurySample),
                    CLINICAL_SAMPLE_ID, Collections.singletonList(clinicalMercurySample)
            ));
        // @formatter:on
        when(mockBspSampleDataFetcher
                .fetchSamplesFromBSP(argThat(containsInAnyOrder(BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, bspOnlySampleData, BSP_SAMPLE_ID, bspSampleData));
        configureMercurySampleDataFetcher(CLINICAL_SAMPLE_ID, clinicalSampleData);

        Map<String, SampleData> sampleData = sampleDataFetcher.fetchSampleData(Arrays.asList(
                GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID, CLINICAL_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(3));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo((SampleData) bspOnlySampleData));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo((SampleData) bspSampleData));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID), equalTo((SampleData) clinicalSampleData));
    }

    // TODO: fetch_clinical_samples_without_MercurySample_should_..._throw_exception?_return_no_data?

    /*
     * Test cases for SampleDataFetcher#getStockIdForAliquotId
     */

    public void test_getStockIdForAliquotId_for_GSSR_only_sample_should_query_nothing() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(GSSR_ONLY_SAMPLE_ID);

        assertThat(stockId, nullValue());
        // TODO: make this consistent with other SampleDataFetcher methods... either always or never query BSP for GSSR samples
//        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void test_getStockIdForAliquotId_for_GSSR_sample_should_query_nothing() {
        configureMercurySampleDao(GSSR_SAMPLE_ID, gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!

        String stockId = sampleDataFetcher.getStockIdForAliquotId(GSSR_SAMPLE_ID);

        assertThat(stockId, nullValue());
        // TODO: make this consistent with other SampleDataFetcher methods... either always or never query BSP for GSSR samples
//        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void test_getStockIdForAliquotId_for_BSP_only_sample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_only_bare_sample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_BARE_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_BARE_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_sample_should_query_BSP() {
        configureMercurySampleDao(BSP_SAMPLE_ID, bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        configureBspFetcher(BSP_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_bare_sample_should_query_BSP() {
        configureMercurySampleDao(BSP_BARE_SAMPLE_ID, bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        configureBspFetcher(BSP_BARE_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_BARE_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_clinical_sample_should_return_itself() {
        configureMercurySampleDao(CLINICAL_SAMPLE_ID, clinicalMercurySample);
        when(mockMercurySampleDataFetcher.getStockIdForAliquotId(CLINICAL_SAMPLE_ID)).thenReturn(CLINICAL_SAMPLE_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(CLINICAL_SAMPLE_ID);

        verifyZeroInteractions(mockBspSampleDataFetcher);
        assertThat(stockId, equalTo(CLINICAL_SAMPLE_ID));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void test_getStockIdForAliquotId_for_ambiguous_sample_should_throw_exception() {
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(contains(FUBAR_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(FUBAR_SAMPLE_ID, Arrays.asList(
                        new MercurySample(FUBAR_SAMPLE_ID, MercurySample.MetadataSource.BSP),
                        new MercurySample(FUBAR_SAMPLE_ID, MercurySample.MetadataSource.MERCURY))));

        sampleDataFetcher.getStockIdForAliquotId(FUBAR_SAMPLE_ID);
    }

    /*
     * Test cases for SampleDataFetcher#getStockIdByAliquotId
     */

    public void test_getStockIdByAliquotId_for_GSSR_only_sample_should_query_nothing() {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_ONLY_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(0));
        // TODO: make this consistent with other SampleDataFetcher methods... either always or never query BSP for GSSR samples
//        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void test_getStockIdByAliquotId_for_GSSR_sample_should_query_nothing() {
        configureMercurySampleDao(GSSR_SAMPLE_ID, gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(0));
        // TODO: make this consistent with other SampleDataFetcher methods... either always or never query BSP for GSSR samples
//        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void test_getStockIdByAliquotId_for_BSP_only_sample_should_query_BSP() {
        when(mockBspSampleDataFetcher.getStockIdByAliquotId(argThat(contains(BSP_ONLY_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, BSP_STOCK_ID));

        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_ONLY_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(BSP_ONLY_SAMPLE_ID), equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdByAliquotId_for_BSP_sample_should_query_BSP() {
        configureMercurySampleDao(BSP_SAMPLE_ID, bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        when(mockBspSampleDataFetcher.getStockIdByAliquotId(argThat(contains(BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_SAMPLE_ID, BSP_STOCK_ID));

        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(BSP_SAMPLE_ID), equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdByAliquotId_for_clinical_sample_should_return_itself() {
        when(mockMercurySampleDao.findBySampleKeys(argThat(contains(CLINICAL_SAMPLE_ID))))
                .thenReturn(Collections.singletonList(clinicalMercurySample));
        when(mockMercurySampleDataFetcher.getStockIdByAliquotId(argThat(contains(CLINICAL_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(CLINICAL_SAMPLE_ID, CLINICAL_SAMPLE_ID));

        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(CLINICAL_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(CLINICAL_SAMPLE_ID), equalTo(CLINICAL_SAMPLE_ID));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    /*
     * Utility methods for configuring mocks.
     */

    private void configureMercurySampleDao(String sampleId, MercurySample mercurySample) {
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(contains(sampleId))))
                .thenReturn(ImmutableMap.of(sampleId, Collections.singletonList(mercurySample)));
    }

    private void configureBspFetcher(String sampleId, BSPSampleDTO sampleData) {
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(argThat(contains(sampleId))))
                .thenReturn(ImmutableMap.of(sampleId, sampleData));
    }

    private void configureMercurySampleDataFetcher(String sampleId, MercurySampleData sampleData) {
        when(mockMercurySampleDataFetcher.fetchSampleData(argThat(contains(sampleId))))
                .thenReturn(ImmutableMap.of(CLINICAL_SAMPLE_ID, sampleData));
    }

    private void configureBspFetcher(String aliquotId, String stockId) {
        when(mockBspSampleDataFetcher.getStockIdForAliquotId(aliquotId)).thenReturn(stockId);
    }
}

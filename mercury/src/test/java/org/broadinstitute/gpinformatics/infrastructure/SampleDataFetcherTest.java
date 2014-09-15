package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Matchers.endsWith;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
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

    private BSPSampleDataFetcher mockBspSampleDataFetcher;
    private SampleDataFetcher sampleDataFetcher;

    @BeforeMethod
    public void setUp() {
        /*
         * Configure mock MercurySampleDao.
         */
        MercurySampleDao mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);

        when(mockMercurySampleDao.findBySampleKey(GSSR_ONLY_SAMPLE_ID))
                .thenReturn(Collections.<MercurySample>emptyList());
        when(mockMercurySampleDao.findBySampleKey(BSP_ONLY_SAMPLE_ID))
                .thenReturn(Collections.<MercurySample>emptyList());

        MercurySample gssrMercurySample = new MercurySample(GSSR_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        when(mockMercurySampleDao.findBySampleKey(GSSR_SAMPLE_ID))
                .thenReturn(Collections.singletonList(gssrMercurySample));

        MercurySample bspMercurySample = new MercurySample(BSP_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        when(mockMercurySampleDao.findBySampleKey(BSP_SAMPLE_ID))
                .thenReturn(Collections.singletonList(bspMercurySample));

        MercurySample mercuryMercurySample =
                new MercurySample(CLINICAL_SAMPLE_ID, MercurySample.MetadataSource.MERCURY);
        when(mockMercurySampleDao.findBySampleKey(CLINICAL_SAMPLE_ID))
                .thenReturn(Collections.singletonList(mercuryMercurySample));

        when(mockMercurySampleDao.findBySampleKey(FUBAR_SAMPLE_ID))
                .thenReturn(Arrays.asList(
                        new MercurySample(FUBAR_SAMPLE_ID, MercurySample.MetadataSource.BSP),
                        new MercurySample(FUBAR_SAMPLE_ID, MercurySample.MetadataSource.MERCURY)));

        /*
         * Configure mock BSPSampleDataFetcher.
         */
        mockBspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);

        bspOnlySampleData = new BSPSampleDTO();
        when(mockBspSampleDataFetcher.fetchSingleSampleFromBSP(BSP_ONLY_SAMPLE_ID)).thenReturn(bspOnlySampleData);
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(argThat(contains(BSP_ONLY_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, bspOnlySampleData));

        bspSampleData = new BSPSampleDTO();
        when(mockBspSampleDataFetcher.fetchSingleSampleFromBSP(BSP_SAMPLE_ID)).thenReturn(bspSampleData);
        when(mockBspSampleDataFetcher.fetchSamplesFromBSP(argThat(contains(BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_SAMPLE_ID, bspSampleData));

        when(mockBspSampleDataFetcher
                .fetchSamplesFromBSP(argThat(containsInAnyOrder(BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, bspOnlySampleData, BSP_SAMPLE_ID, bspSampleData));

        when(mockBspSampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_SAMPLE_ID)).thenReturn(BSP_STOCK_ID);
        when(mockBspSampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_BARE_SAMPLE_ID)).thenReturn(BSP_STOCK_ID);

        when(mockBspSampleDataFetcher.getStockIdForAliquotId(BSP_SAMPLE_ID)).thenReturn(BSP_STOCK_ID);
        when(mockBspSampleDataFetcher.getStockIdForAliquotId(BSP_BARE_SAMPLE_ID)).thenReturn(BSP_STOCK_ID);

        when(mockBspSampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_ONLY_SAMPLE_ID)))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, BSP_STOCK_ID));

        when(mockBspSampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_SAMPLE_ID)))
                .thenReturn(ImmutableMap.of(BSP_SAMPLE_ID, BSP_STOCK_ID));

        /*
         * Create unit under test.
         */
        sampleDataFetcher = new SampleDataFetcher(mockBspSampleDataFetcher, mockMercurySampleDao);
    }

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(String).
     */

    public void fetch_single_GSSR_sample_without_MercurySample_should_query_nothing() {
        BSPSampleDTO bspSampleDTO = sampleDataFetcher.fetchSampleData(GSSR_ONLY_SAMPLE_ID);
        assertThat(bspSampleDTO, nullValue());
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_single_GSSR_sample_with_MercurySample_should_query_nothing() {
        BSPSampleDTO bspSampleDTO = sampleDataFetcher.fetchSampleData(GSSR_SAMPLE_ID);
        assertThat(bspSampleDTO, nullValue());
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_single_BSP_sample_without_MercurySample_should_query_BSP() {
        BSPSampleDTO bspSampleDTO = sampleDataFetcher.fetchSampleData(BSP_ONLY_SAMPLE_ID);
        assertThat(bspSampleDTO, Matchers.equalTo(bspOnlySampleData));
    }

    public void fetch_single_BSP_sample_with_MercurySample_should_query_BSP() {
        BSPSampleDTO bspSampleDTO = sampleDataFetcher.fetchSampleData(BSP_SAMPLE_ID);
        assertThat(bspSampleDTO, Matchers.equalTo(bspSampleData));
    }

    // TODO: fetch_single_clinical_sample_with_MercurySample_should_query_Mercury_not_BSP()

    // TODO: fetch_single_clinical_sample_without_MercurySample_should_..._throw_exception?_return_no_data?

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(Collection<String>).
     */

    public void fetch_GSSR_samples_without_MercurySample_should_query_nothing() {
        Map<String, BSPSampleDTO> sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(GSSR_ONLY_SAMPLE_ID));
        assertThat(sampleDataBySampleId.size(), equalTo(0));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_GSSR_samples_with_MercurySample_should_query_nothing() {
        Map<String, BSPSampleDTO> sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(GSSR_SAMPLE_ID));
        assertThat(sampleDataBySampleId.size(), equalTo(0));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_BSP_samples_without_MercurySample_should_query_BSP() {
        Map<String, BSPSampleDTO> sampleData =
                sampleDataFetcher.fetchSampleData(Collections.singleton(BSP_ONLY_SAMPLE_ID));
        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo(bspOnlySampleData));
    }

    public void fetch_BSP_samples_with_MercurySample_should_query_BSP() {
        Map<String, BSPSampleDTO> sampleData =
                sampleDataFetcher.fetchSampleData(Collections.singleton(BSP_SAMPLE_ID));
        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo(bspSampleData));
    }

    public void fetch_mixed_samples_should_query_appropriate_sources() {
        Map<String, BSPSampleDTO> sampleData = sampleDataFetcher
                .fetchSampleData(Arrays.asList(GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID));
        assertThat(sampleData.size(), equalTo(2));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo(bspOnlySampleData));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo(bspSampleData));
    }

    // TODO: fetch_clinical_samples_with_MercurySample_should_query_Mercury_not_BSP()

    // TODO: fetch_clinical_samples_without_MercurySample_should_..._throw_exception?_return_no_data?

    /*
     * Test cases for SampleDataFetcher#getStockIdForAliquotId
     */

    public void test_getStockIdForAliquotId_for_GSSR_only_sample_should_query_nothing() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(GSSR_ONLY_SAMPLE_ID);
        assertThat(stockId, nullValue());
    }

    public void test_getStockIdForAliquotId_for_BSP_only_sample_should_query_BSP() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_SAMPLE_ID);
        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_only_bare_sample_should_query_BSP() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_ONLY_BARE_SAMPLE_ID);
        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_sample_should_query_BSP() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_SAMPLE_ID);
        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_bare_sample_should_query_BSP() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_BARE_SAMPLE_ID);
        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_clinical_sample_should_return_itself() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(CLINICAL_SAMPLE_ID);
        verifyZeroInteractions(mockBspSampleDataFetcher);
        assertThat(stockId, equalTo(CLINICAL_SAMPLE_ID));
    }

    @Test(expectedExceptions = RuntimeException.class)
    public void test_getStockIdForAliquotId_for_ambiguous_sample_should_throw_exception() {
        sampleDataFetcher.getStockIdForAliquotId(FUBAR_SAMPLE_ID);
    }

    /*
     * Test cases for SampleDataFetcher#getStockIdByAliquotId
     */

    public void test_getStockIdByAliquotId_for_GSSR_only_sample_should_query_nothing () {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_ONLY_SAMPLE_ID));
        assertThat(stockIdByAliquotId.size(), equalTo(0));
    }

    public void test_getStockIdByAliquotId_for_GSSR_sample_should_query_nothing () {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_SAMPLE_ID));
        assertThat(stockIdByAliquotId.size(), equalTo(0));
    }

    public void test_getStockIdByAliquotId_for_BSP_only_sample_should_query_BSP() {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_ONLY_SAMPLE_ID));
        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(BSP_ONLY_SAMPLE_ID), equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdByAliquotId_for_BSP_sample_should_query_BSP() {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_SAMPLE_ID));
        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(BSP_SAMPLE_ID), equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdByAliquotId_for_clinical_sample_should_return_itself() {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(CLINICAL_SAMPLE_ID));
        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(CLINICAL_SAMPLE_ID), equalTo(CLINICAL_SAMPLE_ID));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    @Test(enabled = false)
    public void noDataReturnedFromBspWhenMercuryIsDataSource_or_queryingMercuryOwnedSampleDoesNotQueryBsp() {

        Map<String, BSPSampleDTO> sampleData = sampleDataFetcher.fetchSampleData(
                Collections.singleton(CLINICAL_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(0));

        verify(mockBspSampleDataFetcher, never()).fetchSamplesFromBSP(anyCollectionOf(String.class));
    }
}

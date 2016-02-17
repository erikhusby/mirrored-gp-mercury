package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
     * A non-BSP sample (e.g., GSSR) for which there is a {@link MercurySample}.
     */
    public static final String GSSR_SAMPLE_ID = "456.0";

    /**
     * The stock ID for SM-BSP1.
     */
    private static final String BSP_STOCK_ID = "SM-BSP0";

    /**
     * A sample for which BSP is the metadata source.
     */
    private static final String BSP_ONLY_SAMPLE_ID = "SM-BSP1";
    private static final String BSP_ONLY_BARE_SAMPLE_ID = "BSP1";

    /**
     * A sample for which BSP is the metadata source.
     */
    private static final String BSP_SAMPLE_ID = "SM-BSP2";
    private static final String BSP_BARE_SAMPLE_ID = "BSP2";

    /**
     * A sample that was exported from BSP for which Mercury is the metadata source.
     */
    private static final String CLINICAL_SAMPLE_ID = "SM-MERC1";
    private static final String CLINICAL_PATIENT_ID = "ZB12345";
    public MercurySample bspBareMercurySample;

    private BspSampleData bspOnlySampleData;
    private BspSampleData bspSampleData;
    private Map<String, SampleData> presetSampleToSampleDataMap = new HashMap<>();
    private MercurySampleData clinicalSampleData = new MercurySampleData(CLINICAL_SAMPLE_ID, ImmutableSet.of(
            new Metadata(Metadata.Key.SAMPLE_ID, CLINICAL_SAMPLE_ID),
            new Metadata(Metadata.Key.PATIENT_ID, CLINICAL_PATIENT_ID)
    ));

    private MercurySampleDao mockMercurySampleDao;
    private BSPSampleDataFetcher mockBspSampleDataFetcher;
    private MercurySampleDataFetcher mockMercurySampleDataFetcher;
    private SampleDataFetcher sampleDataFetcher;

    private MercurySample gssrMercurySample;
    private MercurySample bspMercurySample;
    private MercurySample clinicalMercurySample;

    private Map<String, MercurySample> presetSampleToMercurySampleMap = new HashMap<>();

    @BeforeMethod
    public void setUp() {
        bspBareMercurySample = new MercurySample(BSP_BARE_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        gssrMercurySample = new MercurySample(GSSR_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        bspMercurySample = new MercurySample(BSP_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        clinicalMercurySample = new MercurySample(CLINICAL_SAMPLE_ID, MercurySample.MetadataSource.MERCURY);
        bspOnlySampleData = new BspSampleData();
        bspSampleData = new BspSampleData();
        presetSampleToSampleDataMap.put(BSP_BARE_SAMPLE_ID, bspSampleData);
        presetSampleToSampleDataMap.put(BSP_SAMPLE_ID, bspSampleData);
        clinicalSampleData = new MercurySampleData(CLINICAL_SAMPLE_ID, Collections.<Metadata>emptySet());
        clinicalMercurySample.setSampleData(clinicalSampleData);
        presetSampleToSampleDataMap.put(CLINICAL_SAMPLE_ID, clinicalSampleData );

        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);

        mockBspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcher.class);
        mockMercurySampleDataFetcher = Mockito.mock(MercurySampleDataFetcher.class);
        SampleDataSourceResolver sampleDataSourceResolver = new SampleDataSourceResolver(mockMercurySampleDao);
        sampleDataFetcher =
                new SampleDataFetcher(mockMercurySampleDao, sampleDataSourceResolver, mockBspSampleDataFetcher,
                        mockMercurySampleDataFetcher);

        presetSampleToMercurySampleMap.put(GSSR_SAMPLE_ID, gssrMercurySample);
        presetSampleToMercurySampleMap.put(BSP_SAMPLE_ID, bspMercurySample);
        presetSampleToMercurySampleMap.put(CLINICAL_SAMPLE_ID, clinicalMercurySample);
        presetSampleToMercurySampleMap.put(BSP_BARE_SAMPLE_ID, bspBareMercurySample);
    }

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(String).
     */

    public void fetch_single_GSSR_sample_without_MercurySample_should_query_nothing() {
        SampleData sampleData = sampleDataFetcher.fetchSampleData(GSSR_ONLY_SAMPLE_ID);

        assertThat(sampleData, nullValue());
        verify(mockBspSampleDataFetcher).fetchSampleData(argThat(contains(GSSR_ONLY_SAMPLE_ID)));
    }

    public void fetch_single_GSSR_sample_with_MercurySample_should_query_nothing() {
        configureMercurySampleDao(gssrMercurySample);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(GSSR_SAMPLE_ID);

        assertThat(sampleData, nullValue());
        verify(mockBspSampleDataFetcher).fetchSampleData(argThat(contains(GSSR_SAMPLE_ID)));
    }

    public void fetch_single_BSP_sample_without_MercurySample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_SAMPLE_ID, bspOnlySampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_ONLY_SAMPLE_ID);

        assertThat(sampleData, equalTo((SampleData) bspOnlySampleData));
    }

    public void fetch_single_BSP_sample_with_MercurySample_should_query_BSP() {
        configureMercurySampleDao(bspMercurySample);
        configureBspFetcher(BSP_SAMPLE_ID, bspSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_SAMPLE_ID);

        assertThat(sampleData, equalTo((SampleData) bspSampleData));
    }

    public void fetch_single_clinical_sample_with_MercurySample_should_query_Mercury() {
        configureMercurySampleDao(clinicalMercurySample);
        configureMercuryFetcher(clinicalMercurySample, clinicalSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(CLINICAL_SAMPLE_ID);

        assertThat(sampleData.getSampleId(), equalTo(CLINICAL_SAMPLE_ID));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    // TODO: fetch_single_clinical_sample_without_MercurySample_should_..._throw_exception?_return_no_data?
    public void fetch_single_clinical_sample_without_MercurySample_should___() {

    }

    @DataProvider(name = "gssrFetch_data_provider")
    public Object[][] gssrFetch_data_provider() {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{GSSR_ONLY_SAMPLE_ID});
        dataList.add(new Object[]{GSSR_SAMPLE_ID});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    /*
     * Test cases for SampleDataFetcher#fetchSampleData(Collection<String>).
     */

    @Test(dataProvider = "gssrFetch_data_provider")
    public void fetch_GSSR_samples_without_MercurySample_should_query_nothing(String sampleId) {

        when(mockBspSampleDataFetcher.fetchSampleData(argThat(contains(sampleId)))).thenReturn(
                Collections.<String, BspSampleData>emptyMap());

        Map<String, SampleData> sampleDataBySampleId;
        sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(sampleId));
        verify(mockBspSampleDataFetcher).fetchSampleData(argThat(contains(sampleId)));

        assertThat(sampleDataBySampleId.size(), equalTo(0));
    }

    @DataProvider(name = "bsp_bare_sample_fetch_data_provider")
    public Object[][] bsp_bare_sample_fetch_data_provider () {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{BSP_BARE_SAMPLE_ID, true});
        dataList.add(new Object[]{BSP_BARE_SAMPLE_ID, false});
        dataList.add(new Object[]{BSP_SAMPLE_ID, true});
        dataList.add(new Object[]{BSP_SAMPLE_ID, false});
        dataList.add(new Object[]{build_product_order_sample_with_mercury_sample_bound(MercurySample.MetadataSource.BSP,
                BSP_SAMPLE_ID), true});
        dataList.add(new Object[]{build_product_order_sample_without_mercury_sample_bound(BSP_SAMPLE_ID), false});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(dataProvider = "bsp_bare_sample_fetch_data_provider")
    public void test_BSP_sample_should_query_BSP(Object sample, Boolean configureDao) {

        String sampleId = (sample instanceof String) ? (String) sample : ((ProductOrderSample) sample).getSampleKey();

        if(configureDao) {
            configureMercurySampleDao(presetSampleToMercurySampleMap.get(sampleId));
        }
        configureBspFetcher(sampleId, (BspSampleData) presetSampleToSampleDataMap.get(sampleId));

        Map<String, SampleData> sampleDataBySampleId;
        if (sample instanceof String) {
            sampleDataBySampleId = sampleDataFetcher.fetchSampleData(Collections.singleton((String) sample));
        } else {
            sampleDataBySampleId = sampleDataFetcher.fetchSampleDataForSamples(
                    Collections.singleton((ProductOrderSample) sample), BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
        }

        verify(mockMercurySampleDao).findMapIdToMercurySample(Collections.singleton(sampleId));

        assertThat(sampleDataBySampleId.size(), equalTo(1));
        assertThat(sampleDataBySampleId.get(sampleId), equalTo(presetSampleToSampleDataMap.get(sampleId)));
    }

    @DataProvider(name = "clinicalSampleFetchProvider")
    public Object[][] clinicalSampleFetchProvider() {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{CLINICAL_SAMPLE_ID});
        dataList.add(new Object[]{build_product_order_sample_without_mercury_sample_bound(CLINICAL_SAMPLE_ID)});
        dataList.add(new Object[]{build_product_order_sample_with_mercury_sample_bound(
                MercurySample.MetadataSource.MERCURY,
                CLINICAL_SAMPLE_ID)});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(dataProvider = "clinicalSampleFetchProvider")
    public void fetch_clinical_samples_with_MercurySample_should_query_Mercury(Object sample) {

        String sampleId = (sample instanceof String) ? (String) sample : ((ProductOrderSample) sample).getSampleKey();

        configureMercurySampleDao(presetSampleToMercurySampleMap.get(sampleId));
        configureMercuryFetcher(presetSampleToMercurySampleMap.get(sampleId),
                (MercurySampleData) presetSampleToSampleDataMap.get(sampleId));

        Map<String, SampleData> sampleData;
        if(sample instanceof String) {
            sampleData = sampleDataFetcher.fetchSampleData(Collections.singleton((String)sample));
        } else {
            sampleData = sampleDataFetcher.fetchSampleDataForSamples(
                    Collections.singleton((ProductOrderSample) sample), BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
        }

        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(sampleId), Matchers.equalTo(presetSampleToSampleDataMap.get(sampleId)));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    public void fetch_mixed_samples_should_query_appropriate_sources() {
        // @formatter:off
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(
                GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID, CLINICAL_SAMPLE_ID))))
            .thenReturn(ImmutableMap.of(
                    GSSR_SAMPLE_ID, gssrMercurySample,
                    BSP_SAMPLE_ID, bspMercurySample,
                    CLINICAL_SAMPLE_ID, clinicalMercurySample
            ));
        // @formatter:on
        when(mockBspSampleDataFetcher
                .fetchSampleData(argThat(containsInAnyOrder(GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID,
                        BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, bspOnlySampleData, BSP_SAMPLE_ID, bspSampleData));
        configureMercurySampleDao(clinicalMercurySample);
        configureMercuryFetcher(clinicalMercurySample, clinicalSampleData);

        Map<String, SampleData> sampleData = sampleDataFetcher.fetchSampleData(Arrays.asList(
                GSSR_ONLY_SAMPLE_ID, GSSR_SAMPLE_ID, BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID, CLINICAL_SAMPLE_ID));

        assertThat(sampleData.size(), equalTo(3));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo((SampleData) bspOnlySampleData));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo((SampleData) bspSampleData));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID).getSampleId(), equalTo(CLINICAL_SAMPLE_ID));
    }

    @DataProvider(name = "mixedProductOrderSampleDataProvider")
    private Object[][] mixedProductOrderSampleDataProvider() {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{Arrays.asList(build_product_order_sample_without_mercury_sample_bound(
                        BSP_ONLY_SAMPLE_ID),
                build_product_order_sample_without_mercury_sample_bound(BSP_SAMPLE_ID),
                build_product_order_sample_without_mercury_sample_bound(CLINICAL_SAMPLE_ID))});
        dataList.add(new Object[]{Arrays.asList(build_product_order_sample_with_mercury_sample_bound(
                        MercurySample.MetadataSource.BSP, BSP_ONLY_SAMPLE_ID),
                build_product_order_sample_with_mercury_sample_bound(MercurySample.MetadataSource.BSP, BSP_SAMPLE_ID),
                build_product_order_sample_with_mercury_sample_bound(MercurySample.MetadataSource.MERCURY,
                        CLINICAL_SAMPLE_ID))});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(dataProvider = "mixedProductOrderSampleDataProvider")
    public void fetch_mixed_product_order_samples_should_query_appropriate_sources(Collection<ProductOrderSample> samples) {
        // @formatter:off
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(BSP_ONLY_SAMPLE_ID, BSP_SAMPLE_ID,
                CLINICAL_SAMPLE_ID))))
            .thenReturn(ImmutableMap.of(
                    GSSR_SAMPLE_ID, gssrMercurySample,
                    BSP_SAMPLE_ID, bspMercurySample,
                    CLINICAL_SAMPLE_ID, clinicalMercurySample
            ));
        // @formatter:on
        when(mockBspSampleDataFetcher
                .fetchSampleData(argThat(containsInAnyOrder(BSP_ONLY_SAMPLE_ID,
                        BSP_SAMPLE_ID)), (BSPSampleSearchColumn[]) anyVararg()))
                .thenReturn(ImmutableMap.of(BSP_ONLY_SAMPLE_ID, bspOnlySampleData, BSP_SAMPLE_ID, bspSampleData));
        configureMercurySampleDao(clinicalMercurySample);
        configureMercuryFetcher(clinicalMercurySample, clinicalSampleData);

        Map<String, SampleData> sampleData = sampleDataFetcher
                .fetchSampleDataForSamples(samples, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);

        assertThat(sampleData.size(), equalTo(3));
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo((SampleData) bspOnlySampleData));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo((SampleData) bspSampleData));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID).getSampleId(), equalTo(CLINICAL_SAMPLE_ID));
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
        configureMercurySampleDao(gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!

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
        configureMercurySampleDao(bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        configureBspFetcher(BSP_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_BSP_bare_sample_should_query_BSP() {
        configureMercurySampleDao(bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        configureBspFetcher(BSP_BARE_SAMPLE_ID, BSP_STOCK_ID);

        String stockId = sampleDataFetcher.getStockIdForAliquotId(BSP_BARE_SAMPLE_ID);

        assertThat(stockId, equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdForAliquotId_for_clinical_sample_should_return_itself() {
        configureMercurySampleDao(clinicalMercurySample);
       when(mockMercurySampleDataFetcher.getStockIdByAliquotId(argThat(contains(clinicalMercurySample))))
                .thenReturn(ImmutableMap.of(CLINICAL_SAMPLE_ID, CLINICAL_SAMPLE_ID));

        String stockId = sampleDataFetcher.getStockIdForAliquotId(CLINICAL_SAMPLE_ID);

        verifyZeroInteractions(mockBspSampleDataFetcher);
        assertThat(stockId, equalTo(CLINICAL_SAMPLE_ID));
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
        configureMercurySampleDao(gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!
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
        verifyZeroInteractions(mockMercurySampleDataFetcher);
    }

    public void test_getStockIdByAliquotId_for_BSP_sample_should_query_BSP() {
        configureMercurySampleDao(bspMercurySample); // TODO: figure out how to make the test fail when this is not done!
        when(mockBspSampleDataFetcher.getStockIdByAliquotId(argThat(contains(BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_SAMPLE_ID, BSP_STOCK_ID));

        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(BSP_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(BSP_SAMPLE_ID), equalTo(BSP_STOCK_ID));
    }

    public void test_getStockIdByAliquotId_for_clinical_sample_should_return_itself() {
        configureMercurySampleDao(clinicalMercurySample);
        when(mockMercurySampleDataFetcher.getStockIdByAliquotId(argThat(contains(clinicalMercurySample))))
                .thenReturn(ImmutableMap.of(CLINICAL_SAMPLE_ID, CLINICAL_SAMPLE_ID));

        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(CLINICAL_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(1));
        assertThat(stockIdByAliquotId.get(CLINICAL_SAMPLE_ID), equalTo(CLINICAL_SAMPLE_ID));
        verifyZeroInteractions(mockBspSampleDataFetcher);
    }

    @DataProvider(name = "fetchSampleDataWithColumns")
    public Iterator<Object[]> fetchSampleDataWithColumns() {

        List<Object[]> testCases = new ArrayList<>();
        testCases.add(new Object[]{BSPSampleSearchColumn.PDO_SEARCH_COLUMNS, true});
        testCases.add(new Object[]{BSPSampleSearchColumn.BILLING_TRACKER_COLUMNS, false});

        return testCases.iterator();
    }

    @Test(dataProvider = "fetchSampleDataWithColumns")
    public void test_fetchSampleData_for_BSP_sample_should_call_overrideWithMercuryQuants_when_Quants_are_requested(
            BSPSampleSearchColumn[] searchColumns, boolean quantDataExpected) {
        BspSampleData mockBspSampleData = Mockito.mock(BspSampleData.class);
        configureBspFetcher(BSP_SAMPLE_ID, mockBspSampleData);
        configureMercurySampleDao(bspMercurySample);
        ProductOrderSample productOrderSample =
                build_product_order_sample_without_mercury_sample_bound(BSP_SAMPLE_ID);
        productOrderSample.getProductOrder().getProduct().setExpectInitialQuantInMercury(quantDataExpected);

        bspMercurySample.addProductOrderSample(productOrderSample);
        sampleDataFetcher.fetchSampleDataForSamples(Collections.singletonList(productOrderSample), searchColumns);

        when(mockBspSampleDataFetcher.fetchSampleData(argThat(contains(BSP_SAMPLE_ID))))
                .thenReturn(ImmutableMap.of(BSP_SAMPLE_ID, mockBspSampleData));

        int invocationCount = quantDataExpected ? 1 : 0;
        verify(mockBspSampleData, times(invocationCount)).overrideWithMercuryQuants(productOrderSample);
    }

    /*
     * Utility methods for configuring mocks.
     */

    private void configureMercurySampleDao(MercurySample mercurySample) {
        String sampleKey = mercurySample.getSampleKey();
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(contains(sampleKey))))
                .thenReturn(ImmutableMap.of(sampleKey, mercurySample));
    }

    private void configureBspFetcher(String sampleId, BspSampleData sampleData) {
        when(mockBspSampleDataFetcher.fetchSampleData(argThat(contains(sampleId))))
                .thenReturn(ImmutableMap.of(sampleId, sampleData));
        when(mockBspSampleDataFetcher.fetchSampleData(argThat(contains(sampleId)),
                (BSPSampleSearchColumn[]) anyVararg()))
                .thenReturn(ImmutableMap.of(sampleId, sampleData));
    }

    private void configureMercuryFetcher(MercurySample mercurySample, MercurySampleData sampleData) {
        when(mockMercurySampleDataFetcher.fetchSampleData(argThat(contains(mercurySample))))
                .thenReturn(ImmutableMap.of(mercurySample.getSampleKey(), sampleData));
    }

    private void configureBspFetcher(String aliquotId, String stockId) {
        when(mockBspSampleDataFetcher.getStockIdByAliquotId(argThat(contains(aliquotId))))
                .thenReturn(ImmutableMap.of(aliquotId, stockId));
    }

    private ProductOrderSample build_product_order_sample_without_mercury_sample_bound(String sampleId) {

        ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);
        ProductOrder productOrder = new ProductOrder();
        productOrder.setProduct(new Product());
        productOrderSample.setProductOrder(productOrder);
        return productOrderSample;
    }

    private ProductOrderSample build_product_order_sample_with_mercury_sample_bound(
            MercurySample.MetadataSource metadataSource,
            String sampleId) {

        ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);
        ProductOrder productOrder = new ProductOrder();
        productOrder.setProduct(new Product());
        productOrderSample.setProductOrder(productOrder);
        MercurySample mercurySample = presetSampleToMercurySampleMap.get(sampleId);
        if(mercurySample == null) {
            mercurySample = new MercurySample(sampleId, metadataSource);
        }
        SampleData sampleData = presetSampleToSampleDataMap.get(sampleId);
        if(sampleData != null) {
            mercurySample.setSampleData(sampleData);
        }
        productOrderSample.setMercurySample(mercurySample);


        return productOrderSample;
    }
}

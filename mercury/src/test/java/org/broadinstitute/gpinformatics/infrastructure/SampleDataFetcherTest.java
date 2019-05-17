package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.athena.boundary.products.InvalidProductException;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.athena.entity.products.Product;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcher;
import org.hamcrest.Matchers;
import org.mockito.Mockito;
import org.testng.Assert;
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
import java.util.Set;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleDataFetcherTest {
    /**
     * A non-BSP sample (e.g., GSSR) for which there is not a {@link MercurySample}.
     */
    private static final String GSSR_ONLY_SAMPLE_ID = "123.0";

    /**
     * A non-BSP sample (e.g., GSSR) for which there is a {@link MercurySample}.
     */
    private static final String GSSR_SAMPLE_ID = "456.0";

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

    /** A sample with Mercury metadata source. */
    private static final String CLINICAL_SAMPLE_ID = "SM-MERC1";
    private static final String CLINICAL_PATIENT_ID = "ZB12345";
    /** A sample with Mercury metadata source and inherited BSP root. */
    private static final String INHERITED_SAMPLE_ID = "SM-MERC2";
    private static final String INHERITED_ROOT_ID = "SM-BSP3";
    private static final String INHERITED_COLLAB_PATIENT_ID = "KmCXs922";
    private static final String INHERITED_COLLAB_SAMPLE_ID = "wUcpZ922";
    private static final String INHERITED_SEX = "XY";
    private static final String INHERITED_ORGANISM = "Not sure";

    private BspSampleData bspOnlySampleData;
    private BspSampleData bspSampleData;
    private Map<String, SampleData> presetSampleToSampleDataMap = new HashMap<>();
    private MercurySampleData clinicalSampleData = new MercurySampleData(CLINICAL_SAMPLE_ID, ImmutableSet.of(
            new Metadata(Metadata.Key.SAMPLE_ID, CLINICAL_SAMPLE_ID),
            new Metadata(Metadata.Key.PATIENT_ID, CLINICAL_PATIENT_ID)
    ));
    private Set<Metadata> mercuryWithInheritedRootMetadata =
            ImmutableSet.of(new Metadata(Metadata.Key.ROOT_SAMPLE, INHERITED_ROOT_ID));
    private MercurySampleData mercuryWithInheritedRootSampleData = new MercurySampleData(INHERITED_SAMPLE_ID,
            mercuryWithInheritedRootMetadata);
    private BspSampleData bspInheritanceRootSampleData = new BspSampleData(ImmutableMap.of(
            BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, INHERITED_COLLAB_SAMPLE_ID,
            BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID, INHERITED_COLLAB_PATIENT_ID,
            BSPSampleSearchColumn.GENDER, INHERITED_SEX,
            BSPSampleSearchColumn.SPECIES, INHERITED_ORGANISM
    ));

    private MercurySampleDao mockMercurySampleDao;
    private BSPSampleDataFetcherImpl mockBspSampleDataFetcher;
    private MercurySampleDataFetcher mockMercurySampleDataFetcher;
    private SampleDataFetcher sampleDataFetcher;

    private MercurySample gssrMercurySample;
    private MercurySample bspMercurySample;
    private MercurySample clinicalMercurySample;
    private MercurySample mercurySampleWithInheritedRoot;

    private Map<String, MercurySample> presetSampleToMercurySampleMap = new HashMap<>();

    @BeforeMethod
    public void setUp() {
        MercurySample bspBareMercurySample = new MercurySample(BSP_BARE_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        gssrMercurySample = new MercurySample(GSSR_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        bspMercurySample = new MercurySample(BSP_SAMPLE_ID, MercurySample.MetadataSource.BSP);
        clinicalMercurySample = new MercurySample(CLINICAL_SAMPLE_ID, MercurySample.MetadataSource.MERCURY);
        mercurySampleWithInheritedRoot = new MercurySample(INHERITED_SAMPLE_ID, MercurySample.MetadataSource.MERCURY);

        bspOnlySampleData = new BspSampleData();
        bspSampleData = new BspSampleData();
        presetSampleToSampleDataMap.put(BSP_BARE_SAMPLE_ID, bspSampleData);
        presetSampleToSampleDataMap.put(BSP_SAMPLE_ID, bspSampleData);
        clinicalSampleData = new MercurySampleData(CLINICAL_SAMPLE_ID, Collections.emptySet());
        clinicalMercurySample.setSampleData(clinicalSampleData);
        mercurySampleWithInheritedRoot.addMetadata(mercuryWithInheritedRootMetadata);

        presetSampleToSampleDataMap.put(CLINICAL_SAMPLE_ID, clinicalSampleData );

        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);

        mockBspSampleDataFetcher = Mockito.mock(BSPSampleDataFetcherImpl.class);
        mockMercurySampleDataFetcher = Mockito.mock(MercurySampleDataFetcher.class);
        sampleDataFetcher = new SampleDataFetcher(mockMercurySampleDao, mockBspSampleDataFetcher,
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
    }

    public void fetch_single_GSSR_sample_with_MercurySample_should_query_nothing() {
        configureMercurySampleDao(gssrMercurySample);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(GSSR_SAMPLE_ID);

        assertThat(sampleData, nullValue());
    }

    public void fetch_single_BSP_sample_without_MercurySample_should_query_BSP() {
        configureBspFetcher(BSP_ONLY_SAMPLE_ID, bspOnlySampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_ONLY_SAMPLE_ID);

        assertThat(sampleData, equalTo(bspOnlySampleData));
    }

    public void fetch_single_BSP_sample_with_MercurySample_should_query_BSP() {
        configureMercurySampleDao(bspMercurySample);
        configureBspFetcher(BSP_SAMPLE_ID, bspSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(BSP_SAMPLE_ID);

        assertThat(sampleData, equalTo(bspSampleData));
    }

    public void fetch_single_clinical_sample_with_MercurySample_should_query_Mercury() {
        configureMercurySampleDao(clinicalMercurySample);
        configureMercuryFetcher(clinicalMercurySample, clinicalSampleData);

        SampleData sampleData = sampleDataFetcher.fetchSampleData(CLINICAL_SAMPLE_ID);

        assertThat(sampleData.getSampleId(), equalTo(CLINICAL_SAMPLE_ID));
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

        when(mockBspSampleDataFetcher.fetchSampleData(Collections.singletonList(sampleId),
                BSPSampleSearchColumn.PDO_SEARCH_COLUMNS)).thenReturn(
                Collections.emptyMap());

        Map<String, SampleData> sampleDataBySampleId;
        sampleDataBySampleId =
                sampleDataFetcher.fetchSampleData(Collections.singleton(sampleId));

        assertThat(sampleDataBySampleId.size() == 0 || sampleDataBySampleId.get(sampleId) == null, equalTo(true));
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
    }

    public void fetch_mixed_samples_should_query_appropriate_sources() {
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(
                GSSR_ONLY_SAMPLE_ID,
                GSSR_SAMPLE_ID,
                BSP_ONLY_SAMPLE_ID,
                BSP_SAMPLE_ID,
                CLINICAL_SAMPLE_ID
        )))).thenReturn(ImmutableMap.of(
                GSSR_SAMPLE_ID, gssrMercurySample,
                BSP_SAMPLE_ID, bspMercurySample,
                CLINICAL_SAMPLE_ID, clinicalMercurySample
        ));
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(
                CLINICAL_SAMPLE_ID
        )))).thenReturn(ImmutableMap.of(
                CLINICAL_SAMPLE_ID, clinicalMercurySample
        ));

        when(mockBspSampleDataFetcher.fetchSampleData(argThat(containsInAnyOrder(
                BSP_ONLY_SAMPLE_ID,
                BSP_SAMPLE_ID
        )), anyVararg())).thenReturn(ImmutableMap.of(
                BSP_ONLY_SAMPLE_ID, bspOnlySampleData,
                BSP_SAMPLE_ID, bspSampleData
        ));
        configureMercuryFetcher(clinicalMercurySample, clinicalSampleData);

        Map<String, SampleData> sampleData = sampleDataFetcher.fetchSampleData(Arrays.asList(
                GSSR_ONLY_SAMPLE_ID,
                GSSR_SAMPLE_ID,
                BSP_ONLY_SAMPLE_ID,
                BSP_SAMPLE_ID,
                CLINICAL_SAMPLE_ID
        ));

        sampleData.entrySet().stream().forEach(mapEntry -> {
            switch (mapEntry.getKey()) {
            case BSP_ONLY_SAMPLE_ID:
                assertThat(mapEntry.getValue(), equalTo(bspOnlySampleData));
                break;
            case BSP_SAMPLE_ID: assertThat(mapEntry.getValue(), equalTo(bspSampleData));
                break;
            case CLINICAL_SAMPLE_ID: assertThat(mapEntry.getValue(), equalTo(clinicalSampleData));
                break;
            default: assertThat(mapEntry.getValue(), nullValue());
            }});
    }

    @Test
    public void testMetadataInheritanceRoot() {
        // Does the setup.
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(
                INHERITED_SAMPLE_ID,
                BSP_SAMPLE_ID,
                CLINICAL_SAMPLE_ID
        )))).thenReturn(ImmutableMap.of(
                INHERITED_SAMPLE_ID, mercurySampleWithInheritedRoot,
                BSP_SAMPLE_ID, bspMercurySample,
                CLINICAL_SAMPLE_ID, clinicalMercurySample
        ));
        when(mockBspSampleDataFetcher.fetchSampleData(argThat(containsInAnyOrder(
                INHERITED_ROOT_ID,
                BSP_SAMPLE_ID
        )), anyVararg())).thenReturn(ImmutableMap.of(
                INHERITED_ROOT_ID, bspInheritanceRootSampleData,
                BSP_SAMPLE_ID, bspSampleData
        ));
        when(mockMercurySampleDataFetcher.fetchSampleData(argThat(containsInAnyOrder(
                mercurySampleWithInheritedRoot, clinicalMercurySample
        )))).thenReturn(ImmutableMap.of(
                mercurySampleWithInheritedRoot.getSampleKey(), mercuryWithInheritedRootSampleData,
                clinicalMercurySample.getSampleKey(), clinicalSampleData
        ));

        // Executes the method under test.
        Map<String, SampleData> sampleData = sampleDataFetcher.fetchSampleData(Arrays.asList(
                INHERITED_SAMPLE_ID,
                CLINICAL_SAMPLE_ID,
                BSP_SAMPLE_ID
        ));

        // Validates the results.
        assertThat(sampleData.size(), equalTo(3));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo(bspSampleData));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID).getSampleId(), equalTo(CLINICAL_SAMPLE_ID));

        // This sample should have the two Mercury metadata and the four inherited Bsp metadata.
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getSampleId(), equalTo(INHERITED_SAMPLE_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getRootSample(), equalTo(INHERITED_ROOT_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getCollaboratorsSampleName(),
                equalTo(INHERITED_COLLAB_SAMPLE_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getCollaboratorParticipantId(),
                equalTo(INHERITED_COLLAB_PATIENT_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getGender(), equalTo(INHERITED_SEX));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getOrganism(), equalTo(INHERITED_ORGANISM));
    }

    @Test
    public void testFalseMetadataInheritanceRoot() {
        // Does the setup.
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(
                INHERITED_SAMPLE_ID
        )))).thenReturn(ImmutableMap.of(
                INHERITED_SAMPLE_ID, mercurySampleWithInheritedRoot
        ));
        // For this test the root id is not found in BSP.
        when(mockBspSampleDataFetcher.fetchSampleData(argThat(containsInAnyOrder(
                INHERITED_ROOT_ID
        )), anyVararg())).thenReturn(ImmutableMap.of(
        ));
        when(mockMercurySampleDataFetcher.fetchSampleData(argThat(containsInAnyOrder(
                mercurySampleWithInheritedRoot
        )))).thenReturn(ImmutableMap.of(
                mercurySampleWithInheritedRoot.getSampleKey(), mercuryWithInheritedRootSampleData
        ));

        // Executes the method under test.
        Map<String, SampleData> sampleData = sampleDataFetcher.fetchSampleData(Collections.singletonList(
                INHERITED_SAMPLE_ID
        ));

        // Validates the results. The inherited metadata is null in this case.
        assertThat(sampleData.size(), equalTo(1));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getSampleId(), equalTo(INHERITED_SAMPLE_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getRootSample(), equalTo(INHERITED_ROOT_ID));
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getCollaboratorsSampleName(), nullValue());
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getCollaboratorParticipantId(), nullValue());
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getGender(), nullValue());
        assertThat(sampleData.get(INHERITED_SAMPLE_ID).getOrganism(), nullValue());
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
        assertThat(sampleData.get(BSP_ONLY_SAMPLE_ID), equalTo( bspOnlySampleData));
        assertThat(sampleData.get(BSP_SAMPLE_ID), equalTo(bspSampleData));
        assertThat(sampleData.get(CLINICAL_SAMPLE_ID).getSampleId(), equalTo(CLINICAL_SAMPLE_ID));
    }

    /*
     * Test cases for SampleDataFetcher#getStockIdForAliquotId
     */

    public void test_getStockIdForAliquotId_for_GSSR_only_sample_should_query_nothing() {
        String stockId = sampleDataFetcher.getStockIdForAliquotId(GSSR_ONLY_SAMPLE_ID);

        assertThat(stockId, nullValue());
    }

    public void test_getStockIdForAliquotId_for_GSSR_sample_should_query_nothing() {
        configureMercurySampleDao(gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!

        String stockId = sampleDataFetcher.getStockIdForAliquotId(GSSR_SAMPLE_ID);

        assertThat(stockId, nullValue());
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

        assertThat(stockId, equalTo(CLINICAL_SAMPLE_ID));
    }

    /*
     * Test cases for SampleDataFetcher#getStockIdByAliquotId
     */

    public void test_getStockIdByAliquotId_for_GSSR_only_sample_should_query_nothing() {
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_ONLY_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(0));
    }

    public void test_getStockIdByAliquotId_for_GSSR_sample_should_query_nothing() {
        configureMercurySampleDao(gssrMercurySample); // TODO: figure out how to make the test fail when this is not done!
        Map<String, String> stockIdByAliquotId =
                sampleDataFetcher.getStockIdByAliquotId(Collections.singleton(GSSR_SAMPLE_ID));

        assertThat(stockIdByAliquotId.size(), equalTo(0));
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
        try {
            productOrder.setProduct(new Product());
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
        productOrderSample.setProductOrder(productOrder);
        return productOrderSample;
    }

    private ProductOrderSample build_product_order_sample_with_mercury_sample_bound(
            MercurySample.MetadataSource metadataSource,
            String sampleId) {

        ProductOrderSample productOrderSample = new ProductOrderSample(sampleId);
        ProductOrder productOrder = new ProductOrder();
        try {
            productOrder.setProduct(new Product());
        } catch (InvalidProductException e) {
            Assert.fail(e.getMessage());
        }
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

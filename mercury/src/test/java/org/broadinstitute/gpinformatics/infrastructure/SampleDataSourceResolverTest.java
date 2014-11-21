package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleDataSourceResolverTest {

    private SampleDataSourceResolver sampleDataSourceResolver;

    private MercurySampleDao mockMercurySampleDao;

    @BeforeMethod
    public void setUp() {
        mockMercurySampleDao = Mockito.mock(MercurySampleDao.class);
        sampleDataSourceResolver = new SampleDataSourceResolver(mockMercurySampleDao);
    }

    /*
     * Test cases for SampleDataSourceResolver#resolveSampleDataSources(Collection<String>)
     */

    /**
     * FIXME: This seems like the wrong answer to expect.
     * This should return something other than BSP, but it's difficult to identify bare BSP IDs in a way that includes
     * those but doesn't potentially include other sample ID formats (of which Squid/GSSR is only one example).
     */
    public void resolveSampleDataSources_for_Squid_sample_without_MercurySample_should_return_BSP() {
        String gssrOnlySampleId = "100.0";

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(gssrOnlySampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(gssrOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
    }

    public void resolveSampleDataSources_for_Squid_sample_with_BSP_MercurySample_should_return_BSP() {
        String gssrSampleId = "100.0";
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;
        stubMercurySampleDao(new MercurySample(gssrSampleId, metadataSource));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(gssrSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(gssrSampleId), equalTo(metadataSource));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(contains(gssrSampleId)));
    }

    public void resolveSampleDataSources_for_Squid_sample_with_MERCURY_MercurySample_should_return_MERCURY() {
        String gssrSampleId = "100.0";
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        stubMercurySampleDao(new MercurySample(gssrSampleId, metadataSource));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(gssrSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(gssrSampleId), equalTo(metadataSource));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(contains(gssrSampleId)));
    }

    public void resolveSampleDataSources_for_BSP_sample_without_MercurySample_should_return_BSP() {
        String bspOnlySampleId = "SM-1234";

        Map<String, MercurySample.MetadataSource> sources = sampleDataSourceResolver.resolveSampleDataSources(
                Collections.singleton(bspOnlySampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(bspOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
    }

    public void resolveSampleDataSources_for_BSP_sample_with_MercurySample_should_return_BSP() {
        String bspSampleId = "SM-1234";
        stubMercurySampleDao(new MercurySample(bspSampleId, MercurySample.MetadataSource.BSP));

        Map<String, MercurySample.MetadataSource> sources = sampleDataSourceResolver.resolveSampleDataSources(
                Collections.singleton(bspSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(bspSampleId), equalTo(MercurySample.MetadataSource.BSP));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(contains(bspSampleId)));
    }

    public void resolveSampleDataSources_for_clinical_sample_with_MercurySample_should_return_Mercury() {
        String clinicalSampleId = "SM-1234"; // Clinical samples can have BSPy IDs.
        stubMercurySampleDao(new MercurySample(clinicalSampleId, MercurySample.MetadataSource.MERCURY));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(clinicalSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(clinicalSampleId), equalTo(MercurySample.MetadataSource.MERCURY));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(contains(clinicalSampleId)));
    }

    public void resolveSampleDataSources_for_multiple_MERCURY_samples_should_return_MERCURY_for_all_samples() {
        String bspSampleId1 = "SM-1111";
        String bspSampleId2 = "SM-2222";
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.MERCURY;
        stubMercurySampleDao(new MercurySample(bspSampleId1, metadataSource),
                new MercurySample(bspSampleId2, metadataSource));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Arrays.asList(bspSampleId1, bspSampleId2));

        assertThat(sources.size(), equalTo(2));
        assertThat(sources.get(bspSampleId1), equalTo(metadataSource));
        assertThat(sources.get(bspSampleId2), equalTo(metadataSource));
    }

    /**
     * FIXME: This seems like the wrong answer to expect for the GSSR-only case.
     * This should return something other than BSP, but it's difficult to identify bare BSP IDs in a way that includes
     * those but doesn't potentially include other sample ID formats (of which Squid/GSSR is only one example).
     */
    public void resolveSampleDataSources_correctly_handles_query_with_all_sample_variations() {
        String gssrOnlySampleId = "100.0"; // BSP because there's no other choice
        String bspOnlySampleId = "SM-BSP1"; // BSP because there's no MercurySample
        String bspSampleId = "SM-BSP2"; // BSP because the MercurySample says BSP
        String gssrSampleIdWithMercuryData = "200.0"; // MERCURY because the MercurySample says MERCURY
        String clinicalSampleId = "SM-MERC"; // MERCURY because the MercurySample says MERCURY

        String[] sampleIds =
                {gssrOnlySampleId, bspOnlySampleId, bspSampleId, gssrSampleIdWithMercuryData, clinicalSampleId};

        Map<String, MercurySample> sampleMap = new HashMap<>();
        addToSampleMap(sampleMap, bspSampleId, MercurySample.MetadataSource.BSP);
        addToSampleMap(sampleMap, gssrSampleIdWithMercuryData, MercurySample.MetadataSource.MERCURY);
        addToSampleMap(sampleMap, clinicalSampleId, MercurySample.MetadataSource.MERCURY);

        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(sampleIds))))
                .thenReturn(sampleMap);

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Arrays.asList(sampleIds));

        assertThat(sources.size(), equalTo(5));
        assertThat(sources.get(gssrOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
        assertThat(sources.get(bspOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
        assertThat(sources.get(bspSampleId), equalTo(MercurySample.MetadataSource.BSP));
        assertThat(sources.get(gssrSampleIdWithMercuryData), equalTo(MercurySample.MetadataSource.MERCURY));
        assertThat(sources.get(clinicalSampleId), equalTo(MercurySample.MetadataSource.MERCURY));
    }

    private void addToSampleMap(Map<String, MercurySample> sampleMap, String sampleId,
                                MercurySample.MetadataSource metadataSource) {
        sampleMap.put(sampleId, new MercurySample(sampleId, metadataSource));
    }

    /*
     * Test cases for SampleDataSourceResolver#populateSampleDataSources(Collection<String>)
     */

    @DataProvider(name = "sampleDataSourceMercurySample")
    public Object[][] sampleDataSourceMercurySample() {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{buildProductOrderSample("SM-1234", false, null)});
        dataList.add(new Object[]{(buildProductOrderSample("SM-1234", true, MercurySample.MetadataSource.MERCURY))});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(dataProvider = "sampleDataSourceMercurySample")
    public void populateSampleDataSources_for_Mercury_sample(ProductOrderSample testSample) {

        MercurySample mercurySample = testSample.getMercurySample();
        if (mercurySample == null) {
            mercurySample = new MercurySample(testSample.getSampleKey(), MercurySample.MetadataSource.MERCURY);
        }
        stubMercurySampleDao(mercurySample);

        sampleDataSourceResolver.populateSampleDataSources(Collections.singleton(testSample));

        assertThat(testSample.getMetadataSource(), equalTo(MercurySample.MetadataSource.MERCURY));
        if(testSample.getMercurySample() != null) {
            verify(mockMercurySampleDao, never()).findMapIdToMercurySample(argThat(containsInAnyOrder(testSample.getSampleKey())));
        } else {
            verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(containsInAnyOrder(testSample.getSampleKey())));
        }
    }

    @DataProvider(name = "sampleDataSourceBspSample")
    public Object[][] sampleDataSourceBspSample() {
        List<Object[]> dataList = new ArrayList<>();

        dataList.add(new Object[]{buildProductOrderSamples(false, null, "SM-1234", "SM-1234")});
        dataList.add(
                new Object[]{buildProductOrderSamples(true, MercurySample.MetadataSource.BSP, "SM-1234", "SM-1234")});

        return dataList.toArray(new Object[dataList.size()][]);
    }

    @Test(dataProvider = "sampleDataSourceBspSample")
    public void populateSampleDataSources_with_duplicate_keys_populates_all_instances(
            Collection<ProductOrderSample> samples) {

        Map<String, Boolean> mapSampleIdToDaoUsage = new HashMap<>();

        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;

        for (ProductOrderSample sample : samples) {
            MercurySample mercurySample = sample.getMercurySample();
            if (mercurySample == null) {
                mercurySample = new MercurySample(sample.getSampleKey(), metadataSource);
                mapSampleIdToDaoUsage.put(sample.getSampleKey(), true);
            } else {
                mapSampleIdToDaoUsage.put(sample.getSampleKey(), false);
            }
            stubMercurySampleDao(mercurySample);
        }
        sampleDataSourceResolver.populateSampleDataSources(samples);

        for (ProductOrderSample sample : samples) {
            assertThat(sample.getMetadataSource(), equalTo(metadataSource));
            if(mapSampleIdToDaoUsage.get(sample.getSampleKey())) {
                verify(mockMercurySampleDao, never()).findMapIdToMercurySample(argThat(containsInAnyOrder(sample.getSampleKey())));
            } else {
                verify(mockMercurySampleDao).findMapIdToMercurySample(argThat(containsInAnyOrder(sample.getSampleKey())));
            }
        }
    }

    private void stubMercurySampleDao(MercurySample... mercurySamples) {
        Map<String, MercurySample> sampleIdToMercurySample = new HashMap<>();

        for (MercurySample mercurySample : mercurySamples) {
            String sampleId = mercurySample.getSampleKey();
            if (sampleIdToMercurySample.containsKey(sampleId)) {
                throw new RuntimeException(
                        "Test configuration error: cannot have multiple MercurySamples with sample ID: " + sampleId);
            }
            sampleIdToMercurySample.put(sampleId, mercurySample);
        }

        Set<String> sampleIds = sampleIdToMercurySample.keySet();
        String[] sampleIdArray = sampleIds.toArray(new String[sampleIds.size()]);
        when(mockMercurySampleDao.findMapIdToMercurySample(argThat(containsInAnyOrder(sampleIdArray))))
                .thenReturn(sampleIdToMercurySample);
    }

    public void testMercurySampleDataReturnsMetadataSourceMercury() {
        SampleData sampleData = new MercurySampleData("SM-1234", Collections.<Metadata>emptySet());
        Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
    }

    public void testBspSampleDataReturnsMetadataSourceBsp() {
        SampleData sampleData = new BspSampleData(ImmutableMap.of(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234"));
        Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.BSP);
    }

    private ProductOrderSample buildProductOrderSample(String sampleId, boolean withMercurySample,
                                                       MercurySample.MetadataSource source) {
        ProductOrderSample sample = new ProductOrderSample(sampleId);

        if (withMercurySample) {
            MercurySample mercurySample = new MercurySample(sampleId, source);
            sample.setMercurySample(mercurySample);

        }
        return sample;
    }

    private Collection<ProductOrderSample> buildProductOrderSamples(boolean withMercurySample,
                                                                    MercurySample.MetadataSource source,
                                                                    String... sampleIds) {
        List<ProductOrderSample> sampleCollection = new ArrayList<>();
        Map<String, MercurySample> sampleToMercurySample = new HashMap<>();

        for (String sampleId : sampleIds) {
            ProductOrderSample sample = new ProductOrderSample(sampleId);

            if (withMercurySample) {
                MercurySample mercurySample = sampleToMercurySample.get(sampleId);
                if (mercurySample == null) {
                    mercurySample = new MercurySample(sampleId, source);
                    sampleToMercurySample.put(sampleId, mercurySample);
                }
                sample.setMercurySample(mercurySample);
            }

        }

        return sampleCollection;
    }
}

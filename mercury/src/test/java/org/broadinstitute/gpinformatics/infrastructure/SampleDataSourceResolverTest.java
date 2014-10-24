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
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class SampleDataSourceResolverTest {

    private SampleDataSourceResolver sampleDataSourceResolver;

    private MercurySampleDao mockMercurySampleDao;

    /*
     * Test cases for SampleDataSourceResolver#determineSampleDataSource(Collection<String>)
     */

    @BeforeMethod
    public void setUp() {
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
                         * This won't generally be null. However, when doing further stubbing for specific sample IDs,
                         * use of argument matchers will cause null to be passed in. Simply returning null in these
                         * cases seems to be in the spirit of stubbing with argument matchers.
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

        sampleDataSourceResolver = new SampleDataSourceResolver(mockMercurySampleDao);
    }

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

    /**
     * FIXME: This seems like the wrong answer to expect.
     * This should return something other than BSP, but it's difficult to identify bare BSP IDs in a way that includes
     * those but doesn't potentially include other sample ID formats (of which Squid/GSSR is only one example).
     */
    public void resolveSampleDataSources_for_Squid_sample_with_MercurySample_should_return_BSP() {
        String gssrSampleId = "100.0";
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;
        stubMercurySampleDao(new MercurySample(gssrSampleId, metadataSource));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(gssrSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(gssrSampleId), equalTo(metadataSource));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToListMercurySample(argThat(contains(gssrSampleId)));
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
        verify(mockMercurySampleDao).findMapIdToListMercurySample(argThat(contains(bspSampleId)));
    }

    public void resolveSampleDataSources_for_clinical_sample_with_MercurySample_should_return_Mercury() {
        String clinicalSampleId = "SM-1234"; // Clinical samples can have BSPy IDs.
        stubMercurySampleDao(new MercurySample(clinicalSampleId, MercurySample.MetadataSource.MERCURY));

        Map<String, MercurySample.MetadataSource> sources =
                sampleDataSourceResolver.resolveSampleDataSources(Collections.singleton(clinicalSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(clinicalSampleId), equalTo(MercurySample.MetadataSource.MERCURY));

        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToListMercurySample(argThat(contains(clinicalSampleId)));
    }

    /**
     * FIXME: This seems like the wrong answer to expect for the GSSR-only case.
     * This should return something other than BSP, but it's difficult to identify bare BSP IDs in a way that includes
     * those but doesn't potentially include other sample ID formats (of which Squid/GSSR is only one example).
     */
    public void resolveSampleDataSources_correctly_handles_query_with_all_sample_variations() {
        String gssrOnlySampleId = "100.0";
        String bspOnlySampleId = "SM-1234";
        String bspSampleId = "SM-5678";

        Map<String, List<MercurySample>> sampleMap = new HashMap<>();
        sampleMap.put(gssrOnlySampleId, Collections.<MercurySample>emptyList());
        sampleMap.put(bspOnlySampleId, Collections.<MercurySample>emptyList());
        sampleMap.put(bspSampleId,
                Collections.singletonList(new MercurySample(bspSampleId, MercurySample.MetadataSource.BSP)));
        when(mockMercurySampleDao.findMapIdToListMercurySample(
                argThat(containsInAnyOrder(gssrOnlySampleId, bspOnlySampleId, bspSampleId)))).thenReturn(sampleMap);

        Map<String, MercurySample.MetadataSource> sources = sampleDataSourceResolver
                .resolveSampleDataSources(Arrays.asList(bspOnlySampleId, bspSampleId, gssrOnlySampleId));

        assertThat(sources.size(), equalTo(3));
        assertThat(sources.get(bspOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
        assertThat(sources.get(bspSampleId), equalTo(MercurySample.MetadataSource.BSP));
        assertThat(sources.get(gssrOnlySampleId), equalTo(MercurySample.MetadataSource.BSP));
    }

    public void populateSampleDataSources_for_Mercury_sample() {
        String mercurySampleId = "SM-1234";
        ProductOrderSample mercuryProductOrderSample = new ProductOrderSample(mercurySampleId);
        stubMercurySampleDao(new MercurySample(mercurySampleId, MercurySample.MetadataSource.MERCURY));

        sampleDataSourceResolver.populateSampleDataSources(Collections.singleton(mercuryProductOrderSample));

        assertThat(mercuryProductOrderSample.getMetadataSource(), equalTo(MercurySample.MetadataSource.MERCURY));
    }

    public void populateSampleDataSources_with_duplicate_keys_populates_all_instances() {
        String sampleId = "SM-1234";
        MercurySample.MetadataSource metadataSource = MercurySample.MetadataSource.BSP;
        stubMercurySampleDao(new MercurySample(sampleId, metadataSource));
        ProductOrderSample productOrderSample1 = new ProductOrderSample(sampleId);
        ProductOrderSample productOrderSample2 = new ProductOrderSample(sampleId);

        sampleDataSourceResolver.populateSampleDataSources(Arrays.asList(productOrderSample1, productOrderSample2));

        assertThat(productOrderSample1.getMetadataSource(), equalTo(metadataSource));
        assertThat(productOrderSample2.getMetadataSource(), equalTo(metadataSource));
    }

    private void stubMercurySampleDao(MercurySample mercurySample) {
        String sampleKey = mercurySample.getSampleKey();
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(contains(sampleKey))))
                .thenReturn(Collections.singletonMap(sampleKey, Collections.singletonList(mercurySample)));
    }

    public void testMercurySampleDataReturnsMetadataSourceMercury(){
        SampleData sampleData = new MercurySampleData("SM-1234", Collections.<Metadata>emptySet());
        Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.MERCURY);
    }
    public void testBspSampleDataReturnsMetadataSourceBsp(){
        SampleData sampleData = new BspSampleData(ImmutableMap.of(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234"));
        Assert.assertEquals(sampleData.getMetadataSource(), MercurySample.MetadataSource.BSP);
    }


}

package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
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
        verify(mockMercurySampleDao).findMapIdToListMercurySample(argThat(contains(gssrSampleId)));
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

        Map<String, List<MercurySample>> sampleMap = new HashMap<>();
        addToSampleMap(sampleMap, gssrOnlySampleId);
        addToSampleMap(sampleMap, bspOnlySampleId);
        addToSampleMap(sampleMap, bspSampleId, MercurySample.MetadataSource.BSP);
        addToSampleMap(sampleMap, gssrSampleIdWithMercuryData, MercurySample.MetadataSource.MERCURY);
        addToSampleMap(sampleMap, clinicalSampleId, MercurySample.MetadataSource.MERCURY);

        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(containsInAnyOrder(sampleIds))))
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

    private void addToSampleMap(Map<String, List<MercurySample>> sampleMap, String sampleId) {
        sampleMap.put(sampleId, Collections.<MercurySample>emptyList());
    }

    private void addToSampleMap(Map<String, List<MercurySample>> sampleMap, String sampleId,
                                MercurySample.MetadataSource metadataSource) {
        MercurySample mercurySample = new MercurySample(sampleId, metadataSource);
        sampleMap.put(mercurySample.getSampleKey(), Collections.singletonList(mercurySample));
    }

    /*
     * Test cases for SampleDataSourceResolver#populateSampleDataSources(Collection<String>)
     */

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

    private void stubMercurySampleDao(MercurySample... mercurySamples) {
        List<String> sampleIds = new ArrayList<>(mercurySamples.length);
        ListMultimap<String, MercurySample> sampleIdToMercurySamples =
                ArrayListMultimap.create(mercurySamples.length, 1);
        for (MercurySample mercurySample : mercurySamples) {
            String sampleId = mercurySample.getSampleKey();
            sampleIds.add(sampleId);
            sampleIdToMercurySamples.put(sampleId, mercurySample);
        }

        String[] sampleIdArray = sampleIds.toArray(new String[sampleIds.size()]);
        @SuppressWarnings("unchecked") // The untyped HashMap is safe because sampleIdToMercurySamples is a ListMultimap
        Map<String, List<MercurySample>> result = new HashMap(sampleIdToMercurySamples.asMap());
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(containsInAnyOrder(sampleIdArray))))
                .thenReturn(result);
    }
}

package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
        sampleDataSourceResolver = new SampleDataSourceResolver(mockMercurySampleDao);
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
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(contains(bspSampleId))))
                .thenReturn(Collections.singletonMap(bspSampleId,
                        Collections.singletonList(new MercurySample(bspSampleId, MercurySample.MetadataSource.BSP))));

        Map<String, MercurySample.MetadataSource> sources = sampleDataSourceResolver.resolveSampleDataSources(
                Collections.singleton(bspSampleId));

        assertThat(sources.size(), equalTo(1));
        assertThat(sources.get(bspSampleId), equalTo(MercurySample.MetadataSource.BSP));
        // Verify that stubbing for this test had some effect on the test run
        verify(mockMercurySampleDao).findMapIdToListMercurySample(argThat(contains(bspSampleId)));
    }

//    public void resolveSampleDataSources_for_Squid_sample_without_MercurySample_should_return_null() {
//    }

    private void stubMercurySampleDao(MercurySample mercurySample) {
        String sampleKey = mercurySample.getSampleKey();
        when(mockMercurySampleDao.findMapIdToListMercurySample(argThat(contains(sampleKey))))
                .thenReturn(Collections.singletonMap(sampleKey, Collections.singletonList(mercurySample)));
    }
}

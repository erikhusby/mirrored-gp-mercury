package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.google.common.collect.ImmutableList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.workrequest.BSPSampleDataFetcherImpl;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.broadinstitute.gpinformatics.Matchers.argThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class BSPSampleDataFetcherUnitTest {

    private BSPSampleSearchService mockBspSampleSearchService;
    private BSPSampleDataFetcher bspSampleDataFetcher;

    @BeforeMethod
    public void setUp() {
        mockBspSampleSearchService = Mockito.mock(BSPSampleSearchService.class);
        /*
         * This is not an actual PROD BSPConfig, so there's no danger using it in tests. It is simply acting like PROD
         * to make BSPSampleDataFetcher more stringent about what it considers a valid BSP sample ID.
         */
        BSPConfig bspConfig = new BSPConfig(Deployment.PROD);
        bspSampleDataFetcher = new BSPSampleDataFetcherImpl(mockBspSampleSearchService, bspConfig);
    }

    @Test
    public void testGetStockIdByAliquotIdEmpty() {
        Map<String, String> result = bspSampleDataFetcher.getStockIdByAliquotId(Collections.<String>emptyList());

        assertThat(result, equalTo(Collections.<String, String>emptyMap()));
        verify(mockBspSampleSearchService, never())
                .runSampleSearch(anyCollectionOf(String.class), Matchers.<BSPSampleSearchColumn[]>anyVararg());
    }

    @Test
    public void testGetStockIdByAliquotId() {
        Map<BSPSampleSearchColumn, String> searchServiceResult = new HashMap<>();
        searchServiceResult.put(BSPSampleSearchColumn.SAMPLE_ID, "SM-1234");
        searchServiceResult.put(BSPSampleSearchColumn.STOCK_SAMPLE, "SM-1230");
        when(mockBspSampleSearchService.runSampleSearch(Collections.singleton("1234"), BSPSampleSearchColumn.SAMPLE_ID,
                BSPSampleSearchColumn.STOCK_SAMPLE)).thenReturn(Collections.singletonList(searchServiceResult));

        Map<String, String> result = bspSampleDataFetcher.getStockIdByAliquotId(Collections.singleton("1234"));

        assertThat("Should have 1 result", result.size(), equalTo(1));
        assertThat(result.get("SM-1234"), equalTo("SM-1230"));
    }

    public void fetchSamplesFromBspWithOnlyNonBspFormatSamplesDoesNotQueryBsp() {
        bspSampleDataFetcher.fetchSampleData(ImmutableList.of("111.0"));

        verifyZeroInteractions(mockBspSampleSearchService);
    }

    public void fetchSamplesFromBspFiltersNonBspFormatSamples() {
        bspSampleDataFetcher.fetchSampleData(ImmutableList.of("SM-1234", "111.0"));

        verify(mockBspSampleSearchService)
                .runSampleSearch(argThat(contains("SM-1234")), Matchers.<BSPSampleSearchColumn[]>anyVararg());
    }
}

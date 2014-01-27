package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.anyCollectionOf;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
        bspSampleDataFetcher = new BSPSampleDataFetcher(mockBspSampleSearchService);
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
}

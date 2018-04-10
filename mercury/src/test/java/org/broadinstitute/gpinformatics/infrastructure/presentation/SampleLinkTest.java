package org.broadinstitute.gpinformatics.infrastructure.presentation;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class SampleLinkTest {

    private SampleLink.Factory linkFactory;

    /**
     * Sets up a mock {@link BSPConfig} that responds to {@link BSPConfig#getUrl(String)} by echoing what
     * {@link SampleLink#getUrl()} passes into it. This allows for testing of {@link SampleLink#getUrl()} in isolation
     * without also exercising {@link BSPConfig#getUrl(String)}. Also, using a real BSPConfig involves loading the
     * Mercury configuration file. This test should not depend on the contents of that file or the ability to load and
     * parse it.
     */
    @BeforeMethod
    public void setUp() throws Exception {
        BSPConfig mockBspConfig = Mockito.mock(BSPConfig.class);
        when(mockBspConfig.getUrl(anyString())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                return invocation.getArguments()[0];
            }
        });
        linkFactory = new SampleLink.Factory(mockBspConfig);
    }

    public void yieldsNoLinkForNonBspSample() {
        String sampleId = "100.0";
        SampleLink link = linkFactory.create(new ProductOrderSample(sampleId));
        assertThat(link.getHasLink(), is(false));
        assertThat(link.getUrl(), equalTo(sampleId));
        assertThat(link.getLabel(), nullValue());
        assertThat(link.getTarget(), nullValue());
    }

    public void yieldsBspLinkForBspSample() {
        String sampleId = "SM-1234";
        ProductOrderSample sample = new ProductOrderSample(sampleId);
        sample.setMetadataSource(MercurySample.MetadataSource.BSP);
        SampleLink link = linkFactory.create(sample);
        assertThat(link.getHasLink(), is(true));
        assertThat(link.getUrl(), equalTo(BSPConfig.SEARCH_PATH + sampleId));
        assertThat(link.getLabel(), equalTo("BSP Sample"));
        assertThat(link.getTarget(), equalTo("BSP_SAMPLE"));
    }

    public void yieldsNoLinkForMercurySample() {
        String sampleId = "SM-1234";
        ProductOrderSample sample = new ProductOrderSample(sampleId);
        sample.setMetadataSource(MercurySample.MetadataSource.MERCURY);
        SampleLink link = linkFactory.create(sample);
        assertThat(link.getHasLink(), is(false));
        assertThat(link.getUrl(), equalTo(sampleId));
        assertThat(link.getLabel(), nullValue());
        assertThat(link.getTarget(), nullValue());
    }
}

/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(enabled = true)
public class MercurySampleDataFetcherTest {

    private static final String SM_MERC1 = "SM-MERC1";
    private MercurySampleDataFetcher mercurySampleDataFetcher;

    @BeforeMethod
    public void setUp() throws Exception {
        mercurySampleDataFetcher = new MercurySampleDataFetcher();
    }

    public void testFetchNothingReturnsNothing() throws Exception {
        List<MercurySample> mercurySamples = new ArrayList<>();
        Map<String, MercurySampleData>
                mercurySampleData = mercurySampleDataFetcher.fetchFromMercurySamples(mercurySamples);
        assertThat(mercurySampleData.size(), equalTo(0));
    }

    public void testFetchOneMercurySampleReturnsOneResult() {
        List<MercurySample> mercurySamples =
                Collections.singletonList(new MercurySample(SM_MERC1, MercurySample.MetadataSource.MERCURY));
        Map<String, MercurySampleData>
                mercurySampleData = mercurySampleDataFetcher.fetchFromMercurySamples(mercurySamples);
        assertThat(mercurySampleData.size(), equalTo(1));
        assertThat(mercurySampleData.get(SM_MERC1).getSampleId(), equalTo(SM_MERC1));
    }

    public void testFetchMercurySampleHasSampleData() {
        List<Metadata> metadataList = Arrays.asList(
                new Metadata(Metadata.Key.SAMPLE_ID, SM_MERC1),
                new Metadata(Metadata.Key.GENDER, "Male"),
                new Metadata(Metadata.Key.PATIENT_ID, "PATIENT_ID_1234"),
                new Metadata(Metadata.Key.SAMPLE_TYPE, "DNA"),
                new Metadata(Metadata.Key.TUMOR_NORMAL, "TUMOR"),
                new Metadata(Metadata.Key.BUICK_COLLECTION_DATE, "1/1/2014"),
                new Metadata(Metadata.Key.BUICK_VISIT, "1")
        );
        assertThat(1, equalTo(2));
    }
}

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

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

@Test(groups = TestGroups.DATABASE_FREE)
public class MercurySampleDataFetcherTest {

    private static final String SM_MERC1 = "SM-MERC1";
    private static final String GENDER = "Male";
    private static final String PATIENT_ID = "PATIENT_ID_1234";
    private static final String TUMOR_NORMAL = "TUMOR";
    private static final String COLLECTION_DATE = "1/1/2014";
    private static final String BUICK_VISIT = "1";
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
        Set<Metadata> metaData = new HashSet<>(
                Arrays.asList(
                        new Metadata(Metadata.Key.SAMPLE_ID, SM_MERC1),
                        new Metadata(Metadata.Key.GENDER, GENDER),
                        new Metadata(Metadata.Key.PATIENT_ID, PATIENT_ID),
                        new Metadata(Metadata.Key.TUMOR_NORMAL, TUMOR_NORMAL),
                        new Metadata(Metadata.Key.BUICK_COLLECTION_DATE, COLLECTION_DATE),
                        new Metadata(Metadata.Key.BUICK_VISIT, BUICK_VISIT)
                ));
        MercurySample mercurySample = new MercurySample(SM_MERC1, MercurySample.MetadataSource.MERCURY, metaData);
        MercurySampleData mercurySampleData = mercurySampleDataFetcher.fetchSampleData(mercurySample);

        assertThat(mercurySampleData.getSampleId(), equalTo(SM_MERC1));
        assertThat(mercurySampleData.getGender(), equalTo(GENDER));
        assertThat(mercurySampleData.getPatientId(), equalTo(PATIENT_ID));
        assertThat(mercurySampleData.getSampleType(), equalTo(TUMOR_NORMAL));
        assertThat(mercurySampleData.getCollectionDate(), equalTo(COLLECTION_DATE));
        assertThat(mercurySampleData.getVisit(), equalTo(BUICK_VISIT));
    }
}

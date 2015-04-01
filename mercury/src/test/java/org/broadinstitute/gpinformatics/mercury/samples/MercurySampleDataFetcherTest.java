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

import com.google.common.collect.ImmutableSet;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.hamcrest.Description;
import org.hamcrest.Factory;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.samples.MercurySampleDataFetcherTest.SampleDataPatientIdMatcher.sampleWithPatientId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

@Test(groups = TestGroups.DATABASE_FREE)
public class MercurySampleDataFetcherTest {

    private static final String SM_MERC1 = "SM-MERC1";
    private static final String SM_MERC2 = "SM-MERC2";
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
                mercurySampleData = mercurySampleDataFetcher.fetchSampleData(mercurySamples);
        assertThat(mercurySampleData.size(), equalTo(0));
    }

    public void testFetchOneMercurySampleReturnsOneResult() {
        String collaboratorSampleId = "Sample-1";
        MercurySample mercurySample = new MercurySample(SM_MERC1,
                Collections.singleton(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId)));

        MercurySampleData mercurySampleData = mercurySampleDataFetcher.fetchSampleData(mercurySample);

        assertThat(mercurySampleData, notNullValue());
        assertThat(mercurySampleData.getCollaboratorsSampleName(), equalTo(collaboratorSampleId));
    }

    public void testFetchMercurySampleHasSampleData() {
        Set<Metadata> metaData = ImmutableSet.of(
                        new Metadata(Metadata.Key.SAMPLE_ID, SM_MERC1),
                        new Metadata(Metadata.Key.GENDER, GENDER),
                        new Metadata(Metadata.Key.PATIENT_ID, PATIENT_ID),
                        new Metadata(Metadata.Key.TUMOR_NORMAL, TUMOR_NORMAL),
                        new Metadata(Metadata.Key.BUICK_COLLECTION_DATE, COLLECTION_DATE),
                        new Metadata(Metadata.Key.BUICK_VISIT, BUICK_VISIT)
                );
        MercurySample mercurySample = new MercurySample(SM_MERC1, metaData);
        MercurySampleData mercurySampleData = mercurySampleDataFetcher.fetchSampleData(mercurySample);

        assertThat(mercurySampleData.getSampleId(), equalTo(SM_MERC1));
        assertThat(mercurySampleData.getGender(), equalTo(GENDER));
        assertThat(mercurySampleData.getPatientId(), equalTo(PATIENT_ID));
        assertThat(mercurySampleData.getSampleType(), equalTo(TUMOR_NORMAL));
        assertThat(mercurySampleData.getCollectionDate(), equalTo(COLLECTION_DATE));
        assertThat(mercurySampleData.getVisit(), equalTo(BUICK_VISIT));
    }

    public void testFetchForClinicalSamples() {
        String patientId1 = "Patient-1";
        Set<Metadata> metadata1 = ImmutableSet.of(new Metadata(Metadata.Key.SAMPLE_ID, SM_MERC1),
                new Metadata(Metadata.Key.PATIENT_ID, patientId1));
        String patientId2 = "Patient-2";
        Set<Metadata> metadata2 = ImmutableSet.of(new Metadata(Metadata.Key.SAMPLE_ID, SM_MERC2),
                new Metadata(Metadata.Key.PATIENT_ID, patientId2));
        MercurySample mercurySample1 = new MercurySample(SM_MERC1, metadata1);
        MercurySample mercurySample2 = new MercurySample(SM_MERC2, metadata2);

        Map<String, MercurySampleData> sampleDatas =
                mercurySampleDataFetcher.fetchSampleData(Arrays.asList(mercurySample1, mercurySample2));

        assertThat(sampleDatas.size(), equalTo(2));
        assertThat(sampleDatas.get(SM_MERC1), is(sampleWithPatientId(patientId1)));
        assertThat(sampleDatas.get(SM_MERC2), is(sampleWithPatientId(patientId2)));
    }

    public void testGetStockIdForAliquotIdReturnsMercurySample() {
        String stockId = mercurySampleDataFetcher
                .getStockIdForAliquotId(new MercurySample(SM_MERC1, MercurySample.MetadataSource.MERCURY));

        assertThat(stockId, equalTo(SM_MERC1));
    }

    public void testGetStockIdByAliquotIdReturnsMercurySample() {
        Map<String, String> stockId = mercurySampleDataFetcher
                .getStockIdByAliquotId(
                        Collections.singleton(new MercurySample(SM_MERC1, MercurySample.MetadataSource.MERCURY)));

        assertThat(stockId.size(), equalTo(1));
    }

    public static class SampleDataPatientIdMatcher extends TypeSafeMatcher<SampleData> {

        private String patientId;

        private SampleDataPatientIdMatcher(String patientId) {
            this.patientId = patientId;
        }

        @Override
        protected boolean matchesSafely(SampleData sampleData) {
            return sampleData.getPatientId().equals(patientId);
        }

        @Override
        public void describeTo(Description description) {
            description.appendText(String.format("SampleData with patient ID of %s", patientId));
        }

        @Factory
        public static Matcher<SampleData> sampleWithPatientId(String patientId) {
            return new SampleDataPatientIdMatcher(patientId);
        }
    }
}

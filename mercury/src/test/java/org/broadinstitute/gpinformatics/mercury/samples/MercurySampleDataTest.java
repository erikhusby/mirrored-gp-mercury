package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests for MercurySampleData, which is responsible for knowing how to implement SampleData for MercurySamples using
 * Mercury's Metadata.
 */
@Test(groups = TestGroups.DATABASE_FREE)
public class MercurySampleDataTest {

    public static final String SAMPLE_ID = "SM-MERC1";

    public void testNoMetadata() {
        MercurySampleData data = new MercurySampleData(SAMPLE_ID, Collections.<Metadata>emptySet());
        assertThat(data.getSampleId(), equalTo(SAMPLE_ID));
        assertThat(data.getCollaboratorsSampleName(), nullValue());
        assertThat(data.getPatientId(), nullValue());
        assertThat(data.getGender(), nullValue());
        assertThat(data.getSampleType(), nullValue());
        assertThat(data.getCollectionDate(), nullValue());
        assertThat(data.getVisit(), nullValue());
    }

    /*
     * Tests for mappings of SampleData getters to Metadata.Keys.
     */

    public void testGetCollaboratorSampleNameReturnsSampleId() {
        String collaboratorSampleId = "Sample-1";
        MercurySampleData data = new MercurySampleData(
                SAMPLE_ID, Collections.singleton(new Metadata(Metadata.Key.SAMPLE_ID, collaboratorSampleId)));
        assertThat(data.getCollaboratorsSampleName(), equalTo(collaboratorSampleId));
    }

    public void testGetPatientIdReturnsPatientId() {
        String patientId = "Patient-1";
        MercurySampleData data = new MercurySampleData(SAMPLE_ID,
                Collections.singleton(new Metadata(Metadata.Key.PATIENT_ID, patientId)));
        assertThat(data.getPatientId(), equalTo(patientId));
    }

    public void testGetGenderReturnsGender() {
        String gender = "unknown";
        MercurySampleData data =
                new MercurySampleData(SAMPLE_ID, Collections.singleton(new Metadata(Metadata.Key.GENDER, gender)));
        assertThat(data.getGender(), equalTo(gender));
    }

    public void testGetSampleTypeReturnsTumorNormal() {
        String tumorNormal = "normal";
        MercurySampleData data = new MercurySampleData(SAMPLE_ID,
                Collections.singleton(new Metadata(Metadata.Key.TUMOR_NORMAL, tumorNormal)));
        assertThat(data.getSampleType(), equalTo(tumorNormal));
    }

    public void testGetCollectionDateReturnsBuickCollectionDate() {
        String collectionDate = "4/5/06";
        MercurySampleData data = new MercurySampleData(SAMPLE_ID,
                Collections.singleton(new Metadata(Metadata.Key.BUICK_COLLECTION_DATE, collectionDate)));
        assertThat(data.getCollectionDate(), equalTo(collectionDate));
    }

    public void testGetVisitReturnsBuickVisit() {
        String visit = "visit 1";
        MercurySampleData data =
                new MercurySampleData(SAMPLE_ID, Collections.singleton(new Metadata(Metadata.Key.BUICK_VISIT, visit)));
        assertThat(data.getVisit(), equalTo(visit));
    }
}

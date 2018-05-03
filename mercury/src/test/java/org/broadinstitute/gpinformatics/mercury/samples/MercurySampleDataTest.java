package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

    public void testGetRootSampleIdReturnsSampleId() {
        MercurySampleData data = new MercurySampleData(SAMPLE_ID, Collections.<Metadata>emptySet());
        assertThat(data.getRootSample(), equalTo(SAMPLE_ID));
    }

    public void testGetCollaboratorParticipantIdReturnsPatientId() {
        String patientId = "Patient-1";
        MercurySampleData data = new MercurySampleData(SAMPLE_ID,
                                                       Collections.singleton(new Metadata(Metadata.Key.PATIENT_ID, patientId)));
        assertThat(data.getCollaboratorParticipantId(), equalTo(patientId));
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

    public void testGetMaterialTypeReturnsEmptyString() {
        SampleData hasWgaDummy = new MercurySampleData(SAMPLE_ID, Collections.<Metadata>emptySet());
        ProductOrderSample productOrderSample = new ProductOrderSample(SAMPLE_ID, hasWgaDummy);
        Assert.assertEquals(productOrderSample.getSampleData().getMaterialType(), "");
    }

    public void testSetSampleDataInMercurySampleAndProductOrderSampleSampleDataShouldBeInitialized() {
        SampleData dummyData = new MercurySampleData(SAMPLE_ID, Collections.<Metadata>emptySet());
        ProductOrderSample productOrderSample = new ProductOrderSample(SAMPLE_ID, dummyData);
        assertThat(productOrderSample.isHasBspSampleDataBeenInitialized(), is(true));
    }

    public void testSetSampleDataInProductOrderSampleAndMercurySampleSampleDataShouldBeInitialized() {
        SampleData dummyData = new MercurySampleData(SAMPLE_ID, Collections.<Metadata>emptySet());
        MercurySample mercurySample = new MercurySample(SAMPLE_ID, MercurySample.MetadataSource.BSP);
        ProductOrderSample productOrderSample = new ProductOrderSample(SAMPLE_ID);
        mercurySample.addProductOrderSample(productOrderSample);
        productOrderSample.setSampleData(dummyData);
        assertThat(mercurySample.isHasBspSampleDataBeenInitialized(), is(true));
    }

    public void testMercurySampleDataDoesNotInitializeQuantDataWhenNotQueried() {
        MercurySample mercurySample = new MercurySample("some pig", MercurySample.MetadataSource.MERCURY);
        MercurySampleData mockMercurySampleData = Mockito.spy(new MercurySampleData(mercurySample));

        mockMercurySampleData.getCollaboratorName();
        mockMercurySampleData.getRawRin();
        mockMercurySampleData.getCollaboratorParticipantId();
        mockMercurySampleData.getPatientId();
        mockMercurySampleData.getGender();
        mockMercurySampleData.getMaterialType();

        Mockito.verify(mockMercurySampleData, Mockito.never()).initializeQuantData();
    }

    public void testMercurySampleDataInitializesQuantDataOnGetVolume() {
        MercurySample mercurySample = new MercurySample("some pig", MercurySample.MetadataSource.MERCURY);
        MercurySampleData mockMercurySampleData = Mockito.spy(new MercurySampleData(mercurySample));

        mockMercurySampleData.getVolume();
        Mockito.verify(mockMercurySampleData, Mockito.times(1)).initializeQuantData();
    }

    public void testMercurySampleDataInitializesQuantDataOnGetConcentration() {
        MercurySample mercurySample = new MercurySample("some pig", MercurySample.MetadataSource.MERCURY);
        MercurySampleData mockMercurySampleData = Mockito.spy(new MercurySampleData(mercurySample));

        mockMercurySampleData.getConcentration();
        Mockito.verify(mockMercurySampleData, Mockito.times(1)).initializeQuantData();
    }

    public void testMercurySampleDataInitializesQuantDataOnGetPicoRunDate() {
        MercurySample mercurySample = new MercurySample("some pig", MercurySample.MetadataSource.MERCURY);
        MercurySampleData mockMercurySampleData = Mockito.spy(new MercurySampleData(mercurySample));
        mockMercurySampleData.getPicoRunDate();
        Mockito.verify(mockMercurySampleData, Mockito.times(1)).initializeQuantData();
    }

    public void testMercurySampleDataInitializesQuantDataOnGetTotal() {
        MercurySample mercurySample = new MercurySample("some pig", MercurySample.MetadataSource.MERCURY);
        MercurySampleData mockMercurySampleData = Mockito.spy(new MercurySampleData(mercurySample));

        mockMercurySampleData.getTotal();
        Mockito.verify(mockMercurySampleData, Mockito.times(1)).initializeQuantData();
    }
}

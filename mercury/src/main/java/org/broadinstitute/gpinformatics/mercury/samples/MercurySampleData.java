package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 */
public class MercurySampleData implements SampleData {
    private String visit;
    private String collectionDate;
    private String tumorNormal;
    private String materialType;
    private String patientId;
    private String gender;
    private String sampleId;

    public MercurySampleData(Set<Metadata> metadata) {
        extractMetadataFromSample(metadata);
    }

    private void extractMetadataFromSample(Set<Metadata> metadata) {
        for (Metadata data : metadata) {
            String value = data.getValue();
            switch (data.getKey()) {
            case SAMPLE_ID:
                this.sampleId = value;
                break;
            case GENDER:
                this.gender = value;
                break;
            case PATIENT_ID:
                this.patientId = value;
                break;
            // TODO: are we interested in SAMPLE_TYPE???
//            case SAMPLE_TYPE:
//                this.materialType = data.getValue();
//                break;
            case TUMOR_NORMAL:
                this.tumorNormal = value;
                break;
            case BUICK_COLLECTION_DATE:
                this.collectionDate = value;
                break;
            case BUICK_VISIT:
                this.visit = value;
                break;
            }
        }
    }

    public MercurySampleData() {
    }

    @Override
    public boolean hasData() {
        return false;
    }

    @Override
    public boolean canRinScoreBeUsedForOnRiskCalculation() {
        return false;
    }

    @Override
    public Date getPicoRunDate() {
        return null;
    }

    @Override
    public String getRawRin() {
        return null;
    }

    @Override
    public Double getRin() {
        return null;
    }

    @Override
    public Double getRqs() {
        return null;
    }

    @Override
    public double getVolume() {
        return 0;
    }

    @Override
    public Double getConcentration() {
        return null;
    }

    @Override
    public String getRootSample() {
        return null;
    }

    @Override
    public String getStockSample() {
        return null;
    }

    @Override
    public String getCollection() {
        return null;
    }

    @Override
    public String getCollaboratorsSampleName() {
        return null;
    }

    @Override
    public String getContainerId() {
        return null;
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getOrganism() {
        return null;
    }

    @Override
    public boolean getHasSampleKitUploadRackscanMismatch() {
        return false;
    }

    @Override
    public String getSampleLsid() {
        return null;
    }

    @Override
    public String getCollaboratorParticipantId() {
        return null;
    }

    @Override
    public String getMaterialType() {
        return materialType;
    }

    @Override
    public double getTotal() {
        return 0;
    }

    @Override
    public String getSampleType() {
        return tumorNormal;
    }

    public String getTumorNormal() {
        return tumorNormal;
    }

    @Override
    public String getPrimaryDisease() {
        return null;
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public String getStockType() {
        return null;
    }

    @Override
    public boolean isSampleReceived() {
        return false;
    }

    @Override
    public Date getReceiptDate() throws ParseException {
        return null;
    }

    @Override
    public boolean isActiveStock() {
        return false;
    }

    @Override
    public String getSampleId() {
        return sampleId;
    }

    @Override
    public MaterialType getMaterialTypeObject() {
        return null;
    }

    @Override
    public String getCollaboratorName() {
        return null;
    }

    @Override
    public String getEthnicity() {
        return null;
    }

    @Override
    public String getRace() {
        return null;
    }

    @Override
    public Boolean getFfpeStatus() {
        return null;
    }

    @Override
    public List<String> getPlasticBarcodes() {
        return null;
    }

    @Override
    public String getBarcodeForLabVessel() {
        return null;
    }

    public String getCollectionDate() {
        return collectionDate;
    }

    public String getVisit() {
        return visit;
    }
}

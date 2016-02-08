package org.broadinstitute.gpinformatics.mercury.samples;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Date;
import java.util.Set;

/**
 * This class holds sample data specific to MercurySamples whose MetadataSource == MERCURY.
 */
public class MercurySampleData implements SampleData {
    private String sampleId;
    private String collaboratorSampleId;
    private String patientId;
    private String gender;
    private String tumorNormal;

    private String collectionDate;
    private String visit;
    private final boolean hasData;
    private Date receiptDate;
    private String materialType;
    private String originalMaterialType;

    public MercurySampleData(@Nonnull String sampleId, @Nonnull Set<Metadata> metadata) {
        this(sampleId, metadata, null);
    }
    public MercurySampleData(@Nonnull String sampleId, @Nonnull Set<Metadata> metadata, @Nullable Date receiptDate) {
        this.sampleId = sampleId;
        hasData = !metadata.isEmpty();
        this.receiptDate = receiptDate;
        extractSampleDataFromMetadata(metadata);
    }

    private void extractSampleDataFromMetadata(Set<Metadata> metadata) {
        for (Metadata data : metadata) {
            String value = data.getValue();
            switch (data.getKey()) {
            case SAMPLE_ID:
                this.collaboratorSampleId = value;
                break;
            case GENDER:
                this.gender = value;
                break;
            case PATIENT_ID:
                this.patientId = value;
                break;
            case TUMOR_NORMAL:
                this.tumorNormal = value;
                break;
            case BUICK_COLLECTION_DATE:
                this.collectionDate = value;
                break;
            case BUICK_VISIT:
                this.visit = value;
                break;
            case MATERIAL_TYPE:
                this.materialType = value;
                break;
            case ORIGINAL_MATERIAL_TYPE:
                this.originalMaterialType = value;
                break;
            }
        }
    }

    @Override
    public boolean hasData() {
        return hasData;
    }

    /**
     * Mercury currently doesn't capture RIN (or RQS) scores. For the purpose of this method, the lack of data is a
     * valid case, so this always returns true.
     *
     * It is likely that, should Mercury start capturing this data, it will only store valid values. Therefore,
     * returning true here will probably always be the right thing to do.
     *
     * See {@link SampleData#canRinScoreBeUsedForOnRiskCalculation()} and
     * {@link BspSampleData#canRinScoreBeUsedForOnRiskCalculation()} for more details about the reason why this check is
     * needed.
     *
     * @return
     */
    @Override
    public boolean canRinScoreBeUsedForOnRiskCalculation() {
        return true;
    }

    @Override
    public Date getPicoRunDate() {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRawRin() {
        return "";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Double getRin() {
        /*
         * Note: This currently always returns null. When changing this, review and update the implementation of
         * {@link canRinScoreBeUsedForOnRiskCalculation()} (and this comment) if necessary.
         */
        return null;
    }

    @Override
    public Double getRqs() {
        return null;
    }

    @Override
    public Double getDv200() {
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

    /**
     * For mercury samples, the root id is considered
     * the same thing as the sample id.
     */
    @Override
    public String getRootSample() {
        return sampleId;
    }

    @Override
    public String getStockSample() {
        return sampleId;
    }

    @Override
    public String getCollection() {
        return "";
    }

    @Override
    public String getCollaboratorsSampleName() {
        return collaboratorSampleId;
    }

    @Override
    public String getContainerId() {
        return "";
    }

    @Override
    public String getPatientId() {
        return patientId;
    }

    @Override
    public String getOrganism() {
        return "";
    }

    @Override
    public boolean getHasSampleKitUploadRackscanMismatch() {
        return false;
    }

    @Override
    public String getSampleLsid() {
        return "";
    }

    /**
     * For mercury samples, the patient id is the
     * collaborator patient id because the only
     * patient id we know is the one given to us
     * by the collaborator.
     */
    @Override
    public String getCollaboratorParticipantId() {
        return patientId;
    }

    @Override
    public String getMaterialType() {
        if (StringUtils.isBlank(materialType)) {
            return "";
        }
        return materialType;
    }

    @Override
    public String getOriginalMaterialType() {
        if (StringUtils.isBlank(originalMaterialType)) {
            return "";
        }
        return originalMaterialType;
    }

    @Override
    public double getTotal() {
        return 0;
    }

    @Override
    public String getSampleType() {
        return tumorNormal;
    }

    @Override
    public String getPrimaryDisease() {
        return "";
    }

    @Override
    public String getGender() {
        return gender;
    }

    @Override
    public String getStockType() {
        return "";
    }

    @Override
    public boolean isSampleReceived() {
        return receiptDate != null;
    }

    @Override
    public Date getReceiptDate() {
        return receiptDate;
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
        return "";
    }

    @Override
    public String getEthnicity() {
        return "";
    }

    @Override
    public String getRace() {
        return "";
    }

    @Override
    public Boolean getFfpeStatus() {
        return null;
    }

    @Override
    public MercurySample.MetadataSource getMetadataSource() {
        return MercurySample.MetadataSource.MERCURY;
    }

    // TODO: decide whether to keep these methods or implement a general get(Metadata.Key)
    public String getCollectionDate() {
        return collectionDate;
    }

    public String getVisit() {
        return visit;
    }
}

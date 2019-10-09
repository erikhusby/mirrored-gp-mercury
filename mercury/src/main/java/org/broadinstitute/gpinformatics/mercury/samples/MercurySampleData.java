package org.broadinstitute.gpinformatics.mercury.samples;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * This class holds sample data specific to MercurySamples whose MetadataSource == MERCURY.
 */
public class MercurySampleData implements SampleData {
    private MercurySample mercurySample;
    private String rootSampleId;
    private String sampleId;
    private String collaboratorSampleId;
    private String patientId;
    private String broadPatientId;
    private String gender;
    private String tumorNormal;

    private String collectionDate;
    private String visit;
    private final boolean hasData;
    private Date receiptDate;
    private String materialType;
    private String originalMaterialType;
    private String species;
    private String sampleLSID;
    private QuantData quantData;

    public MercurySampleData(@Nonnull String sampleId, @Nonnull Set<Metadata> metadata) {
        this(sampleId, metadata, null);
    }

    public MercurySampleData(@Nonnull String sampleId, @Nonnull Set<Metadata> metadata, @Nullable Date receiptDate) {
        this.sampleId = sampleId;
        hasData = !metadata.isEmpty();
        this.receiptDate = receiptDate;
        extractSampleDataFromMetadata(metadata);
    }

    public MercurySampleData(@Nonnull MercurySample mercurySample) {
        this(mercurySample.getSampleKey(), mercurySample.getMetadata(), mercurySample.getReceivedDate());
        this.mercurySample = mercurySample;
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
            case SPECIES:
                this.species = value;
                break;
            case LSID:
                this.sampleLSID = value;
                break;
            case BROAD_PARTICIPANT_ID:
                this.broadPatientId = value;
                break;
            case ROOT_SAMPLE:
                this.rootSampleId = value;
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


    /**
     * Initialize QuantData.
     *
     * @return true if QuantData was successfully initialized.
     */
    boolean initializeQuantData() {
        if (quantData == null) {
            if (mercurySample != null) {
                quantData = new QuantData(mercurySample);
            }
        }
        return quantData != null;
    }

    @Override
    public Date getPicoRunDate() {
        if (initializeQuantData()) {
            return quantData.getPicoRunDate();
        }
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
        if (initializeQuantData()) {
            return quantData.getVolume() == null ? 0 : quantData.getVolume();
        }
        return 0;
    }

    @Override
    public Double getConcentration() {
        if (initializeQuantData()) {
            return quantData.getConcentration();
        }
        return null;
    }

    @Override
    public String getReceptacleType() {
        return null;
    }

    /**
     * Merges in metadata from a metadata inheritance root. Existing values are overwritten.
     */
    public void mergeInheritedMetadata(SampleData other) {
        if (other != null) {
            collaboratorSampleId = other.getCollaboratorsSampleName();
            patientId = other.getCollaboratorParticipantId();
            gender = other.getGender();
            species = other.getOrganism();
        }
    }

    /**
     * A sample may have a root MetadataSource of BSP (i.e. BspSampleData), but have aliquots that are managed by
     * Mercury.  This class returns Mercury quant information.
     */
    public static class QuantData {
        private Date picoRunDate;
        private Double volume;
        private Double concentration;
        private Double totalDna;

        private QuantData() {
        }

        public QuantData(MercurySample mercurySample) {
            if (!mercurySample.getLabVessel().isEmpty()) {
                // A sample with multiple vessels is a data inconsistency that should be fixed before quanting.
                LabVessel labVessel = mercurySample.getLabVessel().iterator().next();
                BigDecimal vesselVolume = labVessel.getVolume();
                if (vesselVolume != null) {
                    volume = vesselVolume.doubleValue();
                }

                List<LabMetric> labMetrics = labVessel.getNearestMetricsOfType(LabMetric.MetricType.INITIAL_PICO,
                        TransferTraverserCriteria.TraversalDirection.Descendants);
                if (labMetrics != null && !labMetrics.isEmpty()) {
                    // Use most recent
                    LabMetric labMetric = labMetrics.get(labMetrics.size() - 1);
                    updateFromLabMetric(labMetric);
                }
            }
        }

        public void updateFromLabMetric(LabMetric labMetric) {
            concentration = labMetric.getValue().doubleValue();
            if (labMetric.getTotalNg() != null) {
                // convert ng to ug
                totalDna = labMetric.getTotalNg().doubleValue()  / 1000.0;
            }
            LabMetricRun labMetricRun = labMetric.getLabMetricRun();

            // Generic uploads don't have runs
            if (labMetricRun == null) {
                picoRunDate = labMetric.getCreatedDate();
            } else {
                picoRunDate = labMetricRun.getRunDate();
            }
        }

        public Date getPicoRunDate() {
            return picoRunDate;
        }

        public Double getVolume() {
            return volume;
        }

        public Double getConcentration() {
            return concentration;
        }

        public Double getTotalDna() {
            return totalDna;
        }
    }

    /**
     * For clinical samples, the root id is considered the same thing as the sample id,
     * so return the sample id as default when the root metadata is not explicitly set.
     */
    @Override
    public String getRootSample() {
        return rootSampleId == null ? sampleId : rootSampleId;
    }

    public void setRootSampleId(String rootSampleId) {
        this.rootSampleId = rootSampleId;
    }

    @Override
    public String getStockSample() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    @Override
    public String getCollection() {
        return "";
    }

    @Override
    public String getCollectionWithoutGroup() {
        return "";
    }

    @Override
    public String getCollectionId() {
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
        return broadPatientId == null ? patientId : broadPatientId;
    }

    @Override
    public String getOrganism() {
        return species;
    }

    @Override
    public boolean getHasSampleKitUploadRackscanMismatch() {
        return false;
    }

    @Override
    public String getSampleLsid() {
        return sampleLSID;
    }

    /**
     * For clinical samples, the patient id is the
     * collaborator patient id because the only
     * patient id we know is the one given to us
     * by the collaborator.
     */
    @Override
    public String getCollaboratorParticipantId() {
        return patientId;
    }

    @Override
    public String getCollaboratorFamilyId() {
        return "";
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
        if (initializeQuantData()) {
            return quantData.getTotalDna() == null ? 0 : quantData.getTotalDna();
        }
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

    @Override
    public void overrideWithQuants(Collection<LabMetric> labMetrics) {
        if (quantData == null) {
            quantData = new QuantData();
        }
        for (LabMetric labMetric : labMetrics) {
            if (labMetric.getName() == LabMetric.MetricType.INITIAL_PICO) {
                quantData.updateFromLabMetric(labMetric);
            }
        }
    }

    @Override
    public void overrideWithSampleInstance(SampleInstanceV2 sampleInstance) {
        tumorNormal = sampleInstance.getSampleType();
    }

    @Override
    public String getSampleKitId() {
        return null;
    }

    @Override
    public String getSampleStatus() {
        return null;
    }
}

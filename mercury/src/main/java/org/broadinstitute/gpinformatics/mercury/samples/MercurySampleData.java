package org.broadinstitute.gpinformatics.mercury.samples;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
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
    private Date picoRunDate;
    private double volume;
    private Double concentration;
    private double totalDna;

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
        QuantData quantData = new QuantData(mercurySample);
        picoRunDate = quantData.getPicoRunDate();
        volume = quantData.getVolume();
        concentration = quantData.getConcentration();
        totalDna = quantData.getTotalDna();
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
        return picoRunDate;
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
        return volume;
    }

    @Override
    public Double getConcentration() {
        return concentration;
    }

    /**
     * A sample may have a root MetadataSource of BSP (i.e. BspSampleData), but have aliquots that are managed by
     * Mercury.  This class returns Mercury quant information.
     */
    public static class QuantData {
        private Date picoRunDate;
        private double volume;
        private Double concentration;
        private double totalDna;

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
                    concentration = labMetric.getValue().doubleValue();
                    totalDna = labMetric.getTotalNg().doubleValue();
                    LabMetricRun labMetricRun = labMetric.getLabMetricRun();

                    // Generic uploads don't have runs
                    if (labMetricRun == null) {
                        picoRunDate = labMetric.getCreatedDate();
                    } else {
                        picoRunDate = labMetricRun.getRunDate();
                    }
                }
            }
        }

        public Date getPicoRunDate() {
            return picoRunDate;
        }

        public double getVolume() {
            return volume;
        }

        public Double getConcentration() {
            return concentration;
        }

        public double getTotalDna() {
            return totalDna;
        }
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
    public double getTotal() {
        return totalDna;
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

package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import java.util.Collection;
import java.util.Date;

/**
 */
public interface SampleData {
    boolean hasData();

    /**
     * Determines whether or not this sample data contains a RIN score that is a valid number that can be used in a risk
     * calculation. Returning false indicates that there is an intent to have a RIN score but no meaningful checks can
     * be made against it. In that case, in order to avoid silently allowing an out-of-spec sample from being processed,
     * work on the sample should probably not proceed.
     *
     * @return true if there is a valid RIN score (including no score); false otherwise
     */
    boolean canRinScoreBeUsedForOnRiskCalculation();

    Date getPicoRunDate();

    /**
     * Returns a string representing the RIN score. This is character rather than numeric data because it is sometimes
     * recorded as a range.
     *
     * @return the raw RIN score
     */
    String getRawRin();

    /**
     * Get the RIN value for the sample. Returns null if no RIN value is set. If the RIN value is expressed as a range,
     * the lower value is returned. Throws NumberFormatException if the value cannot be parsed as a single value or as a
     * range.
     *
     * @return the (lowest) RIN value from BSP; null if no value is set
     * @throws NumberFormatException if the value cannot be parsed as a single value or a range
     *
     * @see #getRawRin() to get the unmodified rin value as a string.
     */
    Double getRin();

    Double getRqs();

    Double getDv200();

    double getVolume();

    Double getConcentration();

    String getRootSample();

    String getStockSample();

    String getCollection();

    String getCollectionWithoutGroup();

    String getCollectionId();

    String getCollaboratorsSampleName();

    String getContainerId();

    String getPatientId();

    String getOrganism();

    boolean getHasSampleKitUploadRackscanMismatch();

    String getSampleLsid();

    String getCollaboratorParticipantId();

    String getCollaboratorFamilyId();

    String getMaterialType();

    String getOriginalMaterialType();

    double getTotal();

    String getSampleType();

    String getPrimaryDisease();

    String getGender();

    String getStockType();

    boolean isSampleReceived();

    Date getReceiptDate();

    boolean isActiveStock();

    String getSampleId();

    MaterialType getMaterialTypeObject();

    String getCollaboratorName();

    String getEthnicity();

    String getRace();

    Boolean getFfpeStatus();

    MercurySample.MetadataSource getMetadataSource();

    void overrideWithQuants(Collection<LabMetric> labMetrics);

    String getSampleKitId();

    String getSampleStatus();

    String getReceptacleType();
}

package org.broadinstitute.gpinformatics.infrastructure;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import java.text.ParseException;
import java.util.Date;

/**
 */
public interface SampleData {
    boolean hasData();

    boolean canRinScoreBeUsedForOnRiskCalculation();

    Date getPicoRunDate();

    String getRawRin();

    Double getRin();

    Double getRqs();

    Double getDv200();

    double getVolume();

    Double getConcentration();

    String getRootSample();

    String getStockSample();

    String getCollection();

    String getCollaboratorsSampleName();

    String getContainerId();

    String getPatientId();

    String getOrganism();

    boolean getHasSampleKitUploadRackscanMismatch();

    String getSampleLsid();

    String getCollaboratorParticipantId();

    String getMaterialType();

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
}

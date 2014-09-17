package org.broadinstitute.gpinformatics.mercury.samples;

import org.broadinstitute.bsp.client.sample.MaterialType;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 */
public class MercurySampleData implements SampleData {
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
        return null;
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
        return null;
    }

    @Override
    public double getTotal() {
        return 0;
    }

    @Override
    public String getSampleType() {
        return null;
    }

    @Override
    public String getPrimaryDisease() {
        return null;
    }

    @Override
    public String getGender() {
        return null;
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
        return null;
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
}

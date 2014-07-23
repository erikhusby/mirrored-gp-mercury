package org.broadinstitute.gpinformatics.athena.boundary.orders;


import java.util.ArrayList;
import java.util.List;

/**
 * Bean used by web service to get billing status
 * for a product order sample
 */
public class PDOSample {

    private ArrayList<String> riskInformation;

    public PDOSample() {
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled) {
        this(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, null, false);
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Boolean onRisk,
                     boolean isRiskCalculated) {
        this.pdoKey = pdoKey;
        this.sampleName = sampleName;
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
        this.onRisk = onRisk;
        this.isRiskCalculated = isRiskCalculated;
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Boolean onRisk) {
        this(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, onRisk, true);
    }

    private String pdoKey;

    private String sampleName;

    private Boolean hasPrimaryPriceItemBeenBilled;

    private Boolean onRisk;

    private boolean isRiskCalculated;

    private List<String> riskCategories;

    public List<String> getRiskCategories() {
        return riskCategories;
    }

    public void setRiskCategories(List<String> riskCategories) {
        this.riskCategories = riskCategories;
    }

    public String getPdoKey() {
        return pdoKey;
    }

    public void setPdoKey(String pdoKey) {
        this.pdoKey = pdoKey;
    }

    public String getSampleName() {
        return sampleName;
    }

    public void setSampleName(String sampleName) {
        this.sampleName = sampleName;
    }

    public Boolean isHasPrimaryPriceItemBeenBilled() {
        return hasPrimaryPriceItemBeenBilled;
    }

    public void setHasPrimaryPriceItemBeenBilled(Boolean hasPrimaryPriceItemBeenBilled) {
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
    }

    public Boolean isOnRisk() {
        return onRisk;
    }

    public void setOnRisk(Boolean onRisk) {
        this.onRisk = onRisk;
    }


    public void setRiskInformation(ArrayList<String> riskInformation) {
        this.riskInformation = riskInformation;
    }

    public ArrayList<String> getRiskInformation() {
        return riskInformation;
    }

    public boolean isRiskCalculated() {
        return isRiskCalculated;
    }

    public void setRiskCalculated(boolean isRiskCalculated) {
        this.isRiskCalculated = isRiskCalculated;
    }
}

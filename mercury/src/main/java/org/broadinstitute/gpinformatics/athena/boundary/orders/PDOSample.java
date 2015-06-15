package org.broadinstitute.gpinformatics.athena.boundary.orders;


import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Bean used by web service to get billing status for a product order sample
 */
public class PDOSample {

    private List<String> riskInformation = new ArrayList<>();

    public PDOSample() {
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Date receiptDate) {
        this(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, null, false, receiptDate);
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Boolean onRisk,
                     boolean isRiskCalculated, Date receiptDate) {
        this.pdoKey = pdoKey;
        this.sampleName = sampleName;
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
        this.onRisk = onRisk;
        this.isRiskCalculated = isRiskCalculated;
        this.receiptDate = receiptDate;
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Boolean onRisk,
                     Date receiptDate) {
        this(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, onRisk, true, receiptDate);
    }

    private String pdoKey;

    private String sampleName;

    private Boolean hasPrimaryPriceItemBeenBilled;

    private Boolean onRisk;

    private boolean isRiskCalculated;

    private List<String> riskCategories;

    private Date receiptDate;

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


    public void setRiskInformation(List<String> riskInformation) {
        this.riskInformation = riskInformation;
    }

    public List<String> getRiskInformation() {
        return riskInformation;
    }

    public boolean isRiskCalculated() {
        return isRiskCalculated;
    }

    public void setRiskCalculated(boolean isRiskCalculated) {
        this.isRiskCalculated = isRiskCalculated;
    }
}

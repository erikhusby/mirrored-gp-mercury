package org.broadinstitute.gpinformatics.athena.boundary.orders;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * Bean used by web service to get billing status
 * for a product order sample
 */
public class PDOSample {

    public PDOSample() {}

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled) {
        this(pdoKey, sampleName, hasPrimaryPriceItemBeenBilled, null);
    }

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled, Boolean onRisk) {
        this.pdoKey = pdoKey;
        this.sampleName = sampleName;
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
        this.onRisk = onRisk;
    }

    private String pdoKey;

    private String sampleName;

    private Boolean hasPrimaryPriceItemBeenBilled;

    private Boolean onRisk;

    private Collection<String> riskCategories;

    public Collection<String> getRiskCategories() {
        return riskCategories;
    }

    public void setRiskCategories(Collection<String> riskCategories) {
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
}

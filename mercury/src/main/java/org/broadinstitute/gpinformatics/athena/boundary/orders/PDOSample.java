package org.broadinstitute.gpinformatics.athena.boundary.orders;


/**
 * Bean used by web service to get billing status
 * for a product order sample
 */
public class PDOSample {

    public PDOSample() {}

    public PDOSample(String pdoKey, String sampleName, Boolean hasPrimaryPriceItemBeenBilled) {
        this.pdoKey = pdoKey;
        this.sampleName = sampleName;
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
    }

    private String pdoKey;

    private String sampleName;

    private Boolean hasPrimaryPriceItemBeenBilled;

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

}

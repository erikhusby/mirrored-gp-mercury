package org.broadinstitute.gpinformatics.athena.boundary.orders;


// todo arz docs
public class PDOSamplePair {

    public PDOSamplePair() {}

    public PDOSamplePair(String pdoKey,String sampleName,Boolean hasPrimaryPriceItemBeenBilled) {
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

    // todo arz figure out how to make this named right and still be bean compliant
    public Boolean isHasPrimaryPriceItemBeenBilled() {
        return hasPrimaryPriceItemBeenBilled;
    }

    public void setHasPrimaryPriceItemBeenBilled(Boolean hasPrimaryPriceItemBeenBilled) {
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
    }

}

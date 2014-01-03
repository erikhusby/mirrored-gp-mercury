package org.broadinstitute.gpinformatics.athena.boundary.orders;


// todo arz docs
public class PDOSamplePair {

    public PDOSamplePair() {}

    public PDOSamplePair(String pdoKey,String sampleName) {
        this.pdoKey = pdoKey;
        this.sampleName = sampleName;
    }

    private String pdoKey;

    private String sampleName;

    private boolean hasPrimaryPriceItemBeenBilled;

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

    public boolean isHasPrimaryPriceItemBeenBilled() {
        return hasPrimaryPriceItemBeenBilled;
    }

    public void setHasPrimaryPriceItemBeenBilled(boolean hasPrimaryPriceItemBeenBilled) {
        this.hasPrimaryPriceItemBeenBilled = hasPrimaryPriceItemBeenBilled;
    }

}

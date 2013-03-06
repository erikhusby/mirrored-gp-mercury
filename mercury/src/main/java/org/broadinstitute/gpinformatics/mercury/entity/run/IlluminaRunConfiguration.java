package org.broadinstitute.gpinformatics.mercury.entity.run;


public class IlluminaRunConfiguration implements RunConfiguration {

    private final int readLength;
    private final boolean paired;

    public IlluminaRunConfiguration(int readLength,boolean isPaired) {

        this.readLength = readLength;
        paired = isPaired;
    }
    
    @Override
    public String getConfigurationName() {
        return getReadLength() + "bp run"; //etc.
    }

    public int getReadLength() {
        return readLength;
    }

    public boolean isPaired() {
        return paired;
    }

    public boolean isIndexed() {
        throw new RuntimeException("Method not yet implemented.");
    }

}

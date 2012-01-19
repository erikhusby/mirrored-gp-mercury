package org.broadinstitute.sequel;


public class IlluminaRunConfiguration implements RunConfiguration {

    public IlluminaRunConfiguration(int readLength,boolean isPaired) {

    }
    
    @Override
    public String getConfigurationName() {
        return getReadLength() + "bp run"; //etc.
    }

    public int getReadLength() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public boolean isPaired() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public boolean isIndexed() {
        throw new RuntimeException("Method not yet implemented.");
    }

}

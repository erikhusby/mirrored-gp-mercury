package org.broadinstitute.gpinformatics.infrastructure;

// todo jmt delete
public class StubSampleMetadata implements SampleMetadata {

    private String alias;

    private String organism;

    private String strain;

    public StubSampleMetadata(String sampleAlias,
                              String organism,
                              String strain) {
        this.alias = sampleAlias;
        this.organism = organism;
        this.strain = strain;
    }

    @Override
    public String getSampleAlias() {
        return alias;
    }

    @Override
    public String getOrganism() {
        return organism;
    }

    @Override
    public String getStrain() {
        return strain;
    }
}

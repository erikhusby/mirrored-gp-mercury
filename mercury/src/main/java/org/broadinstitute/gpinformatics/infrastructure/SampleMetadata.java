package org.broadinstitute.gpinformatics.infrastructure;

/**
 * Root-ish level sample information required for
 * analysis, access control, etc.  BSP-ish sample
 * metadata goes here.  Walk up sequencing would
 * also implement this, backed by a spreadsheet
 * upload of some sort.
 */
// todo jmt delete
public interface SampleMetadata {

    /**
     * What does the collaborator call this sample?
     */
    public String getSampleAlias();

    public String getOrganism();

    public String getStrain();


}

package org.broadinstitute.gpinformatics.mercury.entity.run;

public interface SequencingTechnology {

    public TechnologyName getTechnologyName();

    public enum TechnologyName {
        ILLUMINA_MISEQ,
        ILLUMINA_HISEQ,
        ILLUMINA_HISEQ_2500,
        FOUR54,
        ION_TORRENT,
        PACBIO
        
    }

    public String asText();

}

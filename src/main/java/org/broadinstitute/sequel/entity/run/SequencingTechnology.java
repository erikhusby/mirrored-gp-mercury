package org.broadinstitute.sequel.entity.run;

public interface SequencingTechnology {

    public TECHNOLOGY_NAME getTechnologyName();

    public enum TECHNOLOGY_NAME {
        ILLUMINA_MISEQ,
        ILLUMINA_HISEQ,
        FOUR54,
        ION_TORRENT,
        PACBIO
        
    };
    
    public String asText();

}

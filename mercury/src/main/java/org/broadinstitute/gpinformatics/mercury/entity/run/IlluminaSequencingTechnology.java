package org.broadinstitute.gpinformatics.mercury.entity.run;

public class IlluminaSequencingTechnology implements SequencingTechnology{

    @Override
    public TECHNOLOGY_NAME getTechnologyName() {
        return TECHNOLOGY_NAME.ILLUMINA_HISEQ;
    }

    @Override
    public String asText() {
        return "Illumina HiSeq";
    }
}

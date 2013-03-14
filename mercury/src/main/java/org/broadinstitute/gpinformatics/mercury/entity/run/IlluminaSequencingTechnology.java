package org.broadinstitute.gpinformatics.mercury.entity.run;

public class IlluminaSequencingTechnology implements SequencingTechnology{

    @Override
    public TechnologyName getTechnologyName() {
        return TechnologyName.ILLUMINA_HISEQ;
    }

    @Override
    public String asText() {
        return "Illumina HiSeq";
    }
}

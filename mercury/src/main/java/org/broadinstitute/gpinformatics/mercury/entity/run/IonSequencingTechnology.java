package org.broadinstitute.gpinformatics.mercury.entity.run;

public class IonSequencingTechnology implements SequencingTechnology {

    private int cycleCount;
    
    private CHIP_TYPE chip_type;
    
    /**
     * Dunno the details, but there
     * are different kinds of chips
     */
    public enum CHIP_TYPE {
        CHIP1
    }

    public IonSequencingTechnology(int cycleCount,
                                   CHIP_TYPE chipType) {
        if (cycleCount < 1) {
            throw new RuntimeException("cycleCount must be at least 1.");
        }
        if (chipType == null) {
             throw new NullPointerException("chipType cannot be null.");
        }
        this.cycleCount = cycleCount;
        this.chip_type = chipType;
    }

    public int getCycleCount() {
        return cycleCount;
    }
    
    public CHIP_TYPE getChipType() {
        return chip_type;
    }

    @Override
    public TechnologyName getTechnologyName() {
        return TechnologyName.ION_TORRENT;
    }

    @Override
    public String asText() {
        return cycleCount + " cycles on a " + chip_type + " chip";
    }
}

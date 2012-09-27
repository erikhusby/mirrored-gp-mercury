package org.broadinstitute.gpinformatics.mercury.entity.analysis;

/**
 * How should co-cleaning be done?  At the moment,
 * we just do all tumors and normals from the same
 * patient together.
 */
public class CoCleaningInstructions {

    public enum CoCleaningRule {
        TUMOR_NORMAL_PER_PATIENT
    }

    private CoCleaningRule rule;

    public CoCleaningInstructions(CoCleaningRule rule) {
        if (rule == null) {
            throw new NullPointerException("rule cannot be null");
        }
        this.rule = rule;
    }


    public CoCleaningRule getCoCleaningRule() {
        return rule;
    }

}

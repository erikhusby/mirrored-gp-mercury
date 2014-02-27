package org.broadinstitute.gpinformatics.mercury.entity.bucket;

/**
 * The lab recognizes these types of rework. They refer to them as Type 1, 2 or 3. This will tell the lab
 * what they need to rework and how it effects the rest of the batch.
 */
public enum ReworkLevel {
    ONE_SAMPLE_HOLD_REST_BATCH("Type 1", "Rework one sample and hold up the rest of the batch."),
    ONE_SAMPLE_RELEASE_REST_BATCH("Type 2", "Rework one sample let the rest of the batch proceed. "),
    ENTIRE_BATCH("Type 3", "Rework all samples in the batch.");

    private final String value;
    private final String description;

    ReworkLevel(String value, String description) {
        this.value = value;
        this.description = description;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}

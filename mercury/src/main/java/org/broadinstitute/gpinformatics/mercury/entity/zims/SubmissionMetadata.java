package org.broadinstitute.gpinformatics.mercury.entity.zims;

import org.codehaus.jackson.annotate.JsonProperty;

public class SubmissionMetadata {
    @JsonProperty("key")
    private String key;

    @JsonProperty("value")
    private String value;

    public SubmissionMetadata() {
    }

    public SubmissionMetadata(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}

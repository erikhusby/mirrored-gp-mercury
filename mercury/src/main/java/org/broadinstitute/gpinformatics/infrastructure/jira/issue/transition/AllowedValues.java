package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

/**
 * DTO to accept results from JIRA web service call (not currently used, but mapper throws exception without it).
 */
public class AllowedValues {
    private String self;
    private String value;
    private String id;

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

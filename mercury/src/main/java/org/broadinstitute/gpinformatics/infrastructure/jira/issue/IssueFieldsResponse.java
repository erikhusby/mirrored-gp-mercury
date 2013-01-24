package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import java.io.Serializable;
import java.util.Map;

public class IssueFieldsResponse implements Serializable {

    private String expand;

    private String id;

    private String self;

    private String key;

    private Map<String, Object> fields;

    public IssueFieldsResponse() {
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }
}

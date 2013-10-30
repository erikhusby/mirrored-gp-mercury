package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import java.io.Serializable;
import java.util.Map;

public class IssueResolutionResponse implements Serializable {

    private String expand;

    private String id;

    private String self;

    private String key;

    private Map<String, Resolution> fields;

    public static class Resolution {

        private String id;

        private String self;

        private String name;

        private String description;

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

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
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

    public Map<String, Resolution> getFields() {
        return fields;
    }

    public void setFields(Map<String, Resolution> fields) {
        this.fields = fields;
    }
}

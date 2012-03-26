package org.broadinstitute.sequel.infrastructure.jira.issue;


import org.broadinstitute.sequel.infrastructure.jira.JsonUnderscoreToBlankEnumSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;

public class CreateIssueRequest implements Serializable {

    public static class Fields implements Serializable {

        public static class Project implements Serializable {
            
            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }


        @JsonSerialize(using = JsonUnderscoreToBlankEnumSerializer.class)
        public enum Issuetype {

            // the convention for enum instances is all-caps, but the JIRA 5 REST examples I've seen have specified
            // this value as mixed case.  In my limited experience with the JIRA 5 REST API it has proven to be
            // very sensitive to case.
            Bug,
            SequeL_Project,
            Whole_Exome_HybSel
        }

        
        private Project project;
        
        private String summary;
        
        private String description;

        private Issuetype issuetype;


        public Project getProject() {
            return project;
        }

        public void setProject(Project project) {
            this.project = project;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Issuetype getIssuetype() {
            return issuetype;
        }

        public void setIssuetype(Issuetype issuetype) {
            this.issuetype = issuetype;
        }


        public Fields() {
            this.project = new Project();
        }

    }


    private Fields fields;


    public Fields getFields() {
        return fields;
    }

    public void setFields(Fields fields) {
        this.fields = fields;
    }


    public CreateIssueRequest() {
        this.fields = new Fields();
    }


    public static CreateIssueRequest create(String key, Fields.Issuetype issuetype, String summary, String description) {
        
        CreateIssueRequest ret = new CreateIssueRequest();
        
        Fields fields = ret.getFields();

        fields.getProject().setKey(key);
        fields.setIssuetype(issuetype);
        fields.setSummary(summary);
        fields.setDescription(description);

        return ret;
    }
}

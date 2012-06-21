package org.broadinstitute.sequel.infrastructure.jira.issue;


import org.broadinstitute.sequel.infrastructure.jira.JsonLabopsJiraEnumSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.io.Serializable;
import java.math.BigInteger;

@JsonSerialize
public class CreateIssueRequest  {

    public static class Fields {

        public static class Project {

            public Project() {}

            public Project(String key) {
                if (key == null) {
                    throw new RuntimeException("key cannot be null");
                }
                this.key = key;
            }

            private String key;

            public String getKey() {
                return key;
            }

            public void setKey(String key) {
                this.key = key;
            }
        }


        @JsonSerialize(using = JsonLabopsJiraEnumSerializer.class)
        public enum Issuetype {

            Whole_Exome_HybSel("Whole Exome (HybSel)");

            private final String jiraName;

            private Issuetype(String jiraName) {
                this.jiraName = jiraName;
            }

            public String getJiraName() {
                return jiraName;
            }

            // the convention for enum instances is all-caps, but the JIRA 5 REST examples I've seen have specified
            // this value as mixed case.  In my limited experience with the JIRA 5 REST API it has proven to be
            // very sensitive to case.
        }

        
        private Project project;
        
        private String summary;
        
        private String description;

        private Issuetype issuetype;

        // todo custom json serialization that pays attention to createmeta fetched field
        // ids.  have to vary this @ runtime because jira custom field ids are not instance portable
        private String customfield_10020 = "doofus";

        // todo arz fixme
        private String customfield_10011 = "9999";

        public Project getProject() {
            return project;
        }

        public String getCustomfield_10020() {
            return customfield_10020;
        }

        public void setCustomfield_10020(String customfield_10020) {
            this.customfield_10020 = customfield_10020;
        }

        public String getCustomfield_10011() {
            return customfield_10011;
        }

        public void setCustomfield_10011(String customfield_10011) {
            this.customfield_10011 = customfield_10011;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public String getSummary() {
            return summary;
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


    public static CreateIssueRequest create(String key,
                                            Fields.Issuetype issuetype,
                                            String summary,
                                            String description) {
        
        CreateIssueRequest ret = new CreateIssueRequest();
        
        Fields fields = ret.getFields();

        fields.getProject().setKey(key);
        fields.setIssuetype(issuetype);
        fields.setSummary(summary);
        fields.setDescription(description);

        return ret;
    }
}

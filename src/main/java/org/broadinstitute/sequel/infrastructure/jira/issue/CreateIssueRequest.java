package org.broadinstitute.sequel.infrastructure.jira.issue;


import org.broadinstitute.sequel.infrastructure.jira.JsonLabopsJiraIssueTypeSerializer;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CreateJiraIssueFieldsSerializer;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.sequel.infrastructure.jira.customfields.CustomFieldDefinition;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import java.util.Collection;
import java.util.HashSet;


public class CreateIssueRequest  {

    /**
     * We use a custom serializer here because custom fields are not
     * instance portable.  In other words, the custom field names in a cloned
     * dev instance of jira arent't the same as they are in production,
     * so there's a bit more work here to make sure that tickets
     * which have custom fields can be properly created in dev and prod.
     */
    @JsonSerialize(using = CreateJiraIssueFieldsSerializer.class)
    public static class Fields {

        public static class Project {

            public Project() {

            }

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


        @JsonSerialize(using = JsonLabopsJiraIssueTypeSerializer.class)
        public enum Issuetype  {

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

        private final Collection<CustomField> customFields = new HashSet<CustomField>();

        public Collection<CustomField> getCustomFields() {
            return customFields;
        }

        public Project getProject() {
            return project;
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
        // todo arz move these out to JiraService params
        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10020","Protocol",true),"test protocol"));
        this.fields.customFields.add(new CustomField(new CustomFieldDefinition("customfield_10011","Work Request ID(s)",true),"WR 1 Billion!"));
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

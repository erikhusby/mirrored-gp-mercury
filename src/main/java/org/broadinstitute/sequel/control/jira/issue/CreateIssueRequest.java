package org.broadinstitute.sequel.control.jira.issue;


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


        public static class Issuetype {

            // the convention for enum instances is all-caps, but the JIRA 5 REST examples I've seen have specified
            // this value as mixed case.  In my limited experience with the JIRA 5 REST API it has proven to be
            // very sensitive to case.

            public static final Issuetype BUG = new Issuetype("Bug");

            public static final Issuetype SEQUEL_PROJECT = new Issuetype("SequeL Project");

            private String name;

            private Issuetype() {}

            private Issuetype(String name) {
                if (name == null) {
                     throw new NullPointerException("name cannot be null.");
                }
                this.name = name;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

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
            this.issuetype = new Issuetype();
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
        fields.getIssuetype().setName(issuetype.getName());
        fields.setSummary(summary);
        fields.setDescription(description);

        return ret;
    }
}

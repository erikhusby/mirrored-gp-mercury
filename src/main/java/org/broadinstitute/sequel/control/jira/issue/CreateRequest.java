package org.broadinstitute.sequel.control.jira.issue;


import java.io.Serializable;

public class CreateRequest implements Serializable {

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
            public enum IssuetypeName {
                Bug
            }
            
            private IssuetypeName name;

            public IssuetypeName getName() {
                return name;
            }

            public void setName(IssuetypeName name) {
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


    public CreateRequest() {
        this.fields = new Fields();
    }


    public static CreateRequest create(String key, Fields.Issuetype.IssuetypeName issuetypeName, String summary, String description) {
        
        CreateRequest ret = new CreateRequest();
        
        Fields fields = ret.getFields();

        fields.getProject().setKey(key);
        fields.getIssuetype().setName(issuetypeName);
        fields.setSummary(summary);
        fields.setDescription(description);

        return ret;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.JsonLabopsJiraIssueTypeSerializer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CreateJiraIssueFieldsSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nullable;

/**
 * We use a custom serializer here because custom fields are not
 * instance portable.  In other words, the custom field names in a cloned
 * dev instance of jira arent't the same as they are in production,
 * so there's a bit more work here to make sure that tickets
 * which have custom fields can be properly created in dev and prod.
 */
@JsonSerialize(using = CreateJiraIssueFieldsSerializer.class)
public class CreateFields extends UpdateFields {

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

    public static class Reporter {

        public Reporter() {
        }

        public Reporter(String name) {
            if (name == null) {
                throw new RuntimeException("name cannot be null");
            }

            this.name = name;
        }

        private String name;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonSerialize(using = JsonLabopsJiraIssueTypeSerializer.class)
    public enum ProjectType {

        LCSET_PROJECT_PREFIX("Illumina Library Construction Tracking", "LCSET"),
        Product_Ordering("Product Ordering", "PDO"),
        Research_Projects("Research Projects", "RP");

        private final String projectName;
        private final String keyPrefix;

        private ProjectType(String projectNameIn, String keyPrefixIn) {
            projectName = projectNameIn;
            this.keyPrefix = keyPrefixIn;
        }

        public String getProjectName() {
            return projectName;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }
    }


    @JsonSerialize(using = JsonLabopsJiraIssueTypeSerializer.class)
    public enum IssueType {

        WHOLE_EXOME_HYBSEL("Whole Exome (HybSel)"),
        EXOME_EXPRESS("Exome Express"),
        PRODUCT_ORDER("Product Order"),
        RESEARCH_PROJECT("Research Project");

        private final String jiraName;

        private IssueType(String jiraName) {
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

    private IssueType issueType;

    private Reporter reporter;

    public Project getProject() {
        return project;
    }

    public Reporter getReporter() {
        return reporter;
    }

    public void setReporter(@Nullable Reporter reporter) {
        this.reporter = reporter;
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

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public CreateFields() {
        project = new Project();
        reporter = new Reporter();
    }
}

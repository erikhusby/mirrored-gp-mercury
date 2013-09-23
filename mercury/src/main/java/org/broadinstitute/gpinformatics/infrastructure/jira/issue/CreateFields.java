package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.infrastructure.jira.JsonLabopsJiraIssueTypeSerializer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CreateJiraIssueFieldsSerializer;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

/**
 * We use a custom serializer here because custom fields are not
 * instance portable.  In other words, the custom field names in a cloned
 * dev instance of jira aren't the same as they are in production,
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

        public Reporter(@Nonnull String name) {
            if (name == null) {
                throw new RuntimeException("name cannot be null");
            }

            this.name = name;
        }

        @Nonnull
        private String name;

        @Nonnull
        public String getName() {
            return name;
        }

        public void setName(@Nonnull String name) {
            this.name = name;
        }
    }

    @JsonSerialize(using = JsonLabopsJiraIssueTypeSerializer.class)
    public enum ProjectType {
        LCSET_PROJECT("Illumina Library Construction Tracking", "LCSET"),
        CRSP_LCSET_PROJECT("Illumina Library Construction Tracking", "CLCSET"),
        FCT_PROJECT("Flowcell Tracking", "FCT"),
        PRODUCT_ORDERING("Product Ordering", "PDO"),
        CRSP_PRODUCT_ORDERING("Product Ordering", "CPDO"),
        RESEARCH_PROJECTS("Research Projects", "RP"),
        CRSP_RESEARCH_PROJECTS("Research Projects", "CRP");

        private final String projectName;
        private final String keyPrefix;

        private ProjectType(String projectName, String keyPrefix) {
            this.projectName = projectName;
            this.keyPrefix = keyPrefix;
        }

        public static ProjectType getLcsetProjectType() {
            return Deployment.isCRSP ? CRSP_LCSET_PROJECT :
                    LCSET_PROJECT ;
        }

        public static ProjectType getProductOrderingProductType() {
            return (Deployment.isCRSP) ? CRSP_PRODUCT_ORDERING :
                    PRODUCT_ORDERING;
        }

        public static ProjectType getResearchProjectType() {
            return ((Deployment.isCRSP) ? CRSP_RESEARCH_PROJECTS :
                    RESEARCH_PROJECTS);
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
        EXOME_EXPRESS(Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName()),
        PRODUCT_ORDER("Product Order"),
        CLIA_PRODUCT_ORDER("CLIA Product Order"),
        RESEARCH_PROJECT("Research Project"),
        CLIA_RESEARCH_PROJECT("CLIA Research Project"),
        FLOWCELL("Flowcell"),
        MISEQ("MiSeq");

        private final String jiraName;

        private IssueType(String jiraName) {
            this.jiraName = jiraName;
        }

        public static IssueType getProductOrderIssueType() {
            return (Deployment.isCRSP) ? CLIA_PRODUCT_ORDER :
                    PRODUCT_ORDER;
        }

        public static IssueType getResearchProjectIssueType() {
            return (Deployment.isCRSP) ? CLIA_RESEARCH_PROJECT :
                    RESEARCH_PROJECT;
        }

        public String getJiraName() {
            return jiraName;
        }

        /** Contains the IssueType to use for a given workflow. */
        public static final Map<String, IssueType> MAP_WORKFLOW_TO_ISSUE_TYPE = new HashMap<String, IssueType>() {{
            put(Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), EXOME_EXPRESS);
            put(Workflow.ICE.getWorkflowName(), EXOME_EXPRESS);
        }};
    }


    private Project project;

    private String summary;

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

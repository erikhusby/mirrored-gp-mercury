package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import org.broadinstitute.gpinformatics.infrastructure.jira.NameableTypeJsonSerializer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CreateJiraIssueFieldsSerializer;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jpa.Nameable;
import org.broadinstitute.gpinformatics.infrastructure.security.ApplicationInstance;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.Workflow;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;
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

    public static class ProjectJsonSerializer extends JsonSerializer<Project> {
        @Override
        public void serialize(Project project, JsonGenerator jsonGenerator, SerializerProvider serializerProvider)
                throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeObjectField("key", project.getProjectType().getKeyPrefix());
            jsonGenerator.writeEndObject();
        }
    }

    @JsonSerialize(using = ProjectJsonSerializer.class)
    public static class Project {
        public Project() {
        }

        public Project(@Nonnull ProjectType projectType) {
            this.projectType = projectType;
        }

        private ProjectType projectType;

        public ProjectType getProjectType() {
            return projectType;
        }

        public void setProjectType(ProjectType projectType) {
            this.projectType = projectType;
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

    public enum ProjectType {
        LCSET_PROJECT("Illumina Library Construction Tracking", "LCSET", "CLCSET"),
        FCT_PROJECT("Flowcell Tracking", "FCT", "CFCT"),
        PRODUCT_ORDERING("Product Ordering", "PDO", "CPDO"),
        RESEARCH_PROJECTS("Research Projects", "RP", "CRP"),
        RECEIPT_PROJECT("Sample Receipt Tracking", "RCT"),
        EXTRACTION_PROJECT("Extractions", "XTR");

        private final String projectName;
        private final String keyPrefix;

        ProjectType(String projectName, String keyPrefix) {
            this.projectName = projectName;
            this.keyPrefix = keyPrefix;
        }

        /**
         * Create a project type with one of two prefixes.
         * @param projectName the project name
         * @param keyPrefix the non-CRSP prefix
         * @param crspKeyPrefix the CRSP prefix
         */
        ProjectType(String projectName, String keyPrefix, String crspKeyPrefix) {
            this.projectName = projectName;
            this.keyPrefix = ApplicationInstance.CRSP.isCurrent() ? crspKeyPrefix : keyPrefix;
        }

        // TODO: currently unused. Can use someday to look up prefix in JIRA instead of hardcoding it here.
        public String getProjectName() {
            return projectName;
        }

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public static ProjectType fromKeyPrefix(String keyPrefix) {

            ProjectType foundValue = null;

            for (ProjectType projectType : values()) {
                if (projectType.getKeyPrefix().equals(keyPrefix)) {
                    foundValue = projectType;
                    break;
                }
            }

            return foundValue;
        }
    }

    @JsonSerialize(using = NameableTypeJsonSerializer.class)
    public enum IssueType implements Nameable {
        // jiraName is defined by JIRA and must not be based on Mercury Workflow.
        WHOLE_EXOME_HYBSEL("Whole Exome (HybSel)"),
        EXOME_EXPRESS("Exome Express"),
        PRODUCT_ORDER("Product Order", "CLIA "),
        RESEARCH_PROJECT("Research Project", "CLIA "),
        FLOWCELL("Flowcell"),
        MISEQ("MiSeq"),
        SAMPLE_INITIATION("Sample Initiation"),
        RECEIPT("Receipt"),
        ALLPREP("AllPrep"),
        DNA_EXTRACTION("DNA Extraction"),
        EXTRACTION_OTHER("Extraction (Other)"),
        RNA_EXTRACTION("RNA Extraction");

        private final String jiraName;

        IssueType(String jiraName) {
            this(jiraName, "");
        }

        @Override
        public String getName() {
            return jiraName;
        }

        /**
         * Create an issue type with an optional prefix.
         * @param jiraName the name for the issue type in JIRA
         * @param crspPrefix the prefix for CRSP JIRA, if any
         */
        IssueType(String jiraName, String crspPrefix) {
            this.jiraName = ApplicationInstance.CRSP.isCurrent() ? crspPrefix + jiraName : jiraName;
        }

        /** The name for this Issue Type in JIRA. */
        public String getJiraName() {
            return jiraName;
        }

        /** Contains the IssueType to use for a given workflow. */
        public static final Map<String, IssueType> MAP_WORKFLOW_TO_ISSUE_TYPE = new HashMap<String, IssueType>() {{
            put(Workflow.AGILENT_EXOME_EXPRESS.getWorkflowName(), EXOME_EXPRESS);
            put(Workflow.ICE_EXOME_EXPRESS.getWorkflowName(), EXOME_EXPRESS);
            put(Workflow.ICE_CRSP.getWorkflowName(), EXOME_EXPRESS);
            put(Workflow.ICE.getWorkflowName(), EXOME_EXPRESS);
        }};

        public static IssueType fromJiraName(String jiraName) {
            IssueType foundValue = null;
            for (IssueType issuetype : values()) {
                if (issuetype.getJiraName().equals(jiraName)) {
                    foundValue = issuetype;
                    break;
                }
            }

            return foundValue;
        }
    }


    private final Project project;

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

    public CreateFields(Collection<CustomField> customFields) {
        super(customFields);
        project = new Project();
        reporter = new Reporter();
    }
}

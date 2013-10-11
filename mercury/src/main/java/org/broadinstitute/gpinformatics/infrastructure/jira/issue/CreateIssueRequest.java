package org.broadinstitute.gpinformatics.infrastructure.jira.issue;


import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;

import java.util.Collection;

public class CreateIssueRequest {

    private final CreateFields fields;

    public CreateFields getFields() {
        return fields;
    }

    public CreateIssueRequest(CreateFields.ProjectType projectType,
                              String reporter,
                              CreateFields.IssueType issueType,
                              String summary,
                              Collection<CustomField> customFields) {
        fields = new CreateFields(customFields);

        fields.getProject().setProjectType(projectType);

        if (reporter != null) {
            fields.getReporter().setName(reporter);
        } else {
            fields.setReporter(null);
        }

        fields.setIssueType(issueType);
        fields.setSummary(summary);
    }
}

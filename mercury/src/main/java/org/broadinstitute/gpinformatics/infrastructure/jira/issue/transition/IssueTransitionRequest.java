package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateFields;

import java.util.Collection;


public class IssueTransitionRequest {

    // assumes we only want to update custom fields, which for the current GPLIM-488 and GPLIM-371 use cases is true
    private UpdateFields fields = new UpdateFields();

    private Transition transition;

    private String comment;


    public IssueTransitionRequest(Transition transition) {
        this.transition = transition;
    }


    public IssueTransitionRequest(Transition transition, Collection<CustomField> customFields, String comment) {
        this.transition = transition;
        if (customFields != null) {
            fields.getCustomFields().addAll(customFields);
        }
        this.comment = comment;
    }

    public UpdateFields getFields() {
        return fields;
    }

    public Transition getTransition() {
        return transition;
    }

    public String getComment() {
        return comment;
    }
}

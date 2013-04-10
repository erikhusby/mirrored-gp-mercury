package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.UpdateFields;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

@JsonSerialize(using = IssueTransitionSerializer.class)
public class IssueTransitionRequest {

    // assumes we only want to update custom fields, which for the current GPLIM-488 and GPLIM-371 use cases is true
    private final UpdateFields fields = new UpdateFields();

    private final Transition transition;

    @Nullable
    private final String comment;

    public IssueTransitionRequest(Transition transition, @Nullable String comment) {
        this.transition = transition;
        this.comment = comment;
    }

    public IssueTransitionRequest(Transition transition,
                                  @Nonnull Collection<CustomField> customFields,
                                  @Nullable String comment) {
        this.transition = transition;
        fields.getCustomFields().addAll(customFields);
        this.comment = comment;
    }

    public UpdateFields getFields() {
        return fields;
    }

    public Transition getTransition() {
        return transition;
    }

    @Nullable
    public String getComment() {
        return comment;
    }
}

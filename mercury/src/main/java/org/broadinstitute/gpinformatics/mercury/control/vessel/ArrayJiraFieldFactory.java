package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ArrayJiraFieldFactory extends AbstractBatchJiraFieldFactory {

    public ArrayJiraFieldFactory(@Nonnull LabBatch batch) {
        super(batch, CreateFields.ProjectType.ARRAY_PROJECT);
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {
        Set<CustomField> customFields = new HashSet<>();
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.DESCRIPTION, batch.getBatchDescription()));
        return customFields;
    }

    @Override
    public String generateDescription() {
        return "TBD";
    }

    @Override
    public String getSummary() {
        return "TBD";
    }
}

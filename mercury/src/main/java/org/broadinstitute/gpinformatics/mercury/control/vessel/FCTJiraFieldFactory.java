package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 1/28/13
 *         Time: 5:02 PM
 */
public class FCTJiraFieldFactory extends AbstractBatchJiraFieldFactory {


    public FCTJiraFieldFactory(@Nonnull LabBatch batch) {
        super(batch);
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {
        return Collections.emptySet();
    }

    @Override
    public String generateDescription() {
        return null;
    }

    @Override
    public String getSummary() {
        StringBuilder summary = new StringBuilder();
        for (LabVessel vessel : batch.getStartingBatchLabVessels()) {
            summary.append(vessel.getLabel());
        }
        return summary.toString();
    }

    @Override
    public CreateFields.ProjectType getProjectType() {
        return CreateFields.ProjectType.FCT_PROJECT;
    }
}

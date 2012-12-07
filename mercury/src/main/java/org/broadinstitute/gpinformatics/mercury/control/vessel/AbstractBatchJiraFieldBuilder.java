package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 9:47 AM
 */
public abstract class AbstractBatchJiraFieldBuilder {

    public abstract Collection<CustomField> getCustomFields() throws IOException;

    public abstract String generateDescription();

    public static AbstractBatchJiraFieldBuilder getInstance(CreateFields.ProjectType projectType,
                                                            @Nonnull LabBatch batch,
                                                            AthenaClientService athenaClientService,
                                                            JiraService jiraService) {

        AbstractBatchJiraFieldBuilder builder = null;

        switch (projectType) {
            case LCSET_PROJECT:
                builder = new LCSetJiraFieldBuilder(batch, athenaClientService, jiraService);
        }

        return builder;
    }

}

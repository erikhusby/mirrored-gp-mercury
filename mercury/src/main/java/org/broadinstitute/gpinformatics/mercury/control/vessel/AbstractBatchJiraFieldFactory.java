package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

/**
 * AbstractBatchJiraFieldFactory sets the stage for factory methods to assist in the creation of JIRA tickets related
 * to {@link LabBatch} entities.  The system will potentially have many different ticket types related to a batch.
 * This factory setup will give callers the ability to retrieve the type of factory based simply on the {@link
 * org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType type} of JIRA project.
 * <p/>
 * This setup will give us the Flexibility to extend this functionality to provide different custom field factories
 * based on the combination of {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType}
 * and {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.IssueType} in the future.
 */
public abstract class AbstractBatchJiraFieldFactory {

    protected final LabBatch batch;

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch) {
        this.batch = batch;
    }

    /**
     * Provides the collection of {@link CustomField custom fields} necessary to successfully open a JIRA ticket based
     * on the provided {@link LabBatch Batch} entity.
     *
     * @param submissionFields A {@link Map<String, CustomFieldDefinition>} indexed by the name of the field, of all
     *                         custom fields defined within the instance of JIRA that the system is currently
     *                         interacting with to allow the factory to relate a field value to the specific field for
     *                         which that value is to be associated
     *
     * @return A Collection of all necessary Fields for submission that can be defined from the given {@link LabBatch
     *         entity}
     */
    public abstract Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields);

    /**
     * Descriptions for JIRA tickets can be generated from a number of different values dynamically (the product order
     * and product to name a few).  This method assists in generating those dynamic descriptions.
     *
     * @return A string representing the description for this batch
     */
    public abstract String generateDescription();

    /**
     * This method returns the batch specific summary field for JIRA.
     *
     * @return A String representing the summary for this lab batch
     */
    public abstract String getSummary();

    /**
     * This method returns the project type for the concrete JIRA field factories.
     *
     * @return A {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType} enum
     *         value for the concrete JIRA field factory
     */
    public abstract CreateFields.ProjectType getProjectType();

    /**
     * Provides the user the ability to retrieve a concrete factory class specific to the given Project Type.
     *
     * @param projectType         type of JIRA Project for which the user needs to generate submission values
     * @param batch               an instance of a {@link LabBatch} entity and is the primary source of the data from
     *                            which the custom submission fields will be generated
     * @param athenaClientService infrastructure service which will provide access to entity information found on the
     *                            "Athena" side of the Mercury system (e.g. {@link ProductOrder}s)
     *
     * @return The instance of the JIRA field factory for the given project type
     */
    public static AbstractBatchJiraFieldFactory getInstance(@Nonnull CreateFields.ProjectType projectType,
                                                            @Nonnull LabBatch batch,
                                                            AthenaClientService athenaClientService) {
        AbstractBatchJiraFieldFactory builder;

        switch (projectType) {
        case LCSET_PROJECT:
            builder = new LCSetJiraFieldFactory(batch, athenaClientService);
            break;
        case FCT_PROJECT:
            builder = new FCTJiraFieldFactory(batch);
            break;
        default:
            builder = new LCSetJiraFieldFactory(batch, athenaClientService);
        }

        return builder;
    }

    public static AbstractBatchJiraFieldFactory getInstance(LabBatch batch,
                                                            AthenaClientService athenaClientService) {
        AbstractBatchJiraFieldFactory builder;
        switch (batch.getLabBatchType()) {
        case WORKFLOW:
            builder = new LCSetJiraFieldFactory(batch, athenaClientService);
            break;
        case MISEQ:
        case FCT:
            builder = new FCTJiraFieldFactory(batch);
            break;
        default:
            builder = new LCSetJiraFieldFactory(batch, athenaClientService);
        }
        return builder;
    }
}

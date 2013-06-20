package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;

/**
 * AbstractBatchJiraFieldFactory sets the stage for factory methods to assist in the creation of Jira tickets related
 * to {@link LabBatch} entities.  The system will potentially have many different ticket types related to a batch.
 * This factory setup will give callers the ability to retrieve the type of factory based simply on the {@link
 * org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType type} of Jira project.
 * <p/>
 * This setup will give us the Flexibility to extend this functionality to provide different custom field factories
 * based on the combination of {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.ProjectType}
 * and {@link org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields.IssueType} in the future
 *
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 9:47 AM
 */
public abstract class AbstractBatchJiraFieldFactory {

    protected final LabBatch batch;

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch) {
        this.batch = batch;
    }

    /**
     * Provides the collection of {@link CustomField custom fields} necessary to successfully open a jira ticket based
     * on the provided {@link LabBatch Batch} entity
     *
     * @param submissionFields A Map, indexed by the name of the field, of all custom fields defined within the
     *                         instance
     *                         of Jira that the system is currently interacting with.  This allows the factory to
     *                         relate
     *                         a field value to the specific field for which that value is to be associated
     *
     * @return A Collection of all necessary Fields for submission that can be defined from the given {@link LabBatch
     *         entity}
     */
    public abstract Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields);

    /**
     * Descriptions for Jira tickets can be generated from a number of different values dynamically (the Product order
     * and product to name a few).  This method assists in generating those dynamic descriptions
     *
     * @return A string representing the description for this batch.
     */
    public abstract String generateDescription();

    /**
     * This method returns the batch specific summary field for JIRA.
     *
     * @return A string representing the summary for this lab batch.
     */
    public abstract String getSummary();

    /**
     * Provides the user the ability to retrieve a concrete factory class specific to the given Project Type
     *
     * @param projectType         type of Jira Project for which the user needs to generate submission values.
     * @param batch               an instance of a LabBatch Entity.  This entity is the primary source of the data from
     *                            which the
     *                            custom submission fields will be generated
     * @param athenaClientService infrastructure service which will provide access to entity information found on the
     *                            "Athena" side of the Mercury system
     *                            (e.g. {@link org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder}s)
     *
     * @return The instance of the JIRA field factory for the given project type.
     */
    public static AbstractBatchJiraFieldFactory getInstance(@Nonnull CreateFields.ProjectType projectType,
                                                            @Nonnull LabBatch batch,
                                                            AthenaClientService athenaClientService) {

        AbstractBatchJiraFieldFactory builder = null;

        switch (projectType) {
        case LCSET_PROJECT:
            builder = new LCSetJiraFieldFactory(batch, athenaClientService);
        case FCT_PROJECT:
            builder = new FCTJiraFieldFactory(batch);
        }

        return builder;
    }

}

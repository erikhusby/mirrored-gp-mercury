package org.broadinstitute.gpinformatics.mercury.control.vessel;

import com.sun.javaws.exceptions.InvalidArgumentException;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
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
 * CreateFields.ProjectType type} of Jira project.
 * <p/>
 * This setup will give us the Flexibility to extend this functionality to provide different custom field factories
 * based on the combination of {@link CreateFields.ProjectType}
 * and {@link CreateFields.IssueType} in the future
 */
public abstract class AbstractBatchJiraFieldFactory {

    protected final LabBatch batch;

    private final CreateFields.ProjectType projectType;

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull CreateFields.ProjectType projectType) {
        this.batch = batch;
        this.projectType = projectType;
    }

    /**
     * Provides the collection of {@link CustomField custom fields} necessary to successfully open a JIRA ticket based
     * on the provided {@link LabBatch Batch} entity.
     *
     * @param submissionFields A {@link Map} indexed by the name of the field, of all
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
     * @return A {@link CreateFields.ProjectType} enum
     *         value for the concrete JIRA field factory
     */
    public CreateFields.ProjectType getProjectType() {
        return projectType;
    }

    /**
     * Provides the user the ability to retrieve a concrete factory class specific to the given Project Type.
     *
     *
     * @param projectType         type of JIRA Project for which the user needs to generate submission values
     * @param batch               an instance of a {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch} entity and is the primary source of the data from
     *                            which the custom submission fields will be generated
     * @param productOrderDao
     * @return The instance of the JIRA field factory for the given project type
     */
    public static AbstractBatchJiraFieldFactory getInstance(@Nonnull CreateFields.ProjectType projectType,
                                                            @Nonnull LabBatch batch,
                                                            ProductOrderDao productOrderDao) {
        AbstractBatchJiraFieldFactory builder;

        if (projectType == null) {
            return getInstance(batch, productOrderDao);
        }
        switch (projectType) {
        case FCT_PROJECT:
            builder = new FCTJiraFieldFactory(batch);
            break;
        case EXTRACTION_PROJECT:
            builder = new ExtractionJiraFieldFactory(batch, productOrderDao);
            break;
        case LCSET_PROJECT:
        default:
            builder = new LCSetJiraFieldFactory(batch, productOrderDao);
            break;
        }

        return builder;
    }

    /**
     * This method does not seem to serve a purpose anymore.
     * @param batch
     * @param productOrderDao
     * @return
     */
    @Deprecated
    public static AbstractBatchJiraFieldFactory getInstance(LabBatch batch,
                                                            ProductOrderDao productOrderDao) {
        AbstractBatchJiraFieldFactory builder;
        switch (batch.getLabBatchType()) {
        case MISEQ:
        case FCT:
            builder = new FCTJiraFieldFactory(batch);
            break;
        default:
            throw new IllegalArgumentException("Not enough information was passed to determine the type of Jira ticket"
                                           + " that should be populated");
        }
        return builder;
    }
}

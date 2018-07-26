package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
    protected final CreateFields.ProjectType projectType;
    protected final ProductOrderDao productOrderDao;
    protected final WorkflowConfig workflowConfig;
    // Determines whether to use nearest or earliest sample name in the Jira ticket.
    protected boolean jiraSampleFromNearest = true;

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull CreateFields.ProjectType projectType) {
        this(batch, projectType, null, null);
    }

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull CreateFields.ProjectType projectType,
            ProductOrderDao productOrderDao, WorkflowConfig workflowConfig) {
        this.batch = batch;
        this.projectType = projectType;
        this.productOrderDao = productOrderDao;
        this.workflowConfig = workflowConfig;
    }

    public String buildSamplesListString() {
        return buildSamplesListString(batch, jiraSampleFromNearest);
    }

    /**
     * Builds a string for the JIRA ticket consisting of the sample names
     * from initial and rework samples in the batch.
     */
    public static String buildSamplesListString(LabBatch labBatch, boolean nearestSample) {
        StringBuilder samplesText = new StringBuilder();
        Set<String> newSamples = new TreeSet<>();
        Set<String> reworkSamples = new TreeSet<>();
        for (BucketEntry bucketEntry : labBatch.getBucketEntries()) {
            Collection<String> sampleNames = bucketEntry.getLabVessel().getSampleNames(nearestSample);
            switch (bucketEntry.getEntryType()) {
            case PDO_ENTRY:
                newSamples.addAll(sampleNames);
                break;
            case REWORK_ENTRY:
                reworkSamples.addAll(sampleNames);
                break;
            default:
                throw new IllegalArgumentException("No support for " + bucketEntry.getEntryType());
            }
        }

        samplesText.append(StringUtils.join(newSamples, "\n"));
        samplesText.append("\n");

        if (!reworkSamples.isEmpty()) {
            samplesText.append(StringUtils.join(reworkSamples, "\n"));
            samplesText.append("\n");
        }
        return samplesText.toString();
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
     * Returns a subclass depending on the type of JIRA ticket.
     *
     * @param projectType         type of JIRA Project for which the user needs to generate submission values
     *                            If null, defaults to FCT.
     * @param batch               an instance of a {@link LabBatch} entity and is the primary source of the data from
     *                            which the custom submission fields will be generated
     */
    public static AbstractBatchJiraFieldFactory getInstance(CreateFields.ProjectType projectType,
            @Nonnull LabBatch batch, ProductOrderDao productOrderDao, WorkflowConfig workflowConfig) {

        if (projectType == null || projectType == CreateFields.ProjectType.FCT_PROJECT) {
            switch (batch.getLabBatchType()) {
            case MISEQ:
            case FCT:
                return new FCTJiraFieldFactory(batch);
            default:
                throw new IllegalArgumentException(projectType + " ticket type cannot be used with a " +
                        batch.getLabBatchType() + " batch type.");
            }
        }

        switch (projectType) {
        case EXTRACTION_PROJECT:
            return new ExtractionJiraFieldFactory(batch, productOrderDao, workflowConfig);

        case ARRAY_PROJECT:
            return new ArrayJiraFieldFactory(batch);

        case LCSET_PROJECT:
        default:
            return new LCSetJiraFieldFactory(batch, productOrderDao, workflowConfig);
        }
    }
}

package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashSet;
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

    private final CreateFields.ProjectType projectType;

    public AbstractBatchJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull CreateFields.ProjectType projectType) {
        this.batch = batch;
        this.projectType = projectType;
    }

    /**
     * Returns the unique list of sample names referenced by the given collection of vessels.
     */
    private static Set<String> getUniqueSampleNames(Collection<LabVessel> labVessels) {
        Set<String> sampleNames = new HashSet<>();
        for (LabVessel labVessel : labVessels) {
            Collection<String> sampleNamesForVessel = labVessel.getSampleNames();
            sampleNames.addAll(labVessel.getSampleNames());
        }
        return sampleNames;
    }

    /**
     * Takes the initial samples and the rework samples
     * from the batch and builds a string to display
     * on the batch ticket
     *
     * @param labBatch contains samples
     *
     * @return sample list
     */
    public static String buildSamplesListString(LabBatch labBatch, @Nullable Bucket bucket) {
        StringBuilder samplesText = new StringBuilder();
        Set<String> newSamples = new TreeSet<>();
        Set<String> reworkSamples = new TreeSet<>();
        newSamples.addAll(getUniqueSampleNames(labBatch.getNonReworkStartingLabVessels()));
        reworkSamples.addAll(getUniqueSampleNames(labBatch.getReworks()));

        samplesText.append(StringUtils.join(newSamples, "\n"));
        samplesText.append("\n");

        if (!reworkSamples.isEmpty()) {
            samplesText.append("\n");
            for (String reworkSample : reworkSamples) {
                if (bucket == null) {
                    samplesText.append(reworkSample).append(" (rework)\n");
                } else {
                    samplesText.append(reworkSample).append(" (rework from ").append(bucket.getBucketDefinitionName())
                            .append(
                                    ")\n");
                }
            }
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
            return getInstanceByBatchType(batch);
        }
        switch (projectType) {
        case FCT_PROJECT:
            builder = getInstanceByBatchType(batch);
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
     * @param batch an instance of a {@link org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch} entity
     *              and is the primary source of the data from which the custom submission fields will be generated
     * @return The instance of the JIRA field factory for the given project type
     */
    private static AbstractBatchJiraFieldFactory getInstanceByBatchType(LabBatch batch) {
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

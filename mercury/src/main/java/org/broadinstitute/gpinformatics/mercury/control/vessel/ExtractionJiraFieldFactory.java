/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2015 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.CreateFields;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.Bucket;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Concrete factory implementation specific to creating the custom and required fields for creating an XTR ticket.
 */
public class ExtractionJiraFieldFactory extends AbstractBatchJiraFieldFactory {

    public static final String LIB_QC_SEQ_REQUIRED_DEFAULT = "None";
    public static final String POOLING_STATUS = "Pool w/o Positive Control";
    public static final String PROGRESS_STATUS = "On Track";

    public static final String LIB_QC_SEQ_REQUIRED_NO = "No";
    public static final String LIB_QC_SEQ_REQUIRED_HISEQ = "Yes - HiSeq";
    public static final String LIB_QC_SEQ_REQUIRED_MISEQ = "Yes - MiSeq";
    public static final List<String> ISSUE_TYPES =
            Arrays.asList("AllPrep", "DNA Extraction", "RNA Extraction", "Extraction (Other)");

    private final Map<String, ResearchProject> foundResearchProjectList = new HashMap<>();
    private Map<String, Set<LabVessel>> pdoToVesselMap = new HashMap<>();
    private final Map<String, ProductWorkflowDef> workflowDefs = new HashMap<>();

    private static final Log log = LogFactory.getLog(ExtractionJiraFieldFactory.class);


    /**
     * LCSet Field constructor.  Extracts information that is used by each of the provided factory fields so that the
     * work is only done once.
     *
     * @param batch               instance of the Lab Batch entity for which a new LCSetT Ticket is to be created
     * @param productOrderDao
     */
    public ExtractionJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull ProductOrderDao productOrderDao) {
        super(batch, CreateFields.ProjectType.EXTRACTION_PROJECT);

        WorkflowLoader wfLoader = new WorkflowLoader();
        WorkflowConfig wfConfig = wfLoader.load();


        if (!batch.getBucketEntries().isEmpty()) {
            for (BucketEntry bucketEntry : batch.getBucketEntries()) {
                String pdoKey = bucketEntry.getProductOrder().getBusinessKey();
                if (!pdoToVesselMap.containsKey(pdoKey)) {
                    pdoToVesselMap.put(pdoKey, new HashSet<LabVessel>());
                }
                pdoToVesselMap.get(pdoKey).add(bucketEntry.getLabVessel());
            }
            for (LabVessel rework : batch.getReworks()) {
                for (SampleInstance sampleInstance : rework.getSampleInstances(LabVessel.SampleType.WITH_PDO, null)) {
                    String pdoKey = sampleInstance.getProductOrderKey();
                    if (!pdoToVesselMap.containsKey(pdoKey)) {
                        pdoToVesselMap.put(pdoKey, new HashSet<LabVessel>());
                    }
                    pdoToVesselMap.get(pdoKey).add(rework);
                }
            }
        } else {
            pdoToVesselMap = LabVessel.extractPdoLabVesselMap(batch.getStartingBatchLabVessels());
        }

        for (String currPdo : pdoToVesselMap.keySet()) {
            ProductOrder pdo = productOrderDao.findByBusinessKey(currPdo);

            if (pdo != null) {
                foundResearchProjectList.put(currPdo, pdo.getResearchProject());
            } else {
                //TODO SGM: Throw an exception here (?)
                log.error("Unable to find a PDO for the business key of " + currPdo);
            }
            if (batch.getWorkflowName() != null) {
                workflowDefs.put(currPdo, wfConfig.getWorkflowByName(batch.getWorkflowName()));
            }
        }

    }

    /**
     * Returns the unique list of sample names referenced by the given collection of vessels.
     */
    private static Set<String> getUniqueSampleNames(Collection<LabVessel> labVessels) {
        Set<String> sampleNames = new HashSet<>();
        for (LabVessel labVessel : labVessels) {
            Collection<String> sampleNamesForVessel = labVessel.getSampleNames();
            if (sampleNamesForVessel.size() > 1) {
                throw new RuntimeException("Cannot build samples list for " + labVessel.getLabel()
                                           + " because we're expecting only a single sample within the vessel.");
            }
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
        Set<String> newSamples = new HashSet<>();
        Set<String> reworkSamples = new HashSet<>();
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

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {

        //TODO SGM: Modify Field settings to Append instead of Overwriting.  This would cover associating an Existing Ticket
        Set<CustomField> customFields = new HashSet<>();

        customFields
                .add(new CustomField(submissionFields, LabBatch.TicketFields.DESCRIPTION, batch.getBatchDescription()));
        customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.SUMMARY, batch.getBatchName()));

        int sampleCount = batch.getStartingBatchLabVessels().size();

        customFields.add(new CustomField(
                submissionFields.get(LabBatch.TicketFields.NUMBER_OF_SAMPLES.getName()), sampleCount));

        if (batch.getDueDate() != null) {

            customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.DUE_DATE,
                    JiraService.JIRA_DATE_FORMAT.format(batch.getDueDate())));
        }

        if (batch.getImportant() != null) {
            customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.IMPORTANT, batch
                    .getImportant()));
        }

        return customFields;

    }

    @Override
    public String generateDescription() {

        StringBuilder ticketDescription = new StringBuilder();

        for (Map.Entry<String, Set<LabVessel>> pdoKey : pdoToVesselMap.entrySet()) {

            int sampleCount = 0;

            for (LabVessel currVessel : pdoKey.getValue()) {
                sampleCount += currVessel.getSampleInstanceCount(LabVessel.SampleType.PREFER_PDO, null);
            }

            ticketDescription.append(sampleCount).append(" samples ");
            if (foundResearchProjectList.containsKey(pdoKey.getKey())) {
                ticketDescription.append("from ").append(foundResearchProjectList.get(pdoKey.getKey()).getTitle())
                        .append(" ");
            }
            ticketDescription.append(pdoKey.getKey()).append("\n");
        }
        return ticketDescription.toString();
    }

    @Override
    public String getSummary() {

        StringBuilder summary = new StringBuilder();
        if (StringUtils.isBlank(batch.getBatchName())) {

            for (ResearchProject currProj : foundResearchProjectList.values()) {
                summary.append(currProj.getTitle()).append("; ");
            }

        } else {
            summary.append(batch.getBatchName());
        }

        return summary.toString();
    }
}

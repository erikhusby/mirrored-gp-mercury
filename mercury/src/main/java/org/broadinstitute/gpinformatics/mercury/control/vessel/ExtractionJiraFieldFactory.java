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

import org.apache.commons.collections4.CollectionUtils;
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
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;
import org.jvnet.inflector.Noun;

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
    public ExtractionJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull ProductOrderDao productOrderDao,
            WorkflowConfig workflowConfig) {
        super(batch, CreateFields.ProjectType.EXTRACTION_PROJECT, null, productOrderDao, workflowConfig);

        if (!batch.getBucketEntries().isEmpty()) {
            for (BucketEntry bucketEntry : batch.getBucketEntries()) {
                String pdoKey = bucketEntry.getProductOrder().getBusinessKey();
                if (!pdoToVesselMap.containsKey(pdoKey)) {
                    pdoToVesselMap.put(pdoKey, new HashSet<LabVessel>());
                }
                pdoToVesselMap.get(pdoKey).add(bucketEntry.getLabVessel());
            }
            for (LabVessel rework : batch.getReworks()) {
                for (SampleInstanceV2 sampleInstance : rework.getSampleInstancesV2()) {
                    String pdoKey = sampleInstance.getSingleProductOrderSample().getBusinessKey();
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
                //TODO Throw an exception here (?)
                log.error("Unable to find a PDO for the business key of " + currPdo);
            }
            if (batch.getWorkflowName() != null) {
                workflowDefs.put(currPdo, workflowConfig.getWorkflowByName(batch.getWorkflowName()));
            }
        }

    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {

        //TODO Modify Field settings to Append instead of Overwriting.  This would cover associating an Existing Ticket
        Set<CustomField> customFields = new HashSet<>();

        customFields
                .add(new CustomField(submissionFields, LabBatch.TicketFields.DESCRIPTION, batch.getBatchDescription()));

        int sampleCount = batch.getBucketEntries().size();

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

        if (CollectionUtils.isNotEmpty(batch.getStartingBatchLabVessels())) {
            customFields.add(new CustomField(submissionFields, LabBatch.TicketFields.SAMPLE_IDS,
                    buildSamplesListString()));
        }

        try {
            // this is in a try/catch since getLatestMaterialType calls ServiceAccessUtility which will die
            // when called in tests.
            for (BucketEntry bucketEntry : batch.getBucketEntries()) {
                MaterialType materialType = bucketEntry.getLabVessel().getLatestMaterialType();
                if (materialType!=null) {
                    CustomField materialTypeField = new CustomField(submissionFields, LabBatch.TicketFields.MATERIAL_TYPE,
                                    new CustomField.ValueContainer(materialType.getDisplayName()));
                    customFields.add(materialTypeField);
                    // TODO: the batchtype field will be changed to a multi-select in the near future,
                    // when it does, this code will change.
                    if (!customFields.isEmpty()) {
                        break;
                    }
                }
            }
        } catch (Exception e) {
            log.error("Could not find material types for bucket entries.");
        }
        return customFields;
    }

    @Override
    public String generateDescription() {
        StringBuilder ticketDescription = new StringBuilder();
        for (Map.Entry<String, Set<LabVessel>> pdoKey : pdoToVesselMap.entrySet()) {
            int sampleCount = 0;
            Set<String> materialTypes = new HashSet<>();
            for (LabVessel currVessel : pdoKey.getValue()) {
                sampleCount += currVessel.getSampleInstanceCount();
                MaterialType latestMaterialType = currVessel.getLatestMaterialType();
                if (latestMaterialType != null) {
                    materialTypes.add(latestMaterialType.getDisplayName());
                }
            }
            String projectName = "";
            if (foundResearchProjectList.containsKey(pdoKey.getKey())) {
                projectName = foundResearchProjectList.get(pdoKey.getKey()).getTitle();
            }
            String vesselDescription = String.format("%d %s with material type %s from %s %s", sampleCount,
                    Noun.pluralOf("sample", sampleCount), materialTypes, projectName, pdoKey.getKey());

            ticketDescription.append(vesselDescription).append("\n");
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

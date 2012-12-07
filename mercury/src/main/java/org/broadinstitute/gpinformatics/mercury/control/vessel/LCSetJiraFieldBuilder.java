package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 10:39 AM
 */
public class LCSetJiraFieldBuilder extends AbstractBatchJiraFieldBuilder {

    public static final  String LIB_QC_SEQ_REQUIRED = "None";
    public static final  String POOLING_STATUS      = "Pool w/o Positive Control";
    public static final String PROGRESS_STATUS     = "On Track";

    private final LabBatch            batch;
    private final AthenaClientService athenaClientService;
    private final JiraService         jiraService;

    public LCSetJiraFieldBuilder(@Nonnull LabBatch batch, AthenaClientService athenaClientService,
                                 JiraService jiraService) {
        this.batch = batch;
        this.athenaClientService = athenaClientService;
        this.jiraService = jiraService;
    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields)
            throws IOException {
        Collection<String> pdos = LabVessel.extractPdoList(batch.getStartingLabVessels());

        Set<CustomField> customFields = new HashSet<CustomField>();

        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.WORK_REQUEST_IDS, ""));

        customFields.add(new CustomField(
                submissionFields.get(LabBatch.RequiredSubmissionFields.PROGRESS_STATUS.getFieldName()), PROGRESS_STATUS,
                CustomField.SingleFieldType.RADIO_BUTTON));

        customFields.add(new CustomField(
                submissionFields.get(LabBatch.RequiredSubmissionFields.LIBRARY_QC_SEQUENCING_REQUIRED.getFieldName()),
                LIB_QC_SEQ_REQUIRED, CustomField.SingleFieldType.SINGLE_SELECT));

        StringBuilder sampleList = new StringBuilder();

        for (LabVessel currVessel : batch.getStartingLabVessels()) {
            sampleList.append("\n").append(currVessel.getLabel());
        }

        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.GSSR_IDS,
                                         sampleList.toString()));
        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.NUMBER_OF_SAMPLES,
                                         String.valueOf(batch.getStartingLabVessels().size())));

        customFields.add(new CustomField(
                submissionFields.get(LabBatch.RequiredSubmissionFields.POOLING_STATUS.getFieldName()), POOLING_STATUS,
                CustomField.SingleFieldType.SINGLE_SELECT));

        if (!pdos.isEmpty() && pdos.size() == 1) {
            //TODO SGM:  Validate.  The following assumes that a description is set ONLY when there is one PDO in the batch set
            ProductOrder pdo = athenaClientService.retrieveProductOrderDetails(pdos.iterator().next());

            /*
                        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

                        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.DUE_DATE,dateFormat.format(pdo.)))
            */

            WorkflowLoader wfLoader = new WorkflowLoader();
            WorkflowConfig wfConfig = wfLoader.load();

            ProductWorkflowDef workflowDef = wfConfig.getWorkflowByName(pdo.getProduct().getWorkflowName());

            customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.PROTOCOL,
                                             workflowDef.getEffectiveVersion().getVersion()));
        }

        return customFields;

    }

    @Override
    public String generateDescription() {
        Collection<String> pdos = LabVessel.extractPdoList(batch.getStartingLabVessels());
        String ticketDescription = "";

        if (!pdos.isEmpty() && pdos.size() == 1) {
            //TODO SGM:  Validate.  The following assumes that a description is set ONLY when there is one PDO in the batch set
            ProductOrder pdo = athenaClientService.retrieveProductOrderDetails(pdos.iterator().next());
            ticketDescription = pdo.getResearchProject().getSynopsis();
        }

        return ticketDescription;
    }
}

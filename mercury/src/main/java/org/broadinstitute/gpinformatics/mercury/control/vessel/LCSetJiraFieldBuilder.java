package org.broadinstitute.gpinformatics.mercury.control.vessel;

import clover.org.apache.commons.lang.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
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

    public static final String LIB_QC_SEQ_REQUIRED = "None";
    public static final String POOLING_STATUS      = "Pool w/o Positive Control";
    public static final String PROGRESS_STATUS     = "On Track";

    private final LabBatch            batch;
    private final Set<ResearchProject>    foundResearchProjectList = new HashSet<ResearchProject>();
    private final Set<ProductWorkflowDef> workflowDefs             = new HashSet<ProductWorkflowDef>();
    private final Collection<String> pdos;

    public LCSetJiraFieldBuilder(@Nonnull LabBatch batch, AthenaClientService athenaClientService) {
        this.batch = batch;

        WorkflowLoader wfLoader = new WorkflowLoader();
        WorkflowConfig wfConfig = wfLoader.load();

        pdos = LabVessel.extractPdoList(batch.getStartingLabVessels());

        for (String currPdo : pdos) {
            ProductOrder pdo = athenaClientService.retrieveProductOrderDetails(currPdo);

            workflowDefs.add(wfConfig.getWorkflowByName(pdo.getProduct().getWorkflowName()));
            foundResearchProjectList.add(pdo.getResearchProject());
        }

    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields)
            throws IOException {

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

        if (!workflowDefs.isEmpty()) {
            String builtProtocol = "";
            for (ProductWorkflowDef currWorkflowDef : workflowDefs) {

                if (StringUtils.isNotBlank(builtProtocol)) {
                    builtProtocol += ", ";
                }
                builtProtocol += currWorkflowDef.getName() + ":" + currWorkflowDef.getEffectiveVersion().getVersion();
            }
            customFields
                    .add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.PROTOCOL, builtProtocol));
        }

        return customFields;

    }

    @Override
    public String generateDescription() {

        String ticketDescription = "";

        if (!foundResearchProjectList.isEmpty()) {
            for (ResearchProject currRp : foundResearchProjectList) {
                if (StringUtils.isNotBlank(ticketDescription)) {
                    ticketDescription += "\n";
                }
                ticketDescription += currRp.getSynopsis();
            }
        }

        return ticketDescription;
    }
}

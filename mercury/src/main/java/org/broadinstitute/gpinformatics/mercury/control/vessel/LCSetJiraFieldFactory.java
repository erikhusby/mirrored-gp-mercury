package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.athena.AthenaClientService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.mercury.control.workflow.WorkflowLoader;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.ProductWorkflowDef;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.WorkflowConfig;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * Concrete factory implementation specific to creating the custom and required fields for creating an LCSET ticket
 *
 * @author Scott Matthews
 *         Date: 12/7/12
 *         Time: 10:39 AM
 */
public class LCSetJiraFieldFactory extends AbstractBatchJiraFieldFactory {

    public static final String LIB_QC_SEQ_REQUIRED_DEFAULT = "None";
    public static final String POOLING_STATUS = "Pool w/o Positive Control";
    public static final String PROGRESS_STATUS = "On Track";

    public static final String LIB_QC_SEQ_REQUIRED_NO = "No";
    public static final String LIB_QC_SEQ_REQUIRED_HISEQ = "Yes - HiSeq";
    public static final String LIB_QC_SEQ_REQUIRED_MISEQ = "Yes - MiSeq";



    private final Map<String, ResearchProject> foundResearchProjectList = new HashMap<String, ResearchProject>();
    private Map<String, Set<LabVessel>> pdoToVesselMap = new HashMap<String, Set<LabVessel>>();
    private final Map<String,ProductWorkflowDef> workflowDefs = new HashMap<String,ProductWorkflowDef>();
//    private final Collection<String> pdos;

    private final static Log logger = LogFactory.getLog(LCSetJiraFieldFactory.class);


    /**
     * LCSet Field constructor.  Extracts information that is used by each of the provided factory fields so that the
     * work is only done once.
     *
     * @param batch               instance of the Lab Batch entity for which a new LCSetT Ticket is to be created
     * @param athenaClientService Infrastructure service to allow this factory the ability to retrieve
     *                            {@link ProductOrder} information for the {@link LabVessel}s that comprise the
     *                            batch entity
     */
    public LCSetJiraFieldFactory(@Nonnull LabBatch batch, @Nonnull AthenaClientService athenaClientService) {
        super(batch);

        WorkflowLoader wfLoader = new WorkflowLoader();
        WorkflowConfig wfConfig = wfLoader.load();

        pdoToVesselMap = LabVessel.extractPdoLabVesselMap(batch.getStartingLabVessels());


        for (String currPdo : pdoToVesselMap .keySet()) {
            ProductOrder pdo = athenaClientService.retrieveProductOrderDetails(currPdo);

            if (pdo != null) {
                if (pdo.getProduct().getWorkflowName() != null) {
                    workflowDefs.put(currPdo, wfConfig.getWorkflowByName(pdo.getProduct().getWorkflowName()));
                }
                foundResearchProjectList.put(currPdo, pdo.getResearchProject());
            } else {
                //TODO SGM: Throw an exception here (?)
                logger.error("Unable to find a PDO for the business key of " + currPdo);
            }
        }

    }

    @Override
    public Collection<CustomField> getCustomFields(Map<String, CustomFieldDefinition> submissionFields) {

        //TODO SGM: Modify Field settings to Append instead of Overwriting.  This would cover associating an Existing Ticket

        Set<CustomField> customFields = new HashSet<CustomField>();

        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.WORK_REQUEST_IDS, "N/A"));

        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.PROGRESS_STATUS,
                new CustomField.ValueContainer(PROGRESS_STATUS)));

        customFields.add(new CustomField(
                submissionFields.get(LabBatch.RequiredSubmissionFields.LIBRARY_QC_SEQUENCING_REQUIRED.getFieldName()),
                new CustomField.SelectOption(LIB_QC_SEQ_REQUIRED_DEFAULT)));

        StringBuilder sampleList = new StringBuilder();

        int sampleCount = batch.getReworks().size() + batch.getStartingLabVessels().size();

        customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.GSSR_IDS,new LcSetSampleFieldUpdater().buildSamplesListString(batch)));

        customFields.add(new CustomField(submissionFields.get(LabBatch.RequiredSubmissionFields.NUMBER_OF_SAMPLES.getFieldName()), sampleCount ));

        if (batch.getDueDate() != null) {

            customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.DUE_DATE,
                    JiraService.JIRA_DATE_FORMAT.format(batch.getDueDate())));
        }

        if (batch.getImportant() != null) {
            customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.IMPORTANT, batch
                    .getImportant()));
        }

        if (!workflowDefs.isEmpty()) {
            String builtProtocol = "";
            for (ProductWorkflowDef currWorkflowDef : workflowDefs.values()) {

                if (StringUtils.isNotBlank(builtProtocol)) {
                    builtProtocol += ", ";
                }
                builtProtocol += currWorkflowDef.getName() + ":" + currWorkflowDef.getEffectiveVersion().getVersion();
            }
            customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.PROTOCOL,
                    builtProtocol));
        } else {
            customFields.add(new CustomField(submissionFields, LabBatch.RequiredSubmissionFields.PROTOCOL, "N/A"));
        }

        return customFields;

    }

    @Override
    public String generateDescription() {

        StringBuilder ticketDescription = new StringBuilder();

        for (Map.Entry<String, Set<LabVessel>> pdoKey:pdoToVesselMap.entrySet()) {

            int sampleCount = 0;

            for(LabVessel currVessel: pdoKey.getValue()) {
                sampleCount += currVessel.getSampleInstanceCount();
            }

            ticketDescription.append(sampleCount).append(" samples ");
            if(foundResearchProjectList.containsKey(pdoKey.getKey()) ) {
                ticketDescription.append("from ").append(foundResearchProjectList.get(pdoKey.getKey()).getTitle()).append(" ");
            }
            ticketDescription.append(pdoKey.getKey()).append("\n");
        }
        return ticketDescription.toString();
    }
}

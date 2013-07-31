package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.inject.Inject;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This instance of the message handler supports messages that have flowcells for target vessels.
 * FlowcellMessageHandler takes care of updating the FCT ticket associated with creating a Flowcell with the Flowcell
 * barcode and other related information.
 */
public class FlowcellMessageHandler extends AbstractEventHandler {

    private static final Log LOG = LogFactory.getLog(FlowcellMessageHandler.class);

    @Inject
    private JiraService jiraService;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AppConfig appConfig;

    public FlowcellMessageHandler() {
    }

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {

        IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(targetEvent.getTargetLabVessels().iterator().next(),
                IlluminaFlowcell.class);

        Collection<LabBatch> flowcellBatches = null;

        Set<LabBatch> batchesToUpdate = new HashSet<>();

        if (flowcell.getFlowcellType() == IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
            flowcellBatches = flowcell.getAllLabBatches(LabBatch.LabBatchType.MISEQ);
        } else {
            flowcellBatches = flowcell.getAllLabBatches(LabBatch.LabBatchType.FCT);
        }

        if (flowcellBatches.isEmpty()) {
            final String emptyBatchListMessage = "Unable to find any Flowcell batch tickets for " + flowcell.getLabel();
            LOG.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
                    emptyBatchListMessage);
            return;
        }

        Map<VesselPosition, LabVessel> loadedVesselsAndPosition = flowcell.getNearestTubeAncestorsForLanes();

        if (flowcellBatches.size() > 1) {
            if (flowcell.getFlowcellType() == IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
                final String emptyBatchListMessage = "There are two many MiSeq Flowcell batch tickets for " +
                                                     flowcell.getLabel() + " to determine which one to update";
                LOG.error(emptyBatchListMessage);
                emailSender.sendHtmlEmail(appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
                        emptyBatchListMessage);
                return;
            } else {
                for (Map.Entry<VesselPosition, LabVessel> loadingVesselByPosition : loadedVesselsAndPosition
                        .entrySet()) {
                    for (LabBatch currentFlowcellBatch : flowcellBatches) {
                        for (LabBatchStartingVessel currentLaneInfo : currentFlowcellBatch
                                .getLabBatchStartingVessels()) {
                            if (null != currentLaneInfo.getDilutionVessel() && currentLaneInfo.getDilutionVessel()
                                    .getLabel()
                                    .equals(loadingVesselByPosition.getValue().getLabel())) {
                                batchesToUpdate.add(currentFlowcellBatch);
                            }
                        }
                    }
                }
            }
        } else {
            batchesToUpdate.add(flowcellBatches.iterator().next());
        }

        if (batchesToUpdate.isEmpty()) {
            final String emptyBatchListMessage = "Unable to find any Flowcell batch tickets for " + flowcell.getLabel();
            LOG.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
                    emptyBatchListMessage);
            return;
        } else {
            try {
                for (LabBatch currentUpdateBatch : batchesToUpdate) {

                    Set<CustomField> updateFields = new HashSet<>();

                    Map<String, CustomFieldDefinition> summaryFieldDefinition =
                            jiraService.getCustomFields(LabBatch.TicketFields.SUMMARY.getFieldName(),
                                    LabBatch.TicketFields.SEQUENCING_STATION.getFieldName());

                    updateFields.add(new CustomField(summaryFieldDefinition, LabBatch.TicketFields.SUMMARY,
                            flowcell.getLabel()));
                    updateFields.add(new CustomField(summaryFieldDefinition, LabBatch.TicketFields.SEQUENCING_STATION,
                            flowcell.getFlowcellType().getSequencerModel()));
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(), updateFields);
                }

            } catch (IOException e) {
                LOG.error("Error connecting to Jira: " + e.getMessage() +
                          " while trying to update an FCT ticket for " + flowcell.getLabel());
            }
        }
    }

    public void setJiraService(JiraService jiraService) {
        this.jiraService = jiraService;
    }

    public void setEmailSender(EmailSender emailSender) {
        this.emailSender = emailSender;
    }

    public void setAppConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }
}

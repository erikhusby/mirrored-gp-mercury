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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This instance of the message handler supports messages that have flowcells for target vessels.
 * FlowcellMessageHandler takes care of updating the FCT ticket associated with creating a Flowcell with the Flowcell
 * barcode and other related information.
 */
public class FlowcellMessageHandler extends AbstractEventHandler {

    private static final Log logger = LogFactory.getLog(FlowcellMessageHandler.class);

    @Inject
    private JiraService jiraService;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AppConfig appConfig;

    public FlowcellMessageHandler() {
    }

    /**
     * <p/>
     * This method will attempt to find the one FCT ticket that it can for the flowcell being processed and set the
     * summary of that ticket to the flowcell barcode name.  Depending on the {@link LabBatch#labBatchType} this may or
     * may not be straight forward.
     * <p/>
     * If the system is able to uniquely find the FCT for the flowcell (either because there is only one or due to some
     * unique identifier that is known) that ticket can be updated with the flowcel barcode.  If the system is
     * unable to find one unique FCT ticket to update, an error is logged and an email is sent to the recipients of
     * the workflow validation email.
     *
     * @param targetEvent Lab event created with the processed bettalims message.  From here, we can accessed the
     *                    vessels that were referenced/created for the incoming message.
     * @param stationEvent Event Type representation of the bettalims message.  From here, we can access any extra data
     */
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
            logger.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
                    emptyBatchListMessage);
            return;
        }

        Map<VesselPosition, LabVessel> loadedVesselsAndPosition;
        LabVessel sourceLabVessel = targetEvent.getSourceLabVessels().iterator().next();
        if (sourceLabVessel.getType() == LabVessel.ContainerType.STRIP_TUBE) {
            loadedVesselsAndPosition = new HashMap<>();
            // position is arbitrary in strip tube case
            loadedVesselsAndPosition.put(VesselPosition.A01, sourceLabVessel);
        } else {
            loadedVesselsAndPosition = flowcell.getNearestTubeAncestorsForLanes();
        }

        if (flowcellBatches.size() > 1) {
            if (flowcell.getFlowcellType() == IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
                final String emptyBatchListMessage = "There are two many MiSeq Flowcell batch tickets for " +
                                                     flowcell.getLabel() + " to determine which one to update";
                logger.error(emptyBatchListMessage);
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
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
            logger.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), "[Mercury] Failed update FCT Ticket",
                    emptyBatchListMessage);
            return;
        } else {
            try {
                for (LabBatch currentUpdateBatch : batchesToUpdate) {

                    Map<String, CustomFieldDefinition> summaryFieldDefinition =
                            jiraService.getCustomFields(LabBatch.TicketFields.SUMMARY.getName(),
                                    LabBatch.TicketFields.SEQUENCING_STATION.getName());

                    final CustomField summaryCustomField = new CustomField(summaryFieldDefinition, LabBatch.TicketFields.SUMMARY,
                            flowcell.getLabel());
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(), Collections.singleton(summaryCustomField));

                    final CustomField sequencingStationCustomField =
                            new CustomField(summaryFieldDefinition, LabBatch.TicketFields.SEQUENCING_STATION,
                                    flowcell.getFlowcellType().getSequencingStationName(), stationEvent.getStation());
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(), Collections.singleton(sequencingStationCustomField));
                }

            } catch (Exception e) {
                // todo jmt flowcell transfer takes plate on Cbot, so machine name doesn't match drop-down (sequencers) in JIRA.
                logger.error("Error connecting to Jira: " + e.getMessage() +
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

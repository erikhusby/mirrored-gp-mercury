package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomField;
import org.broadinstitute.gpinformatics.infrastructure.jira.customfields.CustomFieldDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.LabVesselSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;

import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This instance of the message handler supports messages that have flowcells for target vessels.
 * FlowcellMessageHandler takes care of updating the FCT ticket associated with creating a Flowcell with the Flowcell
 * barcode and other related information.
 */
@Dependent
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

        Set<LabBatch> batchesToUpdate = getLabBatches(flowcell, emailSender, appConfig);
        if (batchesToUpdate.isEmpty()) {
            return;
        }

        try {
            for (LabBatch currentUpdateBatch : batchesToUpdate) {
                Map<String, CustomFieldDefinition> mapNameToField = jiraService.getCustomFields(
                        LabBatch.TicketFields.SUMMARY.getName(), LabBatch.TicketFields.SEQUENCING_STATION.getName(),
                        LabBatch.TicketFields.CLUSTER_STATION.getName());

                CustomField summaryCustomField = new CustomField(mapNameToField, LabBatch.TicketFields.SUMMARY,
                        flowcell.getLabel());
                jiraService.updateIssue(currentUpdateBatch.getBusinessKey(), Collections.singleton(summaryCustomField));

                // Do this in FlowcellLoaded if there's a strip tube B ancestor, because the strip tube to flowcell
                // transfer happens on a CBot, not a sequencer
                LabVessel sourceLabVessel = targetEvent.getSourceLabVessels().iterator().next();
                if (sourceLabVessel.getType() != LabVessel.ContainerType.STRIP_TUBE) {
                    CustomField sequencingStationCustomField =
                            new CustomField(mapNameToField, LabBatch.TicketFields.SEQUENCING_STATION,
                                    flowcell.getFlowcellType().getSequencingStationName(), stationEvent.getStation());
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(),
                            Collections.singleton(sequencingStationCustomField));
                } else {
                    CustomField clusterStationCustomField =
                            new CustomField(mapNameToField, LabBatch.TicketFields.CLUSTER_STATION,
                                    "cBot", stationEvent.getStation());
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(),
                            Collections.singleton(clusterStationCustomField));
                }
            }

        } catch (Exception e) {
            logger.error("Error connecting to Jira: " + e.getMessage() +
                      " while trying to update an FCT ticket for " + flowcell.getLabel());
        }
    }

    /**
     * Get the FCT batches that pertain to the ancestor (dilution tube or strip tube) of this flowcell.
     */
    static Set<LabBatch> getLabBatches(IlluminaFlowcell flowcell, EmailSender emailSender, AppConfig appConfig) {
        List<LabBatch> flowcellBatches = new ArrayList<>();

        Set<LabBatch> batchesToUpdate = new HashSet<>();

        LabBatch.LabBatchType labBatchType = null;
        if (flowcell.getFlowcellType() == IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
            labBatchType = LabBatch.LabBatchType.MISEQ;
        } else {
            labBatchType = LabBatch.LabBatchType.FCT;
        }

        LabVesselSearchDefinition.VesselBatchTraverserCriteria
                downstreamBatchFinder = new LabVesselSearchDefinition.VesselBatchTraverserCriteria();
        flowcell.getContainerRole().applyCriteriaToAllPositions(
                downstreamBatchFinder, TransferTraverserCriteria.TraversalDirection.Ancestors);

        for (LabBatch labBatch: downstreamBatchFinder.getLabBatches()) {
            if(labBatch.getLabBatchType() == labBatchType) {
                flowcellBatches.add(labBatch);
            }
        }

        if (flowcellBatches.isEmpty()) {
            String emptyBatchListMessage = "Unable to find any Flowcell batch tickets for " + flowcell.getLabel();
            logger.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                    "[Mercury] Failed update FCT Ticket", emptyBatchListMessage, false, true);
            return batchesToUpdate;
        }

        Map<VesselPosition, LabVessel> loadedVesselsAndPosition;
        List<LabVessel.VesselEvent> ancestors = flowcell.getContainerRole().getAncestors(VesselPosition.LANE1);
        if (ancestors.get(0).getSourceVesselContainer() != null &&
                ancestors.get(0).getSourceVesselContainer().getEmbedder().getType() == LabVessel.ContainerType.STRIP_TUBE) {
            loadedVesselsAndPosition = new HashMap<>();
            // position is arbitrary in strip tube case
            loadedVesselsAndPosition.put(VesselPosition.A01, ancestors.get(0).getSourceVesselContainer().getEmbedder());
        } else {
            loadedVesselsAndPosition = flowcell.getNearestTubeAncestorsForLanes();
        }

        if (flowcellBatches.size() > 1) {
            if (flowcell.getFlowcellType() == IlluminaFlowcell.FlowcellType.MiSeqFlowcell) {
                String emptyBatchListMessage = "There are too many MiSeq Flowcell batch tickets for " +
                        flowcell.getLabel() + " to determine which one to update";
                logger.error(emptyBatchListMessage);
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                        "[Mercury] Failed update FCT Ticket", emptyBatchListMessage, false, true);
                return batchesToUpdate;
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
            String emptyBatchListMessage = "Unable to find any Flowcell batch tickets for " + flowcell.getLabel();
            logger.error(emptyBatchListMessage);
            emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(), Collections.<String>emptyList(),
                    "[Mercury] Failed update FCT Ticket", emptyBatchListMessage, false, true);
        }
        return batchesToUpdate;
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

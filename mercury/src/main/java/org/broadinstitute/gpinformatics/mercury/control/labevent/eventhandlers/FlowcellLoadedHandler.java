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
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Handler for FlowcellLoaded message.  For flowcells that are descendants of strip tubes, sets the machine name in
 * the FCT ticket (for other flowcells, the machine name is set by FlowcellMessageHandler).
 */
@Dependent
public class FlowcellLoadedHandler extends AbstractEventHandler {

    private static final Log logger = LogFactory.getLog(FlowcellLoadedHandler.class);

    @Inject
    private JiraService jiraService;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AppConfig appConfig;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(targetEvent.getInPlaceLabVessel(), IlluminaFlowcell.class);

        Set<LabBatch> batchesToUpdate = FlowcellMessageHandler.getLabBatches(flowcell, emailSender,
                appConfig);
        if (batchesToUpdate.isEmpty()) {
            return;
        }

        try {
            for (LabBatch currentUpdateBatch : batchesToUpdate) {
                Map<String, CustomFieldDefinition> mapNameToField = jiraService.getCustomFields(
                        LabBatch.TicketFields.SEQUENCING_STATION.getName());

                // This was already done in FlowcellMessageHandler if there was no strip tube B ancestor
                List<LabVessel.VesselEvent> ancestors = flowcell.getContainerRole().getAncestors(VesselPosition.LANE1);
                VesselContainer<?> vesselContainer = ancestors.get(0).getSourceVesselContainer();
                if (vesselContainer != null && vesselContainer.getEmbedder().getType() ==
                        LabVessel.ContainerType.STRIP_TUBE) {
                    CustomField sequencingStationCustomField = new CustomField(mapNameToField,
                            LabBatch.TicketFields.SEQUENCING_STATION,
                            flowcell.getFlowcellType().getSequencingStationName(), stationEvent.getStation());
                    jiraService.updateIssue(currentUpdateBatch.getBusinessKey(),
                            Collections.singleton(sequencingStationCustomField));
                }
            }

        } catch (Exception e) {
            logger.error("Error connecting to Jira: " + e.getMessage() +
                    " while trying to update an FCT ticket for " + flowcell.getLabel());
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

package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Finds pending infinium chip runs and forwards them on to analysis.
 */
@RequestScoped
@Stateless
public class InfiniumRunFinder implements Serializable {
    private static final Log log = LogFactory.getLog(InfiniumRunFinder.class);

    @Inject
    private InfiniumRunProcessor infiniumRunProcessor;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private InfiniumPipelineClient infiniumPipelineClient;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AppConfig appConfig;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private UserBean userBean;

    public void find() {
        List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissing(LabEventType.INFINIUM_XSTAIN,
                        LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
        for (LabVessel labVessel : infiniumChips) {
            try {
                if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                    StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                    processChip(staticPlate);
                }
            } catch (Exception e) {
                log.error("Failed to process chip " + labVessel.getLabel());
                emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(),
                        "[Mercury] Failed to process infinium chip", "For " + labVessel.getLabel() +
                                                                     " with error: " + e.getMessage());
            }
        }
    }

    private void processChip(StaticPlate staticPlate) throws Exception {
        InfiniumRunProcessor.ChipWellResults chipWellResults = infiniumRunProcessor.process(staticPlate);
        LabEvent someStartedEvent = findOrCreateSomeStartedEvent(staticPlate);
        Set<LabEventMetadata> labEventMetadata = someStartedEvent.getLabEventMetadatas();
        boolean allComplete = true;
        for (VesselPosition vesselPosition : chipWellResults.getPositionWithSampleInstance()) {
            boolean autocallStarted = false;
            for (LabEventMetadata metadata : labEventMetadata) {
                if (metadata.getLabEventMetadataType() ==
                    LabEventMetadata.LabEventMetadataType.AutocallStarted) {
                    if (metadata.getValue().equals(vesselPosition.name())) {
                        autocallStarted = true;
                        break;
                    }
                }
            }

            if (!autocallStarted && chipWellResults.getWellCompleteMap().get(vesselPosition)) {
                if (callStarterOnWell(staticPlate, vesselPosition)) {
                    LabEventMetadata newMetadata = new LabEventMetadata();
                    newMetadata.setLabEventMetadataType(LabEventMetadata.LabEventMetadataType.AutocallStarted);
                    newMetadata.setValue(vesselPosition.name());
                    someStartedEvent.addMetadata(newMetadata);
                } else {
                    allComplete = false;
                }
            }
        }
        if (allComplete) {
            createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
        }
    }

    private boolean callStarterOnWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        return infiniumPipelineClient.callStarterOnWell(staticPlate, vesselPosition);
    }

    private LabEvent findOrCreateSomeStartedEvent(StaticPlate staticPlate) throws Exception {
        LabEvent labEvent = findLabEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_SOME_STARTED);
        if (labEvent == null) {
            labEvent = createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_SOME_STARTED);
        }

        return labEvent;
    }

    private LabEvent findLabEvent(StaticPlate staticPlate, LabEventType eventType) {
        for (LabEvent labEvent: staticPlate.getInPlaceLabEvents()) {
            if (labEvent.getLabEventType() == eventType) {
                return labEvent;
            }
        }
        return null;
    }

    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    private LabEvent createEvent(StaticPlate staticPlate, LabEventType eventType) throws Exception {
        BettaLIMSMessage bettaLIMSMessage = new BettaLIMSMessage();
        bettaLIMSMessage.setMode(LabEventFactory.MODE_MERCURY);
        Date start = new Date();
        PlateEventType plateEventType = new PlateEventType();
        plateEventType.setOperator("seqsystem");
        plateEventType.setProgram(LabEvent.UI_PROGRAM_NAME);
        plateEventType.setDisambiguator(1L);
        plateEventType.setStart(start);
        plateEventType.setEventType(eventType.getName());
        PlateType plateType = new PlateType();
        plateType.setBarcode(staticPlate.getLabel());
        plateType.setPhysType(staticPlate.getPlateType().getAutomationName());
        plateType.setSection("ALL96");
        plateEventType.setPlate(plateType);
        bettaLIMSMessage.getPlateEvent().add(plateEventType);

        BspUser bspUser = getBspUser("seqsystem");
        long operator = bspUser.getUserId();
        LabEvent labEvent = new LabEvent(eventType, start, LabEvent.UI_PROGRAM_NAME, 1L, operator, LabEvent.UI_PROGRAM_NAME);
        staticPlate.addInPlaceEvent(labEvent);
        labEventDao.persist(labEvent);
        labEventDao.flush();

        ObjectMarshaller<BettaLIMSMessage> bettaLIMSMessageObjectMarshaller =
                new ObjectMarshaller<>(BettaLIMSMessage.class);
        bettaLimsMessageResource.storeAndProcess(bettaLIMSMessageObjectMarshaller.marshal(bettaLIMSMessage));
        return findLabEvent(staticPlate, eventType);
    }

    private BspUser getBspUser(String operator) {
        BspUser bspUser = bspUserList.getByUsername(operator);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find operator " + operator);
        }
        login(operator);
        return bspUser;
    }

    private void login(String operator) {
        userBean.login(operator);
    }

    public void setInfiniumRunProcessor(
            InfiniumRunProcessor infiniumRunProcessor) {
        this.infiniumRunProcessor = infiniumRunProcessor;
    }

    public void setInfiniumPipelineClient(
            InfiniumPipelineClient infiniumPipelineClient) {
        this.infiniumPipelineClient = infiniumPipelineClient;
    }

    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }
}

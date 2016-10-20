package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Finds pending infinium chip runs and forwards them on to analysis.
 */
@Singleton
@TransactionManagement(value= TransactionManagementType.BEAN)
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
    private LabEventDao labEventDao;

    @Inject
    private EmailSender emailSender;

    @Inject
    private AppConfig appConfig;

    @Inject
    private BSPUserList bspUserList;

    @Resource
    private EJBContext ejbContext;

    private AtomicBoolean busy = new AtomicBoolean(false);

    public void find() throws SystemException {
        if (!busy.compareAndSet(false, true)) {
            return;
        }

        try {
            List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(LabEventType.INFINIUM_XSTAIN,
                    LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
            for (LabVessel labVessel : infiniumChips) {
                UserTransaction utx = ejbContext.getUserTransaction();
                try {
                    if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                        StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                        utx.begin();
                        processChip(staticPlate);
                        utx.commit();
                    }
                } catch (Exception e) {
                    utx.rollback();
                    log.error("Failed to process chip " + labVessel.getLabel(), e);
                    emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(),
                            "[Mercury] Failed to process infinium chip", "For " + labVessel.getLabel() +
                                                                         " with error: " + e.getMessage());
                }
            }
        } finally {
            busy.set(false);
        }
    }

    private void processChip(StaticPlate staticPlate) throws Exception {
        InfiniumRunProcessor.ChipWellResults chipWellResults = infiniumRunProcessor.process(staticPlate);
        if (!chipWellResults.isHasRunStarted() || chipWellResults.getScannerName() == null) {
            return;
        }
        log.debug("Processing chip: " + staticPlate.getLabel());
        LabEvent someStartedEvent = findOrCreateSomeStartedEvent(staticPlate, chipWellResults.getScannerName());
        Set<LabEventMetadata> labEventMetadata = someStartedEvent.getLabEventMetadatas();
        boolean allComplete = true;
        for (VesselPosition vesselPosition : chipWellResults.getPositionWithSampleInstance()) {
            //Check to see if any of the wells in the chip are abandoned.
            if(!staticPlate.isPositionAbandoned(vesselPosition.toString())) {
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
        }

        // Check to see if autocall has now been started on all wells
        boolean starterCalledOnAllWells = true;
        for (VesselPosition vesselPosition: staticPlate.getVesselGeometry().getVesselPositions()) {
            //Check to see if any of the wells in the chip are abandoned.
            if(!staticPlate.isPositionAbandoned(vesselPosition.toString())) {
                Set<SampleInstanceV2> sampleInstancesAtPositionV2 =
                        staticPlate.getContainerRole().getSampleInstancesAtPositionV2(vesselPosition);
                if (sampleInstancesAtPositionV2 != null && !sampleInstancesAtPositionV2.isEmpty()) {
                    boolean autocallStarted = false;
                    for (LabEventMetadata metadata : someStartedEvent.getLabEventMetadatas()) {
                        if (metadata.getLabEventMetadataType() ==
                                LabEventMetadata.LabEventMetadataType.AutocallStarted) {
                            if (metadata.getValue().equals(vesselPosition.name())) {
                                autocallStarted = true;
                                break;
                            }
                        }
                    }
                    if (!autocallStarted) {
                        starterCalledOnAllWells = false;
                        break;
                    }
                }
            }
        }

        if (allComplete && starterCalledOnAllWells) {
            createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_ALL_STARTED, someStartedEvent.getEventLocation());
        }
    }

    private boolean callStarterOnWell(StaticPlate staticPlate, VesselPosition vesselPosition) {
        return infiniumPipelineClient.callStarterOnWell(staticPlate, vesselPosition);
    }

    private LabEvent findOrCreateSomeStartedEvent(StaticPlate staticPlate, String eventLocation) throws Exception {
        LabEvent labEvent = findLabEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_SOME_STARTED);
        if (labEvent == null) {
            labEvent = createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_SOME_STARTED, eventLocation);
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

    private LabEvent createEvent(StaticPlate staticPlate, LabEventType eventType, String eventLocation) throws Exception {
        Date start = new Date();
        BspUser bspUser = getBspUser("seqsystem");
        long operator = bspUser.getUserId();
        LabEvent labEvent =
                new LabEvent(eventType, start, eventLocation, 1L, operator, LabEvent.UI_PROGRAM_NAME);
        staticPlate.addInPlaceEvent(labEvent);
        labEventDao.persist(labEvent);
        labEventDao.flush();
        return labEvent;
    }

    private BspUser getBspUser(String operator) {
        BspUser bspUser = bspUserList.getByUsername(operator);
        if (bspUser == null) {
            throw new RuntimeException("Failed to find operator " + operator);
        }
        return bspUser;
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

package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AppConfig;
import org.broadinstitute.gpinformatics.infrastructure.template.EmailSender;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.annotation.Resource;
import javax.ejb.EJBContext;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;
import java.io.Serializable;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Finds pending infinium chip runs and forwards them on to analysis.
 */
@Stateless
@Dependent
@TransactionManagement(value= TransactionManagementType.BEAN)
public class InfiniumRunFinder implements Serializable {
    private static final Log log = LogFactory.getLog(InfiniumRunFinder.class);

    @Inject
    private InfiniumRunProcessor infiniumRunProcessor;

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

    @Inject
    private UserBean userBean;

    @Resource
    private EJBContext ejbContext;

    private static final AtomicBoolean busy = new AtomicBoolean(false);

    public void find() throws SystemException {
        if (!busy.compareAndSet(false, true)) {
            return;
        }

        try {
            userBean.login("seqsystem");
            List<LabVessel> infiniumChips = labVesselDao.findAllWithEventButMissingAnother(LabEventType.INFINIUM_XSTAIN,
                    LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
            List<String> barcodes = infiniumChips.stream().map(LabVessel::getLabel).collect(Collectors.toList());
            for (String barcode : barcodes) {
                if (labEventDao != null && labEventDao.getEntityManager() != null &&
                        labEventDao.getEntityManager().isOpen()) {
                    UserTransaction utx = ejbContext.getUserTransaction();
                    try {
                        // Clear the session, then fetch each chip individually, otherwise many chips will accumulate.
                        // This accumulation would take progressively longer to dirty check during the flush after
                        // each chip.  For 75 chips, the overall time is reduced from 1m20s to 15s.
                        labVesselDao.clear();
                        LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                        if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                            StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                            utx.begin();
                            log.debug("Processing " + staticPlate.getLabel());
                            processChip(staticPlate);
                            // The commit doesn't cause a flush (not clear why), so we must do it explicitly.
                            labEventDao.flush();
                            utx.commit();
                        }
                    } catch (Exception e) {
                        utx.rollback();
                        log.error("Failed to process chip " + barcode, e);
                        emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(),
                                Collections.emptyList(), "[Mercury] Failed to process infinium chip",
                                "For " + barcode + " with error: " + e.getMessage(),
                                false, true);
                    }
                }
            }
        } finally {
            busy.set(false);
        }
    }

    private void processChip(StaticPlate staticPlate) throws Exception {
        InfiniumRunProcessor.ChipWellResults chipWellResults = infiniumRunProcessor.process(staticPlate);
        if (!chipWellResults.isHasRunStarted()) {
            return;
        }
        boolean failedToFindScannerName = chipWellResults.getScannerName() == null;
        if (failedToFindScannerName) {
            log.warn("Failed to find scanner name from filesystem, setting to Mercury");
            chipWellResults.setScannerName(LabEvent.UI_PROGRAM_NAME);
        }
        if (checkForInvalidPipelineLocation(staticPlate)) {
            log.debug("Won't forward plate where its Pipeline location not set to US Cloud: " + staticPlate.getLabel());
            createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_ALL_STARTED, chipWellResults.getScannerName());
            if (failedToFindScannerName) {
                sendFailedToFindScannerNameEmail(staticPlate);
            }
            return;
        }
        log.debug("Processing chip: " + staticPlate.getLabel());
        LabEvent someStartedEvent = findOrCreateSomeStartedEvent(staticPlate, chipWellResults.getScannerName());
        Set<LabEventMetadata> labEventMetadata = someStartedEvent.getLabEventMetadatas();
        boolean allComplete = true;
        for (VesselPosition vesselPosition : chipWellResults.getPositionWithSampleInstance()) {
            //Check to see if any of the wells in the chip are abandoned.
            if(!isAbandoned(staticPlate,vesselPosition)) {
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
                    boolean started = start(staticPlate, vesselPosition, someStartedEvent);
                    if (!started) {
                        allComplete = false;
                    }
                }
            }
        }

        // Check to see if autocall has now been started on all wells
        boolean starterCalledOnAllWells = true;
        for (VesselPosition vesselPosition: staticPlate.getVesselGeometry().getVesselPositions()) {
            //Check to see if any of the wells in the chip are abandoned.
            if(!isAbandoned(staticPlate,vesselPosition)) {
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
            if (failedToFindScannerName) {
                sendFailedToFindScannerNameEmail(staticPlate);
            }
        }
    }


    /**
     *  Check to see if the any position on the current chip or any ancestor plates or vessels are abandoned.
     */
    private boolean isAbandoned(StaticPlate staticPlate, VesselPosition vesselPosition) {
        TransferTraverserCriteria.AbandonedLabVesselCriteria abandonedLabVesselCriteria =
                new TransferTraverserCriteria.AbandonedLabVesselCriteria();
        staticPlate.getContainerRole().evaluateCriteria(vesselPosition, abandonedLabVesselCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors, 0);
        if(abandonedLabVesselCriteria.isAncestorAbandoned()) {
            return true;
        }
        return false;
    }

    public boolean start(StaticPlate staticPlate, VesselPosition vesselPosition, LabEvent someStartedEvent) {
        boolean started = callStarterOnWell(staticPlate, vesselPosition);
        if (started) {
            LabEventMetadata newMetadata = new LabEventMetadata();
            newMetadata.setLabEventMetadataType(LabEventMetadata.LabEventMetadataType.AutocallStarted);
            newMetadata.setValue(vesselPosition.name());
            someStartedEvent.addMetadata(newMetadata);
        }
        return started;
    }

    private void sendFailedToFindScannerNameEmail(StaticPlate staticPlate) {
        String subject = "[Mercury] Failed to find scanner name for infinium chip " + staticPlate.getLabel();
        String body = "Defaulted scanner name to be " + staticPlate.getLabel() + " for starter events";
        emailSender.sendHtmlEmail(appConfig, appConfig.getWorkflowValidationEmail(),
                Collections.emptyList(),
                subject, body,
                false, true);
    }

    /**
     * Only want to send starter events for chips that are in US Cloud to prevent some samples like Danish Blood Spots
     * to be sent to the cloud
     * @return true if the pipeline location for any sample is not set to US Cloud or null
     */
    public boolean checkForInvalidPipelineLocation(StaticPlate staticPlate) {
        int usCloudCounter = 0;
        for (SampleInstanceV2 sampleInstanceV2: staticPlate.getSampleInstancesV2()) {
            // Ignore the controls assuming that some non-control sample will be present
            if (sampleInstanceV2.getSingleBucketEntry() != null) {
                ProductOrder productOrder = sampleInstanceV2.getSingleBucketEntry().getProductOrder();
                if (productOrder.getPipelineLocation() != ProductOrder.PipelineLocation.US_CLOUD) {
                    return true;
                } else {
                    usCloudCounter++;
                }
            }
        }
        return usCloudCounter == 0;
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

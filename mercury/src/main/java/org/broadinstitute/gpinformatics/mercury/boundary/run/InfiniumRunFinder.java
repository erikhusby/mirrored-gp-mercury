package org.broadinstitute.gpinformatics.mercury.boundary.run;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.ObjectMarshaller;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.BettaLIMSMessage;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.BettaLimsMessageResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventMetadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * User: jowalsh
 * Date: 4/2/16
 */
public class InfiniumRunFinder {
    private static final Log log = LogFactory.getLog(InfiniumRunFinder.class);

    @Inject
    private InfiniumRunProcessor infiniumRunProcessor;

    @Inject
    private LabEventDao labEventDao;

    @Inject
    private BettaLimsMessageResource bettaLimsMessageResource;

    @Inject
    private InfiniumPipelineClient infiniumPipelineClient;

    public void find() {
        for (LabVessel labVessel: listPendingXStainChips()) {
            if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                try {
                    StaticPlate staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                    InfiniumRunProcessor.ChipWellResults chipWellResults = infiniumRunProcessor.process(staticPlate);
                    LabEvent someStartedEvent = findOrCreateSomeStartedEvent(staticPlate);
                    Set<LabEventMetadata> labEventMetadata = someStartedEvent.getLabEventMetadatas();
                    boolean autocallStartedOnAllWells = true;
                    for (VesselPosition vesselPosition: chipWellResults.getCompletedWells()) {
                        boolean autocallStarted = false;
                        for (LabEventMetadata metadata: labEventMetadata) {
                            if (metadata.getLabEventMetadataType() ==
                                LabEventMetadata.LabEventMetadataType.AutocallStarted) {
                                if (metadata.getValue().equals(vesselPosition.name())) {
                                    autocallStarted = true;
                                    break;
                                }
                            }
                        }
                        if (!autocallStarted) {
                            if (callStarterOnWell(staticPlate, vesselPosition)) {
                                LabEventMetadata newMetadata = new LabEventMetadata();
                                newMetadata
                                        .setLabEventMetadataType(LabEventMetadata.LabEventMetadataType.AutocallStarted);
                                newMetadata.setValue(vesselPosition.name());
                                someStartedEvent.addMetadata(newMetadata);
                            } else {
                                autocallStartedOnAllWells = false;
                            }
                        }
                    }

                    if (autocallStartedOnAllWells) {
                        createEvent(staticPlate, LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
                    }
                } catch (Exception e) {
                    log.error("Failed to process chip " + labVessel.getLabel(), e);
                }
            }
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
        plateType.setSection("ALL96"); //TODO
        plateEventType.setPlate(plateType);
        bettaLIMSMessage.getPlateEvent().add(plateEventType);
        ObjectMarshaller<BettaLIMSMessage> bettaLIMSMessageObjectMarshaller =
                new ObjectMarshaller<>(BettaLIMSMessage.class);
        bettaLimsMessageResource.storeAndProcess(bettaLIMSMessageObjectMarshaller.marshal(bettaLIMSMessage));
        return findLabEvent(staticPlate, eventType);
    }

    public List<LabVessel> listPendingXStainChips() {
        List<LabVessel> list = new ArrayList<>();
        List<LabEvent> labEvents = labEventDao.findByEventType(LabEventType.INFINIUM_XSTAIN);
        for (LabEvent labEvent: labEvents) {
            boolean foundAllStartedEvent = false;
            LabVessel labVessel = labEvent.getInPlaceLabVessel();
            Set<LabEvent> inPlaceLabEvents = labVessel.getInPlaceLabEvents();
            for (LabEvent inPlaceLabEvent: inPlaceLabEvents) {
                if (inPlaceLabEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_ALL_STARTED) {
                    foundAllStartedEvent = true;
                    break;
                }
            }
            if (!foundAllStartedEvent) {
                list.add(labVessel);
            }
        }
        return list;
    }

    public void setLabEventDao(LabEventDao labEventDao) {
        this.labEventDao = labEventDao;
    }

    public void setInfiniumRunProcessor(
            InfiniumRunProcessor infiniumRunProcessor) {
        this.infiniumRunProcessor = infiniumRunProcessor;
    }

    public void setInfiniumPipelineClient(
            InfiniumPipelineClient infiniumPipelineClient) {
        this.infiniumPipelineClient = infiniumPipelineClient;
    }
}

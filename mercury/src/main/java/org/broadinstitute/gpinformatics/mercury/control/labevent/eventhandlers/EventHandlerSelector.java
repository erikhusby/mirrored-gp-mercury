package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

/**
 * BettaLims messages that are processed in mercury are, for the most part, generic.  There is no specific validation
 * or processing per message that needs to happen.  The purpose of this method is to provide a structure for those
 * special cases where specific validation or handling needs to happen.
 *
 * By triggering off of the {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType}, we can
 * target each message at its own message validator.
 */
@Dependent
public class EventHandlerSelector {

    private DenatureToDilutionTubeHandler denatureToDilutionTubeHandler;
    private FlowcellMessageHandler flowcellMessageHandler;
    private FlowcellLoadedHandler flowcellLoadedHandler;
    private BspNewRootHandler bspNewRootHandler;
    private CreateLabBatchHandler createLabBatchHandler;
    private QueueEjb queueEjb;
    private DnaQuantEnqueueOverride dnaQuantEnqueueOverride;

    @Inject
    public EventHandlerSelector(DenatureToDilutionTubeHandler denatureToDilutionTubeHandler,
                                FlowcellMessageHandler flowcellMessageHandler,
                                FlowcellLoadedHandler flowcellLoadedHandler,
                                BspNewRootHandler bspNewRootHandler,
                                CreateLabBatchHandler createLabBatchHandler,
                                QueueEjb queueEjb,
                                DnaQuantEnqueueOverride dnaQuantEnqueueOverride) {
        this.denatureToDilutionTubeHandler = denatureToDilutionTubeHandler;
        this.flowcellMessageHandler = flowcellMessageHandler;
        this.flowcellLoadedHandler = flowcellLoadedHandler;
        this.bspNewRootHandler = bspNewRootHandler;
        this.createLabBatchHandler = createLabBatchHandler;
        this.queueEjb = queueEjb;
    }

    /**
     * Primarily called in
     * {@link org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory#buildFromBettaLims}, this
     * method routes message specific handling of lab events to their appropriate handler based on the
     * {@link org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType} associated with the given lab
     * event
     *
     * @param targetEvent Event that is to be processed.  This will have access to the existing/created entities that
     *                    need some action performed based on the message they were processed with
     * @param stationEvent This is the original JAXB message that was sent for processing.  It will typically contain
     *                     some extra information that may not make sense to store with the {@link LabEvent}.
     */
    public void applyEventSpecificHandling(LabEvent targetEvent, StationEventType stationEvent) {

        switch (targetEvent.getLabEventType()) {
        case DENATURE_TO_DILUTION_TRANSFER:
        case STRIP_TUBE_B_TRANSFER:
            denatureToDilutionTubeHandler.handleEvent(targetEvent, stationEvent);
            break;
        case DILUTION_TO_FLOWCELL_TRANSFER:
        case FLOWCELL_TRANSFER:
        case DENATURE_TO_FLOWCELL_TRANSFER:
        case REAGENT_KIT_TO_FLOWCELL_TRANSFER:
            flowcellMessageHandler.handleEvent(targetEvent, stationEvent);
            break;
        case FLOWCELL_LOADED:
            flowcellLoadedHandler.handleEvent(targetEvent, stationEvent);
            break;
        case BLOOD_PLASMA_SECOND_TRANSFER:
        case BLOOD_BUFFY_COAT_TRANSFER:
            bspNewRootHandler.handleEvent(targetEvent, stationEvent);
            break;
        case ARRAY_PLATING_DILUTION:
            createLabBatchHandler.handleEvent(targetEvent, stationEvent);
            break;
        }

        if (targetEvent.getLabEventType() != null && targetEvent.getLabEventType().getResultingMaterialType() != null
                && targetEvent.getLabEventType().getResultingMaterialType().containsIgnoringCase("dna")) {

            MessageCollection messageCollection = new MessageCollection();
            Set<LabVessel> targetLabVessels = targetEvent.getTargetVesselTubes();

            QueueSpecialization queueSpecialization = dnaQuantEnqueueOverride.determineDnaQuantQueueSpecialization(targetLabVessels);

            Set<String> xtrBatches = new HashSet<>();
            for (LabVessel targetLabVessel : targetLabVessels) {
                for (SampleInstanceV2 sampleInstanceV2 : targetLabVessel.getSampleInstancesV2()) {
                    LabBatch singleBatch = sampleInstanceV2.getSingleBatch();
                    if (singleBatch != null && singleBatch.getBatchName().startsWith("XTR")) {
                        xtrBatches.add(singleBatch.getBatchName());
                    }
                }
            }
            String readableText = xtrBatches.isEmpty() ?
                    "Extracted on " + DateUtils.convertDateTimeToString(targetEvent.getEventDate()) :
                    StringUtils.join(xtrBatches, ",");

            queueEjb.enqueueLabVessels(targetLabVessels, QueueType.DNA_QUANT, readableText, messageCollection,
                    QueueOrigin.EXTRACTION, queueSpecialization);
        }
    }

    public FlowcellMessageHandler getFlowcellMessageHandler() {
        return flowcellMessageHandler;
    }

    public FlowcellLoadedHandler getFlowcellLoadedHandler() {
        return flowcellLoadedHandler;
    }

}

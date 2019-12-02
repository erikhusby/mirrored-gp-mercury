package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.entity.sample.ContractClient.MAYO;

@Dependent
public class QueueEventHandler extends AbstractEventHandler {

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private DnaQuantEnqueueOverride dnaQuantEnqueueOverride;

    @Override
    public void handleEvent(LabEvent targetEvent, StationEventType stationEvent) {
        switch (targetEvent.getLabEventType()) {
            case VOLUME_MEASUREMENT: {
                Set<LabVessel> allOfUsVessels = allOfUsVessels(targetEvent);
                if (!allOfUsVessels.isEmpty()) {
                    queueEjb.dequeueLabVessels(allOfUsVessels, QueueType.VOLUME_CHECK,
                            new MessageCollection(), DequeueingOptions.DEFAULT_DEQUEUE_RULES);
                }
                break;
            }
            case PICO_DILUTION_TRANSFER_FORWARD_BSP: {
                Set<LabVessel> allOfUsVessels = allOfUsVessels(targetEvent);
                if (!allOfUsVessels.isEmpty()) {
                    QueueSpecialization queueSpecialization =
                            dnaQuantEnqueueOverride.determineDnaQuantQueueSpecialization(allOfUsVessels);
                    // todo jmt read toggle from preference
                    queueEjb.enqueueLabVessels(allOfUsVessels, QueueType.DNA_QUANT,
                            "Volume checked on " + DateUtils.convertDateTimeToString(targetEvent.getEventDate()),
                            new MessageCollection(), QueueOrigin.RECEIVING, queueSpecialization);
                }
                break;
            }
            case FINGERPRINTING_ALIQUOT:
            case FINGERPRINTING_ALIQUOT_FORWARD_BSP: {
                Set<LabVessel> allOfUsVessels = allOfUsVessels(targetEvent);
                if (!allOfUsVessels.isEmpty()) {
                    queueEjb.enqueueLabVessels(allOfUsVessels, QueueType.FINGERPRINTING,
                            "Finger print aliquoted on " + DateUtils.convertDateTimeToString(targetEvent.getEventDate()),
                            new MessageCollection(), QueueOrigin.RECEIVING, null);
                }
                // todo jmt add queue removal to fingerprint upload
                break;
            }
        }
    }

    @NotNull
    private Set<LabVessel> allOfUsVessels(LabEvent targetEvent) {
        return targetEvent.getSourceVesselTubes().stream().filter(
                labVessel -> labVessel.getSampleInstancesV2().iterator().next().getRootOrEarliestMercurySample().
                        getMetadata().stream().anyMatch(metadata -> metadata.getKey() == Metadata.Key.CLIENT &&
                                    metadata.getStringValue().equals(MAYO.name()))).
                collect(Collectors.toSet());
    }
}

package org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.control.dao.preference.PreferenceDao;
import org.broadinstitute.gpinformatics.athena.entity.preference.NameValueDefinitionValue;
import org.broadinstitute.gpinformatics.athena.entity.preference.Preference;
import org.broadinstitute.gpinformatics.athena.entity.preference.PreferenceType;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.StationEventType;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.MayoManifestEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.jetbrains.annotations.NotNull;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.broadinstitute.gpinformatics.mercury.entity.sample.ContractClient.MAYO;

@Dependent
public class QueueEventHandler extends AbstractEventHandler {

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private DnaQuantEnqueueOverride dnaQuantEnqueueOverride;

    @Inject
    private PreferenceDao preferenceDao;

    @Override
    public void handleEvent(LabEvent labEvent, StationEventType stationEvent) {
        MessageCollection messageCollection = new MessageCollection();
        switch (labEvent.getLabEventType()) {
            case VOLUME_MEASUREMENT: {
                dequeue(labEvent, Direction.SOURCE, QueueType.VOLUME_CHECK, messageCollection);

                // Add to plating queue
                Set<LabVessel> labVessels = getVesselsPreferTubes(labEvent, Direction.SOURCE);
                Set<String> productTypes = labVessels.stream().flatMap(
                        lv -> Arrays.stream(lv.getMetadataValues(Metadata.Key.PRODUCT_TYPE))).collect(Collectors.toSet());
                if (!productTypes.isEmpty()) {
                    if (productTypes.size() == 1) {
                        String productType = productTypes.iterator().next();
                        QueueType queueType;
                        if (productType.equals(MayoManifestEjb.AOU_ARRAY)) {
                            queueType = QueueType.ARRAY_PLATING;
                        } else if (productType.equals(MayoManifestEjb.AOU_GENOME)) {
                            queueType = QueueType.SEQ_PLATING;
                        } else {
                            throw new RuntimeException("Unexpected product type " + productType);
                        }
                        String rack = ((TubeFormation) labEvent.getInPlaceLabVessel()).getRacksOfTubes().iterator().next().getLabel();
                        queueEjb.enqueueLabVessels(labVessels, queueType,
                                rack + " Volume Checked on " + DateUtils.convertDateTimeToString(new Date()),
                                messageCollection, QueueOrigin.OTHER, null);
                    } else {
                        throw new RuntimeException("Multiple product types " + productTypes);
                    }
                }
                break;
            }
            // dilution plate is input to pico, then input to fingerprinting
            // Determine: whether All of Us; source or dest; queue or dequeue; queue type
            case PICO_DILUTION_TRANSFER:
            case PICO_DILUTION_TRANSFER_FORWARD_BSP: {
                if (isAllOfUs(labEvent)) {
                    try {
                        // Check the contract client preference
                        boolean addToQueue = true;
                        Set<LabVessel> vesselsPreferTubes = getVesselsPreferTubes(labEvent, Direction.DEST);
                        LabVessel labVessel = vesselsPreferTubes.iterator().next();
                        Optional<Metadata> optionalMetadata = labVessel.getSampleInstancesV2().stream().
                                findFirst().orElseThrow(() -> new RuntimeException("No samples for " + labVessel.getLabel())).
                                getRootOrEarliestMercurySample().getMetadata().stream().
                                filter(metadata -> metadata.getKey() == Metadata.Key.CLIENT).findFirst();
                        if (optionalMetadata.isPresent()) {
                            Preference preference = preferenceDao.getGlobalPreference(PreferenceType.CONTRACT_CLIENT_QUEUES);
                            if (preference != null) {
                                NameValueDefinitionValue nameValueDefinitionValue =
                                        (NameValueDefinitionValue) preference.getPreferenceDefinition().getDefinitionValue();
                                List<String> queues = nameValueDefinitionValue.getDataMap().get(optionalMetadata.get().getStringValue());
                                if (queues != null && !queues.contains(QueueType.DNA_QUANT.name())) {
                                    addToQueue = false;
                                }
                            }
                        }
                        if (addToQueue) {
                            QueueSpecialization queueSpecialization =
                                    dnaQuantEnqueueOverride.determineDnaQuantQueueSpecialization(vesselsPreferTubes);
                            queueEjb.enqueueLabVessels(vesselsPreferTubes, QueueType.DNA_QUANT,
                                    vesselsPreferTubes.iterator().next().getLabel() + " dilution on " + DateUtils.convertDateTimeToString(labEvent.getEventDate()),
                                    messageCollection, QueueOrigin.RECEIVING, queueSpecialization);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to get client preference", e);
                    }
                }
                break;
            }
            case PICO_TRANSFER:
            case PICO_MICROFLUOR_TRANSFER: {
                if (isAllOfUs(labEvent)) {
                    Set<LabVessel> vesselsPreferTubes = getVesselsPreferTubes(labEvent, Direction.SOURCE);
                    String[] productTypes = vesselsPreferTubes.iterator().next().getMetadataValues(Metadata.Key.PRODUCT_TYPE);
                    if (productTypes.length > 0 && productTypes[0].equals(MayoManifestEjb.AOU_GENOME)) {
                        String sourcePlate = labEvent.getSectionTransfers().iterator().next().getSourceVessel().getLabel();
                        queueEjb.enqueueLabVessels(vesselsPreferTubes, QueueType.FINGERPRINTING,
                                sourcePlate + " DNA quant on " + DateUtils.convertDateTimeToString(labEvent.getEventDate()),
                                messageCollection, QueueOrigin.RECEIVING, null);
                    }
                }
                break;
            }
            case ARRAY_PLATING_DILUTION:
                dequeue(labEvent, Direction.SOURCE, QueueType.ARRAY_PLATING, messageCollection);
                break;
            case AUTO_DAUGHTER_PLATE_CREATION:
                dequeue(labEvent, Direction.SOURCE, QueueType.SEQ_PLATING, messageCollection);
                break;
        }
        if (messageCollection.hasErrors()) {
            throw new RuntimeException(StringUtils.join(messageCollection.getErrors(), ","));
        }
    }

    enum Direction {
        SOURCE,
        DEST
    }

    private void dequeue(LabEvent labEvent, Direction direction, QueueType arrayPlating,
            MessageCollection messageCollection) {
        if (isAllOfUs(labEvent)) {
            queueEjb.dequeueLabVessels(getVesselsPreferTubes(labEvent, direction), arrayPlating, messageCollection,
                    DequeueingOptions.DEFAULT_DEQUEUE_RULES);
        }
    }

    private boolean isAllOfUs(LabEvent labEvent) {
        Set<LabVessel> labVessels = getVesselsPreferTubes(labEvent, Direction.SOURCE);
        return labVessels.stream().anyMatch(lv -> lv.getSampleInstancesV2().stream().anyMatch(
                si -> {
                    MercurySample rootSample = si.getRootOrEarliestMercurySample();
                    return rootSample != null && rootSample.getMetadata().stream().anyMatch(
                            md -> md.getKey() == Metadata.Key.CLIENT && md.getStringValue().equals(MAYO.name()));
                }));
    }

    /*
    get
     */
    @NotNull
    public static Set<LabVessel> getVesselsPreferTubes(LabEvent labEvent, Direction direction) {
        Set<LabVessel> labVessels = direction == Direction.SOURCE ? labEvent.getSourceVesselTubes() :
                labEvent.getTargetVesselTubes();
        if (labVessels.isEmpty()) {
            LabVessel labVessel = labEvent.getInPlaceLabVessel();
            if (labVessel != null) {
                if (labVessel.getContainerRole() != null &&
                        OrmUtil.proxySafeIsInstance(labVessel, TubeFormation.class)) {
                    labVessels = (Set<LabVessel>) labVessel.getContainerRole().getContainedVessels();
                } else {
                    labVessels = Collections.singleton(labVessel);
                }
            }
        }
        return labVessels;
    }
}

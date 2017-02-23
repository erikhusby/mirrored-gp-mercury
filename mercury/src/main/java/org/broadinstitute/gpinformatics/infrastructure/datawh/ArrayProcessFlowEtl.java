package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * ETL all events of interest in Infinium array process flow <br/>
 * The logic is rigidly tied to the vessel transfers in the Infinium array process workflow
 *   by PDO, LCSET, and the Mercury aliquot sample name: <br/>
 *   SectionTransfer to DNA plate, SectionTransfer from DNA plate to amp plate, and CherryPickTransfer from amp plate to infinium chip. <br/>
 *   If no Infinium buckets have been created on the DNSA plate chip well, all events are ignored because we're unable to deterministically track by PDO, LCSET, and sample
 */
@Stateful
public class ArrayProcessFlowEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private final List<String> logErrors = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();

    // This group captures events which take place on the amplification plate
    private static final Set<LabEventType> AMP_EVENT_TYPES = new HashSet<>();
    static {
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_AMPLIFICATION );
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_FRAGMENTATION);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_POST_FRAGMENTATION_HYB_OVEN_LOADED);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_PRECIPITATION);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_POST_PRECIPITATION_HEAT_BLOCK_LOADED);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_PRECIPITATION_ISOPROPANOL_ADDITION);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_RESUSPENSION);
        AMP_EVENT_TYPES.add( LabEventType.INFINIUM_POST_RESUSPENSION_HYB_OVEN);
    }

    // This group captures events which take place on the Infinium chip
    private static final Set<LabEventType> CHIP_EVENT_TYPES = new HashSet<>();
    static {
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_HYBRIDIZATION);
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_POST_HYBRIDIZATION_HYB_OVEN_LOADED);
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_HYB_CHAMBER_LOADED);
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_XSTAIN);
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_AUTOCALL_SOME_STARTED);
        CHIP_EVENT_TYPES.add( LabEventType.INFINIUM_AUTOCALL_ALL_STARTED);
    }
    
    public ArrayProcessFlowEtl() {
    }

    @Inject
    public ArrayProcessFlowEtl(LabEventDao dao) {
        super(LabEvent.class, "array_process", "lab_event_aud", "lab_event_id", dao);
    }

    @Override
    Long entityId(LabEvent entity) {
        return entity.getLabEventId();
    }

    @Override
    Path rootId(Root<LabEvent> root) {
        return root.get(LabEvent_.labEventId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabEvent.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEvent entity) {

        List<ArrayDto> arrayFlowDtos = null;

        if( entity == null ) {
            return (List<String>)Collections.EMPTY_LIST;
        }

        // Supports overrides but not deletes
        if( isDelete ) {
            loggingDeletedEventIds.add(entity.getLabEventId());
            return (List<String>)Collections.EMPTY_LIST;
        }

        LabEventType eventType = entity.getLabEventType();

        try {
            if (eventType == LabEventType.INFINIUM_BUCKET ) {
                // An ARRAY LCSET has been created: LCSET and PDO values are available so get all associated events
                arrayFlowDtos = makeDtosFromBucketEvent(entity);
            } else if( AMP_EVENT_TYPES.contains(eventType)) {
                arrayFlowDtos = makeDtosFromAmpPlate(entity);
            } else if( CHIP_EVENT_TYPES.contains(eventType)) {
                arrayFlowDtos = makeDtosFromChip(entity);
            } else {
                return (List<String>)Collections.EMPTY_LIST;
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
            logger.error("Error in array flow etl", e);
            logErrors.add("Error in array flow etl, EventID: " + entity.getLabEventId() + ", Type: "
                    + entity.getLabEventType().getName() + ", Error: " + e.getMessage() );
        }

        if( arrayFlowDtos == null || arrayFlowDtos.size() == 0 ) {
            return (List<String>)Collections.EMPTY_LIST;
        }

        List<String> records = new ArrayList<>();
        for( ArrayDto dto : arrayFlowDtos ) {
            records.add(dto.toEtlString(etlDateStr, isDelete));
        }
        arrayFlowDtos.clear();
        return records;
    }

    /**
     * From an Infinium bucket event on a DNA plate chip well, look backwards to obtain DNA plate well details and
     * map to PDO, LCSET, and sample data of bucketed plate well<br />
     * Also, look forward in case an Infinium bucket event was performed after other events
     * (many cases in initial array process shakeout mid 2016 to early 2017)
     * @return  A row for all event types of interest in an array process flow starting at DNA plate well
     * to be mapped in ETL to horizontal columns in a single row keyed by PDO, LCSET, and aliquot sample ID
     */
    private List<ArrayDto> makeDtosFromBucketEvent(LabEvent bucketEvent) {
        List<ArrayDto> arrayFlowDtos = new ArrayList<>();

        Set<SampleInstanceV2> dnaWellSampleInstances;

        // Get the ArrayPlatingDilution transfer to this vessel (plate well)
        PlateWell dnaPlateWell = null;
        if( OrmUtil.proxySafeIsInstance(bucketEvent.getInPlaceLabVessel(), PlateWell.class)) {
            dnaPlateWell = OrmUtil.proxySafeCast(bucketEvent.getInPlaceLabVessel(), PlateWell.class);
        } else {
            // Die if not a DNA plate well (very unlikely)
            logErrors.add("InfiniumBucket event does not contain a plate well, ID: " + bucketEvent.getLabEventId());
            return arrayFlowDtos;
        }

        Set<LabEvent> dnaPlateXfers = dnaPlateWell.getTransfersTo();
        LabEvent platingEvent = null;
        // There should never be more than 1 section transfer event, but make sure it's ArrayPlatingDilution
        for( LabEvent event : dnaPlateXfers ) {
            if( event.getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION ) {
                platingEvent = event;
                break;
            }
        }

        if (platingEvent == null) {
            logErrors.add("No plating event prior to InfiniumBucket event, ID: " + bucketEvent.getLabEventId());
            // Don't die here, continue looking for downstream events
        }

        LabVessel dnaPlate = dnaPlateWell.getPlate();
        VesselPosition dnaPlatePosition = dnaPlateWell.getVesselPosition();
        // All downstream Infinium events share samples of DNA plate well (should only ever be 1)
        dnaWellSampleInstances = dnaPlateWell.getSampleInstancesV2();

        for( SampleInstanceV2 si : dnaWellSampleInstances) {
            if( si.getSingleBucketEntry() != null ) {
                arrayFlowDtos.add(new ArrayDto(platingEvent, dnaPlateWell, dnaPlatePosition, dnaPlate,
                        si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                        si.getSingleBatch().getBatchName(),
                        si.getNearestMercurySampleName(),
                        si.getEarliestMercurySampleName())
                );
            } else {
                logErrors.add("No single bucket entry for DNA plate well " + dnaPlateWell.getLabel());
            }
        }

        // Is there some sort of race condition between DNA plate, bucket, and amp plate?
        // Find any downstream events and add them to process ETL
        // An amp plate is 1:1 section transfer from DNA plate
        LabEvent ampEvent = null;
        for( LabEvent event : dnaPlateWell.getTransfersFrom() ) {
            // Should pick up InfiniumAmplification
            if( AMP_EVENT_TYPES.contains( event.getLabEventType() ) ) {
                ampEvent = event;
                break;
            }
        }

        LabVessel ampPlate = null;
        if( ampEvent != null ) {
            // Amp plate is always a single target vessel
            ampPlate = ampEvent.getTargetLabVessels().iterator().next();
            // Amp plate is a 1:1 section transfer from DNA plate, use DNA plate well sample instance
            for( SampleInstanceV2 si : dnaWellSampleInstances ) {
                if( si.getSingleBucketEntry() != null ) {
                    arrayFlowDtos.add(new ArrayDto(ampEvent, ampPlate, dnaPlatePosition, ampPlate,
                            si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                            si.getSingleBatch().getBatchName(),
                            si.getNearestMercurySampleName(),
                            si.getEarliestMercurySampleName())
                    );
                    // Capture in place events of interest on amp plate and use same DNA plate well sample instance
                    for( LabEvent inPlaceEvent : ampPlate.getInPlaceLabEvents() ) {
                        if( AMP_EVENT_TYPES.contains(inPlaceEvent.getLabEventType() )) {
                            arrayFlowDtos.add(new ArrayDto(inPlaceEvent, ampPlate, dnaPlatePosition, ampPlate,
                                    si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                                    si.getSingleBatch().getBatchName(),
                                    si.getNearestMercurySampleName(),
                                    si.getEarliestMercurySampleName())
                            );
                        }
                    }
                }
            }
        } else {
            // Nothing downstream, quit
            return arrayFlowDtos;
        }

        // Chip plates are all cherry pick transfers from 96 amp plate positions
        LabEvent chipEvent = null;
        LabVessel chip = null;
        VesselPosition chipPosition = null;
        for( LabEvent event : ampPlate.getTransfersFrom() ) {
            if( CHIP_EVENT_TYPES.contains(event.getLabEventType() )) {
                for (CherryPickTransfer chipXfer : event.getCherryPickTransfers()) {
                    // Use DNA plate -> amp plate position
                    if (chipXfer.getSourcePosition() == dnaPlatePosition) {
                        chipEvent = event;
                        chip = event.getTargetLabVessels().iterator().next();
                        chipPosition = chipXfer.getTargetPosition();
                        break;
                    }
                }
            }
        }

        if( chipEvent != null ) {
            for ( SampleInstanceV2 si : dnaWellSampleInstances ) {
                if (si.getSingleBucketEntry() != null) {
                    // Use DNA plate well sample instance and chip vesssel data
                    arrayFlowDtos.add(new ArrayDto(chipEvent, chip, chipPosition, chip,
                            si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                            si.getSingleBatch().getBatchName(),
                            si.getNearestMercurySampleName(),
                            si.getEarliestMercurySampleName())
                    );
                    // Capture in place events of interest on chip
                    for (LabEvent inPlaceEvent : chip.getInPlaceLabEvents()) {
                        if (CHIP_EVENT_TYPES.contains(inPlaceEvent.getLabEventType())) {
                            arrayFlowDtos.add(new ArrayDto(inPlaceEvent, chip, chipPosition, chip,
                                    si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                                    si.getSingleBatch().getBatchName(),
                                    si.getNearestMercurySampleName(),
                                    si.getEarliestMercurySampleName())
                            );
                        }
                    }
                }
            }
        }

        return arrayFlowDtos;

    }

    private List<ArrayDto> makeDtosFromInPlaceVessel(LabEvent plateEvent, LabVessel eventVessel) {
        List<ArrayDto> arrayFlowDtos = new ArrayList<>();

        for (SampleInstanceV2 si : eventVessel.getSampleInstancesV2()) {
            if (si.getSingleBucketEntry() != null) {
                // In-place events don't use positions or containers in array process ETL
                // Make rows for each PDO-LCSET-Sample combination on the plate
                arrayFlowDtos.add(new ArrayDto(plateEvent, eventVessel, null, eventVessel,
                        si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                        si.getSingleBatch().getBatchName(),
                        si.getNearestMercurySampleName(),
                        si.getEarliestMercurySampleName())
                );
            }
        }

        return arrayFlowDtos;
    }

    private List<ArrayDto> makeDtosFromAmpPlate(LabEvent plateEvent) {
        List<ArrayDto> arrayFlowDtos = new ArrayList<>();

        LabVessel eventVessel = plateEvent.getInPlaceLabVessel();
        if (eventVessel != null) {
            return makeDtosFromInPlaceVessel(plateEvent, eventVessel);
        } else {
            // Positions required, amp plate is a section transfer
            for (SectionTransfer sectionXfer : plateEvent.getSectionTransfers()) {
                LabVessel plate = sectionXfer.getTargetVesselContainer().getEmbedder();
                for (VesselPosition position : sectionXfer.getTargetSection().getWells()) {
                    for (SampleInstanceV2 si : sectionXfer.getTargetVesselContainer().getSampleInstancesAtPositionV2(position)) {
                        if (si.getSingleBucketEntry() != null) {
                            // Make rows for each PDO-LCSET-Sample combination on the plate
                            arrayFlowDtos.add(new ArrayDto(plateEvent, plate, position, plate,
                                    si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                                    si.getSingleBatch().getBatchName(),
                                    si.getNearestMercurySampleName(),
                                    si.getEarliestMercurySampleName())
                            );
                        }
                    }
                }
            }
        }

        return arrayFlowDtos;
    }

    private List<ArrayDto> makeDtosFromChip(LabEvent plateEvent) {
        List<ArrayDto> arrayFlowDtos = new ArrayList<>();

        LabVessel eventVessel = plateEvent.getInPlaceLabVessel();
        if (eventVessel != null) {
            return makeDtosFromInPlaceVessel(plateEvent, eventVessel);
        } else {
            // Positions required, chip plate are cherry pick transfers
            for (CherryPickTransfer xfer : plateEvent.getCherryPickTransfers()) {
                LabVessel chip = xfer.getTargetVesselContainer().getEmbedder();
                VesselPosition position = xfer.getTargetPosition();
                for (SampleInstanceV2 si : xfer.getTargetVesselContainer().getSampleInstancesAtPositionV2(position)) {
                    if (si.getSingleBucketEntry() != null) {
                        // Make rows for each PDO-LCSET-Sample combination on the chip
                        arrayFlowDtos.add(new ArrayDto(plateEvent, chip, position, chip,
                                si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                                si.getSingleBatch().getBatchName(),
                                si.getNearestMercurySampleName(),
                                si.getEarliestMercurySampleName())
                        );
                    }
                }
            }
        }
        return arrayFlowDtos;
    }

    @Override
    public void postEtlLogging() {

        for( String msg : logErrors ) {
            logger.debug(msg);
        }
        logErrors.clear();

        // Logs any deleted events that currently require delete and re-etl of all later events.
        SortedSet<Long> deletedIds = new TreeSet<>();
        synchronized (loggingDeletedEventIds) {
            deletedIds.addAll(loggingDeletedEventIds);
            loggingDeletedEventIds.clear();
        }
        if (deletedIds.size() > 0) {
            logger.error("Manual etl required to fixup lab events downstream of deleted lab events " +
                    StringUtils.join(deletedIds, ", "));
        }
    }

    /**
     * Holds extract data for array process flow ETL. <br/>
     * The array process flow is tracked by PDO, LCSET, and LCSET sample name
     */
    public static class ArrayDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private LabVessel container;
        private VesselPosition vesselPosition;
        private Long productOrderId;
        private String batchName;
        private String lcsetSampleName;
        private String sampleName;

        /**
         * Encapsulates a single event in the array process flow <br/>
         * The flow is tracked by mandatory fields: PDO, LCSET, and LCSET sample name
         **/
        ArrayDto( LabEvent labEvent, LabVessel labVessel, VesselPosition vesselPosition, LabVessel container,
                  Long productOrderId, String batchName, String lcsetSampleName, String sampleName) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.container = container;
            this.vesselPosition = vesselPosition;
            this.productOrderId = productOrderId;
            this.batchName = batchName;
            this.lcsetSampleName = lcsetSampleName;
            this.sampleName = sampleName;
        }

        public String toEtlString(String etlDateStr, boolean isDelete ) {
            return genericRecord(etlDateStr, isDelete,
                    format(productOrderId),
                    format(batchName),
                    format(lcsetSampleName),
                    format(sampleName),
                    format(labEvent.getLabEventId()),
                    format(labEvent.getLabEventType().getName()),
                    format(labEvent.getEventLocation()),
                    format(labEvent.getEventDate()),
                    format(labVessel.getLabel()),
                    format(container == null ? "" : container.getName()),
                    format(vesselPosition==null?"":vesselPosition.toString())
            );
        }
    }
}

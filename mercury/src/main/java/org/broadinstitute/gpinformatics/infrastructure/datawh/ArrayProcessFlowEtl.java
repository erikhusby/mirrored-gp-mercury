package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.*;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.*;

/**
 * ETL all events of interest in Infinium array process flow <br/>
 * The logic is rigidly tied to the vessel transfers in the Infinium array process workflow
 *   by LCSET and the Mercury aliquot sample name: <br/>
 *   SectionTransfer to DNA plate, SectionTransfer from DNA plate to amp plate, and CherryPickTransfer from amp plate to infinium chip. <br/>
 *   If no Infinium buckets have been created on the DNA plate chip well, all events are ignored because we're unable to deterministically track by LCSET and sample
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class ArrayProcessFlowEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private final List<String> logErrors = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();

    // Events of interest
    private final Set<LabEventType> ampEventTypes = new HashSet<>(Arrays.asList(
            LabEventType.INFINIUM_AMPLIFICATION,
            LabEventType.INFINIUM_POST_FRAGMENTATION_HYB_OVEN_LOADED,
            LabEventType.INFINIUM_FRAGMENTATION,
            LabEventType.INFINIUM_PRECIPITATION,
            LabEventType.INFINIUM_POST_PRECIPITATION_HEAT_BLOCK_LOADED,
            LabEventType.INFINIUM_PRECIPITATION_ISOPROPANOL_ADDITION,
            LabEventType.INFINIUM_RESUSPENSION,
            LabEventType.INFINIUM_POST_RESUSPENSION_HYB_OVEN));
    private final Set<LabEventType> hybEventTypes = new HashSet<>(Arrays.asList(
            LabEventType.INFINIUM_HYBRIDIZATION,
            LabEventType.INFINIUM_POST_HYBRIDIZATION_HYB_OVEN_LOADED,
            LabEventType.INFINIUM_HYB_CHAMBER_LOADED,
            LabEventType.INFINIUM_XSTAIN,
            LabEventType.INFINIUM_AUTOCALL_SOME_STARTED,
            LabEventType.INFINIUM_AUTOCALL_ALL_STARTED));

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
    public Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEvent entity) {

        // Supports inserts and updates but not deletes, safe to assume a delete would be followed by a re-insert
        if (entity == null || isDelete ) {
            return (List<String>) Collections.EMPTY_LIST;
        }

        LabEventType eventType = entity.getLabEventType();
        if ( !ampEventTypes.contains(eventType) && !hybEventTypes.contains(eventType) ) {
            // Not an Infinium event type or not interested in it
            return (List<String>) Collections.EMPTY_LIST;
        }

        // Based upon availability of infinium bucket entries
        //  , otherwise chain of events correlation attempt by lab batch and aliquot sample ID is useless
        Set<LabEvent> bucketEvents = getRelatedBucketEvents(entity);
        List<ArrayDto> arrayFlowDtos = new ArrayList<>();

        for (LabEvent labEvent : bucketEvents) {
            try {
                // An ARRAY LCSET has been created: LCSET and PDO values are available so get all associated events
                arrayFlowDtos.addAll(makeDtosFromBucketEvent(labEvent));
            } catch (Exception e) {
                // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
                logger.error("Error in array flow etl", e);
                logErrors.add("Error in array flow etl, EventID: " + entity.getLabEventId() + ", Type: "
                        + entity.getLabEventType().getName() + ", Error: " + e.getMessage());
            }
        }

        List<String> records = new ArrayList<>();
        for (ArrayDto arrayFlowDto : arrayFlowDtos) {
            records.add(arrayFlowDto.toEtlString(etlDateStr, isDelete));
        }
        return records;
    }

    /**
     * Find the representative InfiniumBucket event related to any Infinium event
     */
    private Set<LabEvent> getRelatedBucketEvents(LabEvent event) {
        Set<LabEvent> bucketEvents = new HashSet<>();
        // The DNA plate wells are bucketed
        Set<LabVessel> dnaPlateWells = Collections.EMPTY_SET;
        if (ampEventTypes.contains(event.getLabEventType())) {
            dnaPlateWells = getDnaPlateWellsFromAmpEvent(event);
        } else if (hybEventTypes.contains(event.getLabEventType())) {
            dnaPlateWells = getDnaPlateWellsFromHybEvent(event);
        }

        // Remove any wells without buckets
        for (Iterator<LabVessel> iter = dnaPlateWells.iterator(); iter.hasNext(); ) {
            LabVessel well = iter.next();
            for (LabEvent inPlaceEvent : well.getInPlaceLabEvents()) {
                if (inPlaceEvent.getLabEventType() == LabEventType.INFINIUM_BUCKET) {
                    bucketEvents.add(inPlaceEvent);
                }
            }
        }
        return bucketEvents;
    }

    /**
     * Gets all the DNA plate wells to look for bucket events given an amplification related event. <br/>
     * Amp events are either a section transfer from a DNA plate or an in-place event on an amp plate
     */
    private Set<LabVessel> getDnaPlateWellsFromAmpEvent(LabEvent ampEvent) {
        Set<LabVessel> dnaPlateWells = new HashSet<>();

        LabVessel ampPlate = null;
        if (ampEvent.getInPlaceLabVessel() != null) {
            // Try in-place first - only one amp plate
            ampPlate = ampEvent.getInPlaceLabVessel();
        } else {
            // Will only be one - a section transfer
            ampPlate = ampEvent.getTargetLabVessels().iterator().next();
        }

        for (LabEvent srcEvent : ampPlate.getTransfersTo()) {
            if (srcEvent.getLabEventType() == LabEventType.INFINIUM_AMPLIFICATION) {
                for (SectionTransfer xfer : srcEvent.getSectionTransfers()) {
                    VesselContainer dnaPlate = xfer.getSourceVessel().getContainerRole();
                    for (VesselPosition pos : xfer.getSourceSection().getWells()) {
                        LabVessel well = dnaPlate.getVesselAtPosition(pos);
                        if (well != null) {
                            dnaPlateWells.add(dnaPlate.getVesselAtPosition(pos));
                        }
                    }
                }

            }
        }

        return dnaPlateWells;
    }

    private Set<LabVessel> getDnaPlateWellsFromAmpEvent(LabEvent ampEvent, Set<VesselPosition> ampPositions) {
        Set<LabVessel> dnaPlateWells = getDnaPlateWellsFromAmpEvent(ampEvent);
        if( dnaPlateWells.isEmpty() ) {
            return dnaPlateWells;
        }

        for( Iterator<LabVessel> iter = dnaPlateWells.iterator() ; iter.hasNext() ; ) {
            LabVessel dnaPlateWell = iter.next();
            VesselContainer dnaPlate = dnaPlateWell.getVesselContainers().iterator().next();
            if(!ampPositions.contains(dnaPlate.getPositionOfVessel(dnaPlateWell))){
                iter.remove();
            }
        }

        return dnaPlateWells;
    }

    private Set<LabVessel> getDnaPlateWellsFromHybEvent(LabEvent hybEvent ){
        Set<LabVessel> dnaPlateWells = new HashSet<>();

        Set<VesselPosition> ampPositions = new HashSet<>();
        // Never more than one chip in a hyb event
        LabVessel chip = null;
        LabVessel ampPlate = null;
        Set<CherryPickTransfer> cherryPickTransfers = Collections.EMPTY_SET;
        if( hybEvent.getInPlaceLabVessel() != null ) {
            chip = hybEvent.getInPlaceLabVessel();
            for( LabEvent xferEvent : chip.getTransfersTo() ) {
                if( hybEventTypes.contains(xferEvent.getLabEventType())){
                    cherryPickTransfers = xferEvent.getCherryPickTransfers();
                    break;
                }
            }
        } else {
            cherryPickTransfers = hybEvent.getCherryPickTransfers();
        }

        for( CherryPickTransfer xfer : cherryPickTransfers ) {
            // All the same chip and amp plate
            chip = xfer.getTargetVessel();
            ampPlate = xfer.getSourceVessel();

            // Unique amp wells
            ampPositions.add(xfer.getSourcePosition());
        }

        for( LabEvent ampEvent : ampPlate.getTransfersTo() ) {
            // InfiniumAmplification would be the only event
            if ( ampEventTypes.contains(ampEvent.getLabEventType()) ) {
                dnaPlateWells = getDnaPlateWellsFromAmpEvent( ampEvent, ampPositions );
                break;
            }
        }

        return dnaPlateWells;
    }

    /**
     * From an Infinium bucket event on a DNA plate chip well, look backwards and forwards for all related events
     * mapped to batch and sample data of bucketed plate well<br />
     *
     * @return ArrayDtos representing the events in an array process flow starting at DNA plate well
     * to be mapped in ETL to horizontal columns in a single row keyed by LCSET and aliquot sample ID
     */
    private List<ArrayDto> makeDtosFromBucketEvent(LabEvent bucketEvent) {

        List<ArrayDto> arrayDtos = new ArrayList<>();

        // Infinium always uses the DNA plate well for the bucket
        PlateWell dnaPlateWell = null;
        if (OrmUtil.proxySafeIsInstance(bucketEvent.getInPlaceLabVessel(), PlateWell.class)) {
            dnaPlateWell = OrmUtil.proxySafeCast(bucketEvent.getInPlaceLabVessel(), PlateWell.class);
        } else {
            // Die if not a DNA plate well (very unlikely)
            logErrors.add("InfiniumBucket event does not contain a plate well, ID: " + bucketEvent.getLabEventId());
            return arrayDtos;
        }
        // Get the DNA Plate, lab batch, and sample details
        VesselPosition dnaPlatePosition = dnaPlateWell.getVesselPosition();
        LabVessel dnaPlate = dnaPlateWell.getPlate();

        Set<BucketEntry> wellBucketEntries = dnaPlateWell.getBucketEntries();
        BucketEntry wellbucket = null;
        LabBatch arrayBatch = null;
        ProductOrder pdo = null;
        // An infiniumBucket entry event without a bucket entry?  Should never happen, but don't even bother.
        if( wellBucketEntries == null || wellBucketEntries.isEmpty() ) {
            logErrors.add("InfiniumBucket event not associated with a batch: " + bucketEvent.getLabEventId());
            return arrayDtos;
        } else {
            // Will a DNA plate well ever be in multiple buckets?  Trusting that it doesn't happen.
            wellbucket = wellBucketEntries.iterator().next();
            arrayBatch = wellbucket.getLabBatch();
            // As of 04/2018 no nulls exist
            pdo = wellbucket.getProductOrder();
        }

        if( arrayBatch == null ) {
            logErrors.add("Infinium bucket entry event not associated with a batch: " + bucketEvent.getLabEventId());
            return arrayDtos;
        }

        // Get samples:  LCSET (nearest) sample name and PDO (earliest) sample name
        Pair<String,String> sampleIds = getWellSampleInstance( dnaPlateWell, arrayBatch );
        if( sampleIds == null ) {
            logErrors.add("No sample IDs related to Infinium plate well " + dnaPlateWell.getLabel() );
            return arrayDtos;
        }

        // Try to find plating event details
        Set<LabEvent> dnaPlateXfers = dnaPlateWell.getTransfersTo();
        LabEvent platingEvent = null;
        // There should never be more than 1 section transfer event, but make sure it's ArrayPlatingDilution
        if (dnaPlateXfers != null) {
            for (LabEvent event : dnaPlateXfers) {
                if (event.getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    platingEvent = event;
                    break;
                }
            }
        }
        if (platingEvent == null) {
            logErrors.add("No plating event prior to InfiniumBucket event, ID: " + bucketEvent.getLabEventId());
            // Don't die here, allows process to record plate name from InfiniumBucket but no plating event data
        } else {
            arrayDtos.add( new ArrayDto(platingEvent, dnaPlateWell, dnaPlatePosition,
                pdo.getProductOrderId(), arrayBatch.getBatchName(), sampleIds.getLeft(), sampleIds.getRight() ) );
        }

        // Amp plate event details, could there ever be more than one amp plate?  Handle it.
        Map<LabVessel, VesselPosition> ampPlates = new HashMap<>();
        for( SectionTransfer sectionTransfer : dnaPlate.getContainerRole().getSectionTransfersFrom() ) {
            LabVessel ampPlate = null;
            if( sectionTransfer.getLabEvent().getLabEventType() == LabEventType.INFINIUM_AMPLIFICATION ) {
                ampPlate = sectionTransfer.getTargetVessel();
                VesselPosition ampPosition = sectionTransfer.getTargetSection().getWells().get( sectionTransfer.getSourceSection().getWells().indexOf(dnaPlatePosition));
                ampPlates.put(ampPlate, ampPosition);
                arrayDtos.add( new ArrayDto(sectionTransfer.getLabEvent(), ampPlate, ampPosition,
                        pdo.getProductOrderId(), arrayBatch.getBatchName(), sampleIds.getLeft(), sampleIds.getRight() ) );
                for( LabEvent inPlaceEvent : getInPlaceArrayEvents( ampPlate ) )  {
                    arrayDtos.add( new ArrayDto(inPlaceEvent, ampPlate, ampPosition,
                            pdo.getProductOrderId(), arrayBatch.getBatchName(), sampleIds.getLeft(), sampleIds.getRight() ) );
                }
            }
        }

        // Chip event details
        for( Map.Entry<LabVessel, VesselPosition> vesselPositionEntry : ampPlates.entrySet()){
            LabVessel ampPlate = vesselPositionEntry.getKey();
            VesselPosition ampPosition = vesselPositionEntry.getValue();
            for( CherryPickTransfer cherryPickTransfer : ampPlate.getContainerRole().getCherryPickTransfersFrom() ) {
                if( cherryPickTransfer.getSourcePosition() == ampPosition && hybEventTypes.contains( cherryPickTransfer.getLabEvent().getLabEventType())) {
                    LabVessel chip = cherryPickTransfer.getTargetVessel();
                    VesselPosition chipPosition = cherryPickTransfer.getTargetPosition();
                    arrayDtos.add( new ArrayDto(cherryPickTransfer.getLabEvent(), chip, chipPosition,
                            pdo.getProductOrderId(), arrayBatch.getBatchName(), sampleIds.getLeft(), sampleIds.getRight() ) );
                    for( LabEvent inPlaceEvent : getInPlaceArrayEvents( chip ) )  {
                        arrayDtos.add( new ArrayDto(inPlaceEvent, chip, chipPosition,
                                pdo.getProductOrderId(), arrayBatch.getBatchName(), sampleIds.getLeft(), sampleIds.getRight() ) );
                    }
                }
            }
        }

        return arrayDtos;
    }

    private List<LabEvent> getInPlaceArrayEvents( LabVessel labVessel ) {
        List<LabEvent> inPlaceEvents = new ArrayList<>();
        for( LabEvent inPlaceEvent : labVessel.getInPlaceLabEvents() )  {
            if( hybEventTypes.contains(inPlaceEvent.getLabEventType()) || ampEventTypes.contains(inPlaceEvent.getLabEventType()) ) {
                inPlaceEvents.add( inPlaceEvent );
            }
        }
        return inPlaceEvents;
    }

    /**
     * Find pair of LCSET (nearest) sample name and PDO (earliest) sample name for a DNA plate well
     */
    private Pair<String,String> getWellSampleInstance(LabVessel dnaPlateWell, LabBatch arrayBatch ){
        // Get samples
        Set<SampleInstanceV2> dnaWellSampleInstances;

        // All downstream Infinium events share samples of DNA plate well (should only ever be 1)
        dnaWellSampleInstances = dnaPlateWell.getSampleInstancesV2();
        if (dnaWellSampleInstances == null ||  dnaWellSampleInstances.isEmpty()) {
            // Process is useless
            logErrors.add("No sampleInstances for DNA plate well " + dnaPlateWell.getLabel());
        } else {
            for (SampleInstanceV2 si : dnaWellSampleInstances) {
                // All we care about is the array batch
                for(BucketEntry bucketEntry : si.getAllBucketEntries() ) {
                    if( bucketEntry.getLabBatch() != null
                            && bucketEntry.getLabBatch().getBatchName().equals(arrayBatch.getBatchName())) {
                        return Pair.of( si.getNearestMercurySampleName(),
                                si.getEarliestMercurySampleName());
                    }
                }
            }
        }
        // Nothing found
        return null;
    }

    @Override
    public void postEtlLogging() {

        for (String msg : logErrors) {
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
     * Scope relaxed from protected to public to allow a backfill service hook <br/>
     * This overwrite handles when all entities are attached in the persistence context
     */
    @Override
    public int writeRecords(Collection<LabEvent> entities,
                            Collection<Long>deletedEntityIds,
                            String etlDateStr) throws Exception {
        return super.writeRecords(entities, deletedEntityIds, etlDateStr);
    }

    /**
     * Scope relaxed from protected to public to allow a backfill service hook <br/>
     * This overwrite handles when the persistence context is cleared and all we have are numeric IDs
     */
    @Override
    public int writeRecords(Collection<Long> deletedEntityIds,
                            Collection<Long> modifiedEntityIds,
                            Collection<Long> addedEntityIds,
                            Collection<RevInfoPair<LabEvent>> revInfoPairs,
                            String etlDateStr) throws Exception {
        Collection<Long> nonDeletedIds = new ArrayList<>();
        nonDeletedIds.addAll(modifiedEntityIds);
        nonDeletedIds.addAll(addedEntityIds);

        Collection<LabEvent> eventList = new ArrayList<>();
        LabEvent event;
        for (Long entityId : nonDeletedIds) {
            event = dao.findById( LabEvent.class, entityId );
            if( event != null ) {
                eventList.add(event);
            }
        }
        return writeRecords( eventList, deletedEntityIds, etlDateStr );
    }

    /**
     * Holds extract data for array process flow ETL. <br/>
     * The array process flow is tracked by PDO, LCSET, and LCSET sample name
     */
    public static class ArrayDto {
        private LabEvent labEvent;
        private LabVessel labVessel;
        private VesselPosition vesselPosition;
        private Long productOrderId;
        private String batchName;
        private String lcsetSampleName;
        private String sampleName;

        /**
         * Encapsulates a single event in the array process flow <br/>
         * The flow is tracked by mandatory fields: PDO, LCSET, and LCSET sample name
         **/
        ArrayDto(LabEvent labEvent, LabVessel labVessel, VesselPosition vesselPosition,
                 Long productOrderId, String batchName, String lcsetSampleName, String sampleName) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
            this.vesselPosition = vesselPosition;
            this.productOrderId = productOrderId;
            this.batchName = batchName;
            this.lcsetSampleName = lcsetSampleName;
            this.sampleName = sampleName;
        }

        public String toEtlString(String etlDateStr, boolean isDelete) {
            return genericRecord(etlDateStr, isDelete,
                    format(productOrderId),
                    format(batchName),
                    format(lcsetSampleName),
                    format(sampleName),
                    format(labEvent.getLabEventId() ),
                    format(labEvent.getLabEventType().getName()),
                    format(labEvent.getEventLocation()),
                    format(labEvent.getEventDate()),
                    format(labVessel.getLabVesselId()),
                    format(vesselPosition == null ? "" : vesselPosition.toString())
            );
        }
    }
}

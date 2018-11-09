package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
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
@TransactionManagement(TransactionManagementType.BEAN)
public class ArrayProcessFlowEtl extends GenericEntityEtl<LabEvent, LabEvent> {
    private final List<String> logErrors = new ArrayList<>();
    private final Set<Long> loggingDeletedEventIds = new HashSet<>();

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

        ArrayDto arrayFlowDto = null;

        if (entity == null) {
            return (List<String>) Collections.EMPTY_LIST;
        }

        // Supports overrides but not deletes
        if (isDelete) {
            loggingDeletedEventIds.add(entity.getLabEventId());
            return (List<String>) Collections.EMPTY_LIST;
        }

        LabEventType eventType = entity.getLabEventType();

        try {
            if (eventType == LabEventType.INFINIUM_BUCKET) {
                // An ARRAY LCSET has been created: LCSET and PDO values are available so get all associated events
                arrayFlowDto = makeDtosFromBucketEvent(entity);
            } else {
                return (List<String>) Collections.EMPTY_LIST;
            }
        } catch (Exception e) {
            // Uncaught RuntimeExceptions kill the injected LabEventEtl in ExtractTransform.
            logger.error("Error in array flow etl", e);
            logErrors.add("Error in array flow etl, EventID: " + entity.getLabEventId() + ", Type: "
                          + entity.getLabEventType().getName() + ", Error: " + e.getMessage());
        }

        if ( arrayFlowDto == null ) {
            return (List<String>) Collections.EMPTY_LIST;
        }

        List<String> records = new ArrayList<>();
        records.add(arrayFlowDto.toEtlString(etlDateStr, isDelete));
        return records;
    }

    /**
     * From an Infinium bucket event on a DNA plate chip well, look backwards to obtain plating event details and
     * map to PDO, LCSET, and sample data of bucketed plate well<br />
     *
     * @return An ArrayDto row representing the plating eventin an array process flow starting at DNA plate well
     * to be mapped in ETL to horizontal columns in a single row keyed by PDO, LCSET, and aliquot sample ID
     */
    private ArrayDto makeDtosFromBucketEvent(LabEvent bucketEvent) {

        // Infinium always uses the DNA plate well for the bucket
        PlateWell dnaPlateWell = null;
        if (OrmUtil.proxySafeIsInstance(bucketEvent.getInPlaceLabVessel(), PlateWell.class)) {
            dnaPlateWell = OrmUtil.proxySafeCast(bucketEvent.getInPlaceLabVessel(), PlateWell.class);
        } else {
            // Die if not a DNA plate well (very unlikely)
            logErrors.add("InfiniumBucket event does not contain a plate well, ID: " + bucketEvent.getLabEventId());
            return null;
        }
        VesselPosition dnaPlatePosition = dnaPlateWell.getVesselPosition();

        Set<BucketEntry> wellBucketEntries = dnaPlateWell.getBucketEntries();
        BucketEntry wellbucket = null;
        LabBatch arrayBatch = null;
        ProductOrder pdo = null;
        // An infiniumBucket entry event without a bucket entry?  Should never happen, but don't even bother.
        if( wellBucketEntries == null || wellBucketEntries.isEmpty() ) {
            logErrors.add("InfiniumBucket event not associated with a batch: " + bucketEvent.getLabEventId());
            return null;
        } else {
            // Will a DNA plate well ever be in multiple buckets?  Have to trust that it will never happen.
            wellbucket = wellBucketEntries.iterator().next();
            arrayBatch = wellbucket.getLabBatch();
            // As of 04/2018 no nulls exist
            pdo = wellbucket.getProductOrder();
        }

        if( arrayBatch == null ) {
            logErrors.add("Infinium bucket entry event not associated with a batch: " + bucketEvent.getLabEventId());
            return null;
        }

        // Try to find plating event details - may not exist if daughter plate creation was mistakenly the event
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
        }

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
                        return new ArrayDto(platingEvent, dnaPlateWell, dnaPlatePosition,
                                pdo.getProductOrderId(),
                                arrayBatch.getBatchName(),
                                si.getNearestMercurySampleName(),
                                si.getEarliestMercurySampleName());
                    }
                }
            }
        }

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
                    format( labEvent == null ? null : labEvent.getLabEventId() ),
                    // Event type not saved - simply flags these fields for plating related data
                    format( labEvent == null ? "ArrayPlatingDilution" : labEvent.getLabEventType().getName()),
                    format( labEvent == null ? null : labEvent.getEventLocation()),
                    format( labEvent == null ? labVessel.getCreatedOn() : labEvent.getEventDate()),
                    format(labVessel.getLabVesselId()),
                    format(vesselPosition == null ? "" : vesselPosition.toString())
            );
        }
    }
}

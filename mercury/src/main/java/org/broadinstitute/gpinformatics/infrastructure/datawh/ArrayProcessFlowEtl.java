package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

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
        if( dnaPlateXfers != null ) {
            for (LabEvent event : dnaPlateXfers) {
                if (event.getLabEventType() == LabEventType.ARRAY_PLATING_DILUTION) {
                    platingEvent = event;
                    break;
                }
            }
        }

        if (platingEvent == null) {
            logErrors.add("No plating event prior to InfiniumBucket event, ID: " + bucketEvent.getLabEventId());
            // Don't die here, continue looking for downstream events
        }

        VesselPosition dnaPlatePosition = dnaPlateWell.getVesselPosition();
        // All downstream Infinium events share samples of DNA plate well (should only ever be 1)
        dnaWellSampleInstances = dnaPlateWell.getSampleInstancesV2();

        for( SampleInstanceV2 si : dnaWellSampleInstances) {
            if( si.getSingleBucketEntry() != null ) {

                if(si.getSingleBatch()==null){
                    logErrors.add("Extracting data, but FYI SampleInstance has single bucket entry and null single batch for bucket event " + bucketEvent.getLabEventId() + ", plating event " + platingEvent.getLabEventId());
                }

                arrayFlowDtos.add(new ArrayDto(platingEvent, dnaPlateWell, dnaPlatePosition,
                        si.getSingleBucketEntry().getProductOrder().getProductOrderId(),
                        si.getSingleBucketEntry().getLabBatch().getBatchName(),
                        si.getNearestMercurySampleName(),
                        si.getEarliestMercurySampleName())
                );
            } else {
                logErrors.add("No single bucket entry for DNA plate well " + dnaPlateWell.getLabel() + ", InfiniumBucket event " + bucketEvent.getLabEventId() );
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
        private VesselPosition vesselPosition;
        private Long productOrderId;
        private String batchName;
        private String lcsetSampleName;
        private String sampleName;

        /**
         * Encapsulates a single event in the array process flow <br/>
         * The flow is tracked by mandatory fields: PDO, LCSET, and LCSET sample name
         **/
        ArrayDto( LabEvent labEvent, LabVessel labVessel, VesselPosition vesselPosition,
                  Long productOrderId, String batchName, String lcsetSampleName, String sampleName) {
            this.labEvent = labEvent;
            this.labVessel = labVessel;
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
                    format(labVessel.getLabVesselId()),
                    format(vesselPosition==null?"":vesselPosition.toString())
            );
        }
    }
}

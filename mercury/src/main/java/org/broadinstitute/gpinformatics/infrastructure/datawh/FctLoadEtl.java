package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tied to LabEvent entity, but is only interested in ETL of loaded flowcell tickets as created for MISEQ and FCT batch types.
 */
@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class FctLoadEtl extends GenericEntityEtl<LabEvent,LabEvent> {

    // Events related to loading of FCT tickets that this process is only interested in
    private List<LabEventType> labEventTypes = Arrays.asList(
            LabEventType.FLOWCELL_TRANSFER,
            LabEventType.DENATURE_TO_FLOWCELL_TRANSFER,
            LabEventType.DILUTION_TO_FLOWCELL_TRANSFER,
            LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER);

    // Spare the overhead of recreating for the overwhelming percentage of events which are ignored
    private Collection<String> emptyList = Collections.emptyList();

    public FctLoadEtl() {
    }

    @Inject
    public FctLoadEtl(LabEventDao dao) {
        super(LabEvent.class, "fct_load", "lab_event_aud",
                "lab_event_id", dao);
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
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId){
        return dataRecords( etlDateStr, isDelete, dao.findById(LabEvent.class, entityId) );
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, LabEvent labEvent) {
        if (labEvent == null ) {
            return emptyList;
        }

        // Ignore all other event types - this process is only interested in flowcell transfer events
        LabEventType labEventType = labEvent.getLabEventType();
        if( !labEventTypes.contains( labEventType )  ) {
            return emptyList;
        }

        Collection<String> records = new ArrayList<>();

        for( LabVessel target : labEvent.getTargetLabVessels() ) {

            // Loading tubes are the dilution vessels registered in batch_starting_vessels
            Pair<IlluminaFlowcell, Set<LabVessel>> flowcellAndLoadingTubes = getFlowcellAndLoadingTubes(labEvent, target);
            if (flowcellAndLoadingTubes == null || flowcellAndLoadingTubes.getRight().isEmpty()) {
                continue;
            }

            for (LabVessel loadingTube : flowcellAndLoadingTubes.getRight()) {
                // Probably never happen, but cheap NPE insurance
                if( loadingTube == null ) {
                    continue;
                }

                if (labEventType == LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER) {
                    records = getMiSeqDataRecords( etlDateStr, labEvent, loadingTube, flowcellAndLoadingTubes.getLeft() );
                } else {
                    records = getDataRecordsForLoadingTube(etlDateStr, loadingTube,
                            flowcellAndLoadingTubes.getLeft());
                    // When a loading tube is used on a flowcell, all batch_starting_vessels for FCT are included in ETL
                    if( records.size() > 0 ) {
                        break;
                    }
                }
            }
        }

        return records;
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    /**
     * Get the event flowcell and the nearest source tubes transferred to the flowcell
     * @param labEvent The event of interest
     * @return The flowcell and the tubes directly transferred to it.  We're calling them loading tubes here - stored as dilution vessel in BatchStartingVessel
     */
    private Pair<IlluminaFlowcell,Set<LabVessel>> getFlowcellAndLoadingTubes( LabEvent labEvent, LabVessel target ) {
        LabEventType labEventType = labEvent.getLabEventType();

        Set<LabVessel> loadingTubes = new HashSet<>();

        IlluminaFlowcell flowcell;
        if( !OrmUtil.proxySafeIsInstance(target, IlluminaFlowcell.class)) {
            // Process is only interested in Illumina flowcells
            return null;
        } else {
            flowcell = OrmUtil.proxySafeCast(target, IlluminaFlowcell.class);
        }

        if( labEventType == LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER ) {
            LabVessel reagentKit = labEvent.getSourceLabVessels().iterator().next();
            // Preceded by LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER, get MiSeq flowcell
            for( LabEvent ancestorEvent : reagentKit.getTransfersTo() ) {
                if( ancestorEvent.getLabEventType() == LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER ) {
                    loadingTubes.addAll( ancestorEvent.getSourceVesselTubes() );
                }
            }
        } else {
            List<LabVessel.VesselEvent> ancestors = new ArrayList<>();
            for (VesselPosition pos : flowcell.getContainerRole().getPositions()) {
                ancestors.addAll(flowcell.getContainerRole().getAncestors(pos));
            }
            // Strip tubes are registered in batch_starting_vessels.dilution_vessel, always a 1:1 relationship to flowcell
            if (ancestors.get(0).getSourceVesselContainer() != null &&
                    ancestors.get(0).getSourceVesselContainer().getEmbedder().getType() == LabVessel.ContainerType.STRIP_TUBE) {
                loadingTubes.add( ancestors.get(0).getSourceVesselContainer().getEmbedder() );
            } else {
                // Otherwise barcoded tubes are registered in batch_starting_vessels.dilution_vessel, an n:1 relationship to flowcell
                Map<VesselPosition,LabVessel> loadedVesselsAndPosition = flowcell.getNearestTubeAncestorsForLanes();
                loadingTubes.addAll(loadedVesselsAndPosition.values());
            }
        }

        return Pair.of(flowcell,loadingTubes);
    }

    /**
     * Build ETL data records for MiSeq event
     * MISEQ batch logic is driven exclusively from vessels registered in LabBatchStartingVessel#labVessel
     */
    private Collection<String> getMiSeqDataRecords(String etlDateStr, LabEvent labEvent, LabVessel loadingTube, IlluminaFlowcell flowcell) {
        Collection<String> records = new ArrayList<>();
        Map<LabVessel, Long> latestBatchVessels = new HashMap<>();
        for (LabBatchStartingVessel labBatchStartingVessel : loadingTube
                .getLabBatchStartingVesselsByDate()) {
            LabBatch miseqBatch = labBatchStartingVessel.getLabBatch();
            // Need to check date so backfill doesn't override older miseq tickets
            if (miseqBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ &&
                miseqBatch.getCreatedOn().before(labEvent.getEventDate())) {
                latestBatchVessels.put(labBatchStartingVessel.getLabVessel(),
                        labBatchStartingVessel.getBatchStartingVesselId());
            }
        }
        for (Long batchVesselId : latestBatchVessels.values()) {
            records.add(genericRecord(etlDateStr, false,
                    batchVesselId,
                    format(flowcell.getLabel())
            ));
        }
        return records;
    }

    /**
     * Build ETL data records for a flowcell transfer
     * A dilution vessel (loading tube) registered in batch_starting_vessels
     *   associates a single flowcell barcode to an entire FCT batch
     * As of 10/18/2018, a single dilution vessel is never used to load more than a single flowcell
     */
    private Collection<String> getDataRecordsForLoadingTube(String etlDateStr, LabVessel loadingTube, IlluminaFlowcell flowcell) {
        Collection<String> records = new ArrayList<>();
        Set<LabBatchStartingVessel> batchStartingVessels = loadingTube.getDilutionReferences();
        if( batchStartingVessels.size() > 0 ) {
            LabBatch fctBatch = batchStartingVessels.iterator().next().getLabBatch();
            for ( LabBatchStartingVessel labBatchStartingVessel : fctBatch.getLabBatchStartingVessels() ) {
                records.add(genericRecord(etlDateStr, false,
                        labBatchStartingVessel.getBatchStartingVesselId(),
                        format(flowcell.getLabel())
                ));
            }
        }
        return records;
    }
}

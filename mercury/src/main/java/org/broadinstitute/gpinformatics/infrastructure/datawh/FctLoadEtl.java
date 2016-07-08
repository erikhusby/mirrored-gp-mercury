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

        // Ignore all other batch types - this process is only interested in flowcell tickets
        LabEventType labEventType = labEvent.getLabEventType();
        if( !labEventTypes.contains( labEventType )  ) {
            return emptyList;
        }

        Pair<IlluminaFlowcell,Set<LabVessel>> flowcellAndSourceTubes = getFlowcellAndSourceTubes( labEvent );
        if( flowcellAndSourceTubes == null || flowcellAndSourceTubes.getRight().isEmpty() ) {
            return emptyList;
        }

        Collection<String> records = new ArrayList<>();

        for( LabVessel srcTube : flowcellAndSourceTubes.getRight() ) {

            if( labEventType == LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER ) {
                // MISEQ batch logic is driven exclusively from vessels registered in LabBatchStartingVessel#labVessel
                Map<LabVessel,Long> latestBatchVessels = new HashMap<>();
                for (LabBatchStartingVessel labBatchStartingVessel : srcTube.getLabBatchStartingVesselsByDate()) {
                    LabBatch miseqBatch = labBatchStartingVessel.getLabBatch();
                    // Need to check date so backfill doesn't override older miseq tickets
                    if( miseqBatch.getLabBatchType() == LabBatch.LabBatchType.MISEQ &&
                            miseqBatch.getCreatedOn().before(labEvent.getEventDate())) {
                        latestBatchVessels.put(labBatchStartingVessel.getLabVessel(),labBatchStartingVessel.getBatchStartingVesselId());
                    }
                }
                for( Long batchVesselId : latestBatchVessels.values()) {
                    records.add(genericRecord(etlDateStr, isDelete,
                            batchVesselId,
                            format(flowcellAndSourceTubes.getLeft().getLabel())
                    ));
                }
            } else {
                // FCT logic is driven exclusively from vessels registered in LabBatchStartingVessel#dilutionVessel
                // Older events with only denatured tubes registered in LabBatchStartingVessel#labVessel
                //     produce non-deterministic flowcell barcodes
                for (LabBatchStartingVessel labBatchStartingVessel : srcTube.getDilutionReferences()) {
                    records.add(genericRecord(etlDateStr, isDelete,
                            labBatchStartingVessel.getBatchStartingVesselId(),
                            format(flowcellAndSourceTubes.getLeft().getLabel())
                    ));
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
     * @return
     */
    private Pair<IlluminaFlowcell,Set<LabVessel>> getFlowcellAndSourceTubes(LabEvent labEvent ) {
        LabEventType labEventType = labEvent.getLabEventType();
        IlluminaFlowcell flowcell = OrmUtil.proxySafeCast(labEvent.getTargetLabVessels().iterator().next(),
                IlluminaFlowcell.class);
        Set<LabVessel> dilutionTubes = new HashSet<>();

        if( labEventType == LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER ) {
            LabVessel reagentKit = labEvent.getSourceLabVessels().iterator().next();
            // Preceded by LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER, get MiSeq flowcell
            for( LabEvent ancestorEvent : reagentKit.getTransfersTo() ) {
                if( ancestorEvent.getLabEventType() == LabEventType.DENATURE_TO_REAGENT_KIT_TRANSFER ) {
                    dilutionTubes.addAll( ancestorEvent.getSourceVesselTubes() );
                }
            }
        } else {
            List<LabVessel.VesselEvent> ancestors = flowcell.getContainerRole().getAncestors(VesselPosition.LANE1);
            // Strip tubes are registered in flowcell ticket as dilution vessels in a FLOWCELL_TRANSFER event
            if (ancestors.get(0).getSourceVesselContainer() != null &&
                    ancestors.get(0).getSourceVesselContainer().getEmbedder().getType() == LabVessel.ContainerType.STRIP_TUBE) {
                dilutionTubes.add( ancestors.get(0).getSourceVesselContainer().getEmbedder() );
            } else {
                // Otherwise barcoded tubes are registered in flowcell ticket as dilution vessels
                Map<VesselPosition,LabVessel> loadedVesselsAndPosition = flowcell.getNearestTubeAncestorsForLanes();
                dilutionTubes.addAll(loadedVesselsAndPosition.values());
            }
        }

        if( flowcell != null && dilutionTubes != null ) {
            return Pair.of(flowcell,dilutionTubes);
        } else {
            return null;
        }
    }

}

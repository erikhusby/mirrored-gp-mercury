package org.broadinstitute.gpinformatics.infrastructure.datawh;

import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Tied to LabEvent entity, but is only interested in ETL of loaded flowcell tickets as created for MISEQ and FCT batch types.
 */
@Stateful
public class FctLoadEtl extends GenericEntityEtl<LabEvent,LabEvent> {

    // Events related to loading of FCT tickets that this process is only interested in
    List<LabEventType> labEventTypes = Arrays.asList(
            LabEventType.FLOWCELL_TRANSFER,
            LabEventType.DENATURE_TO_FLOWCELL_TRANSFER,
            LabEventType.DILUTION_TO_FLOWCELL_TRANSFER,
            LabEventType.REAGENT_KIT_TO_FLOWCELL_TRANSFER);

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
            return Collections.emptyList();
        }

        // Ignore all other batch types - this process is only interested in flowcell tickets
        LabEventType labEventType = labEvent.getLabEventType();
        if( !labEventTypes.contains( labEventType )  ) {
            return Collections.emptyList();
        }

        Collection<String> records = new ArrayList<>();
        // Typically DENATURE_TO_FLOWCELL_TRANSFER and DILUTION_TO_FLOWCELL_TRANSFER
        for (VesselToSectionTransfer vesselToSectionTransfer : labEvent.getVesselToSectionTransfers()) {
            LabVessel srcVessel = vesselToSectionTransfer.getSourceVessel();
            LabVessel targetVessel = vesselToSectionTransfer.getTargetVesselContainer().getEmbedder();
            IlluminaFlowcell flowcell = null;
            String flowcellType = null;
            if( OrmUtil.proxySafeIsInstance (targetVessel, IlluminaFlowcell.class) ){
                flowcell = OrmUtil.proxySafeCast(targetVessel, IlluminaFlowcell.class);
                flowcellType = flowcell.getFlowcellType().getDisplayName();
            }
            // Dilution/strip tubes are never re-used so for backfill or out of sequence data don't have to worry
            //   about finding the latest FCT
            // Legacy HiSeq 2500 Flowcell is only one row with no lanes - split into 2 rows, LANE1 and LANE2?
            for (LabBatchStartingVessel labBatchStartingVessel : srcVessel.getDilutionReferences()) {
                LabBatch labBatch = labBatchStartingVessel.getLabBatch();
                if( labBatchStartingVessel.getVesselPosition()!= null ) {
                    records.add(genericRecord(etlDateStr, isDelete,
                            labBatchStartingVessel.getBatchStartingVesselId(),
                            labBatch.getLabBatchId(),
                            format(labBatch.getBatchName()),
                            format(labBatch.getLabBatchType().toString()),
                            format(labBatchStartingVessel.getLabVessel().getLabel()),
                            format(labBatchStartingVessel.getDilutionVessel() != null ? labBatchStartingVessel.getDilutionVessel().getLabel() : ""),
                            format(labBatch.getCreatedOn()),
                            format(targetVessel.getLabel()),
                            format(labBatch.getFlowcellType() != null ? labBatch.getFlowcellType().getDisplayName() : flowcellType),
                            format(labBatchStartingVessel.getVesselPosition() != null ? labBatchStartingVessel.getVesselPosition().toString() : ""),
                            format(labBatchStartingVessel.getConcentration()),
                            labBatch.getLabBatchType() != LabBatch.LabBatchType.MISEQ ? "N" : "Y"
                    ));
                } else {
                    // ***** FAIL duplicate labBatchStartingVessel.getBatchStartingVesselId() PK created
                    for (VesselPosition vesselPosition : vesselToSectionTransfer.getTargetVesselContainer().getPositions()) {
                        records.add(genericRecord(etlDateStr, isDelete,
                                labBatchStartingVessel.getBatchStartingVesselId(),
                                labBatch.getLabBatchId(),
                                format(labBatch.getBatchName()),
                                format(labBatch.getLabBatchType().toString()),
                                format(labBatchStartingVessel.getLabVessel().getLabel()),
                                format(labBatchStartingVessel.getDilutionVessel() != null ? labBatchStartingVessel.getDilutionVessel().getLabel() : ""),
                                format(labBatch.getCreatedOn()),
                                format(targetVessel.getLabel()),
                                format(labBatch.getFlowcellType() != null ? labBatch.getFlowcellType().getDisplayName() : flowcellType),
                                format(vesselPosition.toString()),
                                format(labBatchStartingVessel.getConcentration()),
                                labBatch.getLabBatchType() != LabBatch.LabBatchType.MISEQ ? "N" : "Y"
                        ));
                    }

                }
            }
        }

        // Skip section transfers if vessel to section transfer above had data,
        //   there will never be a mix of vessel transfer types to a flowcell
        if(records.size() == 0 ) {
            // Typically FLOWCELL_TRANSFER, REAGENT_KIT_TO_FLOWCELL_TRANSFER
            Map<LabBatch,List<LabBatchStartingVessel>> batches = new TreeMap<>(LabBatch.byDate);
            Map<LabBatchStartingVessel,LabVessel> flowcellMap = new HashMap<>();
            // Do a first pass to get all batches
            for (SectionTransfer sectionTransfer : labEvent.getSectionTransfers()) {
                LabVessel srcVessel = sectionTransfer.getSourceVesselContainer().getEmbedder();
                LabVessel flowcell = sectionTransfer.getTargetVesselContainer().getEmbedder();
                for (LabBatchStartingVessel labBatchStartingVessel : srcVessel.getLabBatchStartingVessels()) {
                    if( !batches.containsKey(labBatchStartingVessel.getLabBatch())) {
                        batches.put( labBatchStartingVessel.getLabBatch(), new ArrayList<LabBatchStartingVessel>());
                    }
                    batches.get(labBatchStartingVessel.getLabBatch()).add(labBatchStartingVessel);
                    flowcellMap.put(labBatchStartingVessel,flowcell);
                }
            }

            LabBatch latestValidBatch = null;
            if (batches.size() == 1) {
                latestValidBatch = batches.keySet().iterator().next();
            } else if( batches.size() > 1 ) {
                Date eventDate = labEvent.getEventDate();
                for( LabBatch batch : batches.keySet() ) {
                    if( batch.getCreatedOn().before(eventDate)) {
                        latestValidBatch = batch;
                    } else {
                        // We're done
                        break;
                    }
                }
            }

            List<LabBatchStartingVessel> batchVessels;
            if( latestValidBatch != null ) {
                batchVessels = batches.get(latestValidBatch);
                LabVessel flowcell = flowcellMap.get(latestValidBatch);

                for (LabBatchStartingVessel labBatchStartingVessel : batchVessels) {
                    LabBatch labBatch = labBatchStartingVessel.getLabBatch();

                    records.add(genericRecord(etlDateStr, isDelete,
                            labBatchStartingVessel.getBatchStartingVesselId(),
                            labBatch.getLabBatchId(),
                            format(labBatch.getBatchName()),
                            format(labBatch.getLabBatchType().toString()),
                            format(labBatchStartingVessel.getLabVessel().getLabel()),
                            format(labBatchStartingVessel.getDilutionVessel() != null ? labBatchStartingVessel.getDilutionVessel().getLabel() : ""),
                            format(labBatch.getCreatedOn()),
                            format(flowcell.getLabel()),
                            format(labBatch.getFlowcellType() != null ? labBatch.getFlowcellType().getDisplayName() : flowcell.getType().getName()),
                            format(labBatchStartingVessel.getVesselPosition() != null ? labBatchStartingVessel.getVesselPosition().toString() : ""),
                            format(labBatchStartingVessel.getConcentration()),
                            labBatch.getLabBatchType() != LabBatch.LabBatchType.MISEQ ? "N" : "Y"
                    ));
                }
            }
        }

        if(records.size() == 0 ) {
            // Typically REAGENT_KIT_TO_FLOWCELL_TRANSFER
            for (CherryPickTransfer cherryPickTransfer : labEvent.getCherryPickTransfers() ) {
                LabVessel srcVessel = cherryPickTransfer.getSourceVesselContainer().getEmbedder();
                LabVessel flowcell = cherryPickTransfer.getTargetVesselContainer().getEmbedder();
                for (LabBatchStartingVessel labBatchStartingVessel : srcVessel.getLabBatchStartingVessels()) {
//                    if( !batches.containsKey(labBatchStartingVessel.getLabBatch())) {
//                        batches.put( labBatchStartingVessel.getLabBatch(), new ArrayList<LabBatchStartingVessel>());
//                    }
//                    batches.get(labBatchStartingVessel.getLabBatch()).add(labBatchStartingVessel);
//                    flowcellMap.put(labBatchStartingVessel,flowcell);
                }
            }
        }

        return records;
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabEvent entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }
}

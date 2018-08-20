package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.apache.commons.lang3.tuple.MutableTriple;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;

import javax.ejb.Stateful;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Stateful
@TransactionManagement(TransactionManagementType.BEAN)
public class LabVesselEtl extends GenericEntityEtl<LabVessel, LabVessel> {

    public LabVesselEtl() {
    }

    @Inject
    public LabVesselEtl(LabVesselDao dao) {
        super(LabVessel.class, "lab_vessel", "lab_vessel_aud", "lab_vessel_id", dao);
    }

    @Override
    Long entityId(LabVessel entity) {
        return entity.getLabVesselId();
    }

    @Override
    Path rootId(Root<LabVessel> root) {
        return root.get(LabVessel_.labVesselId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long entityId) {
        return dataRecords(etlDateStr, isDelete, dao.findById(LabVessel.class, entityId));
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, LabVessel entity) {
        return genericRecord(etlDateStr, isDelete,
                entity.getLabVesselId(),
                format(entity.getLabel()),
                format(entity.getType().getName()),
                format(entity.getName()),
                format(entity.getCreatedOn())
        );
    }

    /**
     * Does ETL for event_fact and sequencing_sample_fact for any downstream data related to a vessel
     **/
    public MutableTriple<Integer, String, Exception> backfillEtlForVessel(String barcode, String etlDateStr,
            LabEventEtl labEventEtl, ArrayProcessFlowEtl arrayEventEtl, SequencingSampleFactEtl seqRunEtl ) {

        MutableTriple<Integer, String, Exception> countDateException = new MutableTriple<>(null, null, null);
        try {

            int recordCount = 0;

            LabVessel labVessel = dao.findSingleSafely( LabVessel.class, LabVessel_.label, barcode, LockModeType.NONE);
            if( labVessel == null ) {
                countDateException.setRight(new Exception("No vessel found for barcode " + barcode) );
                return countDateException;
            }

            Set<LabEvent> descendantEvents = new HashSet<>();
            descendantEvents.addAll(labVessel.getInPlaceAndTransferToEvents());
            descendantEvents.addAll(labVessel.getTransfersFrom());

            TransferTraverserCriteria.LabEventDescendantCriteria eventCriteria = new TransferTraverserCriteria.LabEventDescendantCriteria();
            if( labVessel.getContainerRole() != null ) {
                labVessel.getContainerRole().applyCriteriaToAllPositions(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            } else {
                labVessel.evaluateCriteria(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
            }

            descendantEvents.addAll(eventCriteria.getAllEvents());

            // Extract all sequencing runs to refresh before (possibly) detaching lab events
            Set<SequencingRun> sequencingRunsToRefresh = new HashSet<>();
            for( LabEvent labEvent : descendantEvents) {
                // Capture sequencing runs in order to refresh sequencing sample fact data
                LabVessel flowcell = null;
                if(labEvent.getLabEventType() == LabEventType.FLOWCELL_TRANSFER ||
                   labEvent.getLabEventType() == LabEventType.DILUTION_TO_FLOWCELL_TRANSFER ||
                   labEvent.getLabEventType() == LabEventType.DENATURE_TO_FLOWCELL_TRANSFER ) {
                    flowcell = labEvent.getTargetLabVessels().iterator().next();
                } else if( labEvent.getLabEventType() == LabEventType.FLOWCELL_LOADED ) {
                    flowcell = labEvent.getInPlaceLabVessel();
                }
                // Collects sequencing runs and build sequencing sample fact data from any descendant flowcells.
                if (OrmUtil.proxySafeIsInstance(flowcell, RunCartridge.class)) {
                    RunCartridge runCartridge = OrmUtil.proxySafeCast(flowcell, RunCartridge.class );
                    sequencingRunsToRefresh.addAll(runCartridge.getSequencingRuns());
                }
            }

            if( sequencingRunsToRefresh.size() > 0 ) {
               recordCount += seqRunEtl.writeRecords(sequencingRunsToRefresh, Collections.EMPTY_SET, etlDateStr);
            }

            if( descendantEvents.size() <= JPA_CLEAR_THRESHOLD ) {
                // Extract entire set of data from attached entities
                recordCount += labEventEtl.writeRecords(descendantEvents, Collections.EMPTY_SET, etlDateStr);
                recordCount += arrayEventEtl.writeRecords(descendantEvents, Collections.EMPTY_SET, etlDateStr);
            } else {
                int eventLinecount = 0;
                // Break up list of Ids and clear hibernate session to avoid high resource consumption
                List<Long> modifiedEntityIds = new ArrayList<>();
                for( LabEvent evt : descendantEvents ) {
                    modifiedEntityIds.add(evt.getLabEventId() );
                }
                int startDex = 0;
                while( startDex < modifiedEntityIds.size() ){
                    int endDex = startDex + JPA_CLEAR_THRESHOLD;
                    if( endDex > modifiedEntityIds.size() ) {
                        endDex = modifiedEntityIds.size();
                    }
                    dao.clear();
                    eventLinecount = labEventEtl.writeRecords(Collections.EMPTY_SET,
                            modifiedEntityIds.subList(startDex, endDex),
                            Collections.EMPTY_SET,
                            Collections.EMPTY_SET, // Collection<RevInfoPair<LabEvent>> not used
                            etlDateStr);
                    eventLinecount += arrayEventEtl.writeRecords(Collections.EMPTY_SET,
                            modifiedEntityIds.subList(startDex, endDex),
                            Collections.EMPTY_SET,
                            Collections.EMPTY_SET, // Collection<RevInfoPair<LabEvent>> not used
                            etlDateStr);

                    startDex = endDex;
                }
                recordCount += eventLinecount;
            }

            countDateException.setLeft(recordCount);
            countDateException.setMiddle(etlDateStr);
        } catch (Exception e) {
            countDateException.setRight(e);
        }

        return countDateException;
    }
}

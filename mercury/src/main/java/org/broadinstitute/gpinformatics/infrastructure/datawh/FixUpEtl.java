package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary;
import org.broadinstitute.gpinformatics.mercury.entity.envers.FixupCommentary_;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToVesselTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatchStartingVessel;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.CrossTypeRevisionChangesReader;
import org.hibernate.envers.RevisionType;

import javax.ejb.Stateful;
import javax.inject.Inject;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Certain fix-up tests can alter the samples, PDOs, and LCSETs associated with event_fact data which
 * may have been ETL'ed by the time the fix-up is performed.
 * This class analyses the lab event entities affected by the fix-up and refreshes any descendant events.
 */
@Stateful
public class FixUpEtl extends GenericEntityEtl<FixupCommentary, FixupCommentary> {

    public FixUpEtl() {
    }

    @Inject
    public FixUpEtl(AuditReaderDao dao) {
        super(FixupCommentary.class, "event_fact", "fixup_commentary_aud", "fixup_commentary_id", dao);
    }

    // Fixup events are layered on top of existing lab event output
    public final String ancestorFileName = "library_ancestry";
    public final String seqSampleFactFileName = "sequencing_sample_fact";


    @Inject
    LabEventEtl labEventEtl;

    @Inject
    SequencingSampleFactEtl sequencingSampleFactEtl;

    @Override
    Long entityId(FixupCommentary entity) {
        return entity.getFixupCommentaryId();
    }

    @Override
    Path rootId(Root<FixupCommentary> root) {
        return root.get(FixupCommentary_.fixupCommentaryId);
    }

    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, Long fixupId) {
        FixupCommentary fixupCommentary = dao.findById(FixupCommentary.class, fixupId);
        return dataRecords( etlDateStr, isDelete, fixupCommentary);
    }

    @Override
    String dataRecord(String etlDateStr, boolean isDelete, FixupCommentary entity) {
        throw new RuntimeException("This method cannot apply to this etl class.");
    }

    /**
     * Functionality in GenericEntityEtl is enhanced to write records to two different files.  <br/>
     * All delete records go in event fact table.  <br/>
     * Last character of line for update/insert records determines which file the record is written to: <br/>
     * "E" is an event fact record <br/>
     * "A" is an ancestry fact record
     */
    @DaoFree
    @Override
    protected int writeRecords(Collection<FixupCommentary> entities,
                               Collection<Long> neverAnyDeletedEntityIds,
                               String etlDateStr) {

        // Creates the wrapped Writer to the sqlLoader data files.
        DataFile eventDataFile = new DataFile(dataFilename(etlDateStr, baseFilename));
        DataFile ancestryDataFile = new DataFile(dataFilename(etlDateStr, ancestorFileName));
        DataFile seqSampleDataFile = new DataFile(dataFilename(etlDateStr, seqSampleFactFileName));
        int count = 0;

        try {
            // Writes the records.
            for (FixupCommentary entity : entities) {

                try {
                    for (String record : dataRecords(etlDateStr, false, entity)) {
                        // Split file writes between events and ancestry ...
                        if( record.endsWith("E") ) {
                            eventDataFile.write(record);
                            count++;
                        } else if ( record.endsWith("A") ) {
                            ancestryDataFile.write(record);
                            count++;
                        } else if ( record.endsWith("S") ) {
                            seqSampleDataFile.write(record);
                            count++;
                        }
                    }
                } catch (Exception e) {
                    // Continues ETL and logs data-specific Mercury exceptions.  Re-throws systemic exceptions
                    // such as when BSP is down in order to stop this run of ETL.
                    if (!isSystemException(e)) {
                        if (errorException == null) {
                            errorException = e;
                        }
                        errorIds.add(dataSourceEntityId(entity));
                    } else {
                        throw new RuntimeException(e);
                    }
                }
            }
        } finally {
            eventDataFile.close();
            ancestryDataFile.close();
            seqSampleDataFile.close();
        }
        return count;
    }

    /**
     * Overridden to gather LabEvent objects for entity ids and pass to writer so the file writes are forked
     * into event or ancestry file
     */
    @Override
    protected int writeRecords(Collection<Long> neverAnyDeletedEntityIds,
                               Collection<Long> modifiedEntityIds,
                               Collection<Long> addedEntityIds,
                               Collection<RevInfoPair<FixupCommentary>> ignored,
                               String etlDateStr) {

        Collection<Long> nonDeletedIds = new ArrayList<>();
        nonDeletedIds.addAll(modifiedEntityIds);
        nonDeletedIds.addAll(addedEntityIds);

        Collection<FixupCommentary> fixupList = new ArrayList<>();
        FixupCommentary fixup;
        for (Long entityId : nonDeletedIds) {
            fixup = dao.findById( FixupCommentary.class, entityId );
            if( fixup != null ) {
                fixupList.add(fixup);
            }
        }
        return writeRecords( fixupList, neverAnyDeletedEntityIds, etlDateStr );
    }


    @Override
    Collection<String> dataRecords(String etlDateStr, boolean isDelete, FixupCommentary fixupCommentary) {
        Collection<String> records = new ArrayList<>();
        if (fixupCommentary == null) {
            return records;
        }

        // Revision number of oldest (create) fixup is all we're interested in (otherwise we're handling a fixup of a fixup)
        Long fixupId = fixupCommentary.getFixupCommentaryId();
        AuditReader auditReader = AuditReaderFactory.get(dao.getEntityManager());
        List<Number> revisions = auditReader.getRevisions(FixupCommentary.class, fixupId);
        long currentRevision = revisions.iterator().next().longValue();

        // What entities changed at that revision?
        CrossTypeRevisionChangesReader changesReader = auditReader.getCrossTypeRevisionChangesReader();
        Map<RevisionType,List<Object>> fixupObjects = changesReader.findEntitiesGroupByRevisionType(Long.valueOf(currentRevision));

        // Look through for entities of interest and translate into events to use as a basis for refresh
        Set<LabEvent> coreAuditEvents = new HashSet<>();
        for( Map.Entry<RevisionType,List<Object>> mapEntry : fixupObjects.entrySet() ) {
            RevisionType revType = mapEntry.getKey();
            for( Object entity : mapEntry.getValue() ) {
                if( OrmUtil.proxySafeIsInstance(entity, LabEvent.class) ) {
                    LabEvent auditEvent = OrmUtil.proxySafeCast(entity, LabEvent.class);
                    getCoreEventsFromAudit( auditEvent, revType, currentRevision, auditReader, coreAuditEvents );
                } else if( OrmUtil.proxySafeIsInstance(entity, MercurySample.class) ) {
                    MercurySample auditSample = OrmUtil.proxySafeCast(entity, MercurySample.class);
                    getCoreEventsFromAudit( auditSample, revType, currentRevision, auditReader, coreAuditEvents );
                } else if( OrmUtil.proxySafeIsInstance(entity, LabBatchStartingVessel.class) ) {
                    LabBatchStartingVessel auditBatchVessel = OrmUtil.proxySafeCast(entity, LabBatchStartingVessel.class);
                    getCoreEventsFromAudit( auditBatchVessel, revType, currentRevision, auditReader, coreAuditEvents );
                } else if( OrmUtil.proxySafeIsInstance(entity, ProductOrderSample.class) ) {
                    ProductOrderSample auditPdoSample = OrmUtil.proxySafeCast(entity, ProductOrderSample.class);
                    MercurySample auditSample = auditPdoSample.getMercurySample();
                    getCoreEventsFromAudit( auditSample, revType, currentRevision, auditReader, coreAuditEvents );
                }
            }
        }

        // Using base events, traverse target vessel descendant events and refresh all
        Collection<LabEvent> eventsToRefresh = getEventsRelatedToAuditEvents( coreAuditEvents );
        Set<SequencingRun> sequencingRunsToRefresh = new HashSet<>();
        for( LabEvent evt : eventsToRefresh ) {
            records.addAll( labEventEtl.dataRecords(etlDateStr, isDelete, evt) );

            // Capture sequencing runs in order to refresh sequencing sample fact data
            LabVessel flowcell = null;
            if( evt.getLabEventType() == LabEventType.FLOWCELL_TRANSFER ||
                    evt.getLabEventType() == LabEventType.DILUTION_TO_FLOWCELL_TRANSFER  ||
                    evt.getLabEventType() == LabEventType.DENATURE_TO_FLOWCELL_TRANSFER ) {
                flowcell = evt.getTargetLabVessels().iterator().next();
            } else if( evt.getLabEventType() == LabEventType.FLOWCELL_LOADED ) {
                flowcell = evt.getInPlaceLabVessel();
            }
            // Collects sequencing runs and build sequencing sample fact data from any descendant flowcells.
            if (OrmUtil.proxySafeIsInstance(flowcell, RunCartridge.class)) {
                RunCartridge runCartridge = OrmUtil.proxySafeCast(flowcell, RunCartridge.class );
                sequencingRunsToRefresh.addAll(runCartridge.getSequencingRuns());
            }

        }

        for (SequencingRun seqRun : sequencingRunsToRefresh) {
            for( String etlData : sequencingSampleFactEtl.dataRecords( etlDateStr, false, seqRun ) ) {
                // Suffix record with S to differentiate sequencing_sample_fact
                // (vs. E for event_fact and A for library_ancestry)
                records.add( etlData + ",S");
            }
        }

        return records;
    }

    /**
     * Given an event that was part of a fixup, get any events out of audit to use as a source to
     *   determine which events in production need to be refreshed
     */
    private void getCoreEventsFromAudit( LabEvent auditEvent, RevisionType revType
            , long currentRevision, AuditReader auditReader, Set<LabEvent> coreEvents ) {
        switch (revType) {
            case ADD:
                // Events may be created by a fix-up somewhere in the middle of transfer history
                coreEvents.add(auditEvent);
                break;
            case MOD:
                // Event update needs current and prior event versions
                coreEvents.add(auditEvent);
                LabEvent priorEvent = getPreviousEntityRevision(LabEvent.class, auditReader, auditEvent.getLabEventId(), currentRevision);
                if (OrmUtil.proxySafeIsInstance(priorEvent, LabEvent.class)) {
                    coreEvents.add(OrmUtil.proxySafeCast(priorEvent, LabEvent.class));
                }
                break;
            case DEL:
                // Deletion needs prior version
                LabEvent deletedEvent = getPreviousEntityRevision(LabEvent.class, auditReader, auditEvent.getLabEventId(), currentRevision);
                if (OrmUtil.proxySafeIsInstance(deletedEvent, LabEvent.class)) {
                    coreEvents.add(OrmUtil.proxySafeCast(deletedEvent, LabEvent.class));
                }
                break;
        }
    }

    /**
     * Given a LabBatchStartingVessel that was part of a fixup, get any events out of audit to use as a source to
     *   determine which events in production need to be refreshed
     */
    private void getCoreEventsFromAudit( LabBatchStartingVessel baseAuditBatchVessel, RevisionType revType
            , long currentRevision, AuditReader auditReader, Set<LabEvent> coreEvents ) {

        Set<LabVessel> auditVessels = new HashSet<>();
        LabBatchStartingVessel priorAuditBatchVessel;
        switch (revType) {
            case ADD:
                // Add a vessel to a batch is simplest - get all downstream events for the batch vessels
                // Point in time batch vessel data from audit is acceptable
                for( LabBatchStartingVessel auditBatchVessel : baseAuditBatchVessel.getLabBatch().getLabBatchStartingVessels() ) {
                    auditVessels.add(auditBatchVessel.getLabVessel());
                }
                break;
            case MOD:
                // Point in time batch vessel data from audit
                for( LabBatchStartingVessel auditBatchVessel : baseAuditBatchVessel.getLabBatch().getLabBatchStartingVessels() ) {
                    auditVessels.add(auditBatchVessel.getLabVessel());
                }
                // Add previous revision vessels
                priorAuditBatchVessel = getPreviousEntityRevision(
                        LabBatchStartingVessel.class, auditReader, baseAuditBatchVessel.getBatchStartingVesselId(), currentRevision);
                if (OrmUtil.proxySafeIsInstance(priorAuditBatchVessel, LabBatchStartingVessel.class)) {
                    for( LabBatchStartingVessel auditBatchVessel : priorAuditBatchVessel.getLabBatch().getLabBatchStartingVessels() ) {
                        auditVessels.add(auditBatchVessel.getLabVessel());
                    }
                }
                break;
            case DEL:
                // Add previous revision vessels
                priorAuditBatchVessel = getPreviousEntityRevision(
                        LabBatchStartingVessel.class, auditReader, baseAuditBatchVessel.getBatchStartingVesselId(), currentRevision);
                if (OrmUtil.proxySafeIsInstance(priorAuditBatchVessel, LabBatchStartingVessel.class)) {
                    for( LabBatchStartingVessel auditBatchVessel : priorAuditBatchVessel.getLabBatch().getLabBatchStartingVessels() ) {
                        auditVessels.add(auditBatchVessel.getLabVessel());
                    }
                }
                break;
        }

        for( LabVessel auditVessel : auditVessels ) {
            coreEvents.addAll(auditVessel.getInPlaceLabEvents());
            coreEvents.addAll(auditVessel.getTransfersFrom());
            coreEvents.addAll(auditVessel.getTransfersToWithReArrays());
        }
    }

    /**
     * Given a MercurySample that was part of a fixup, get any events out of audit to use as a source to
     *   determine which events in production need to be refreshed
     */
    private void getCoreEventsFromAudit( MercurySample baseMercurySample, RevisionType revType
            , long currentRevision, AuditReader auditReader, Set<LabEvent> coreEvents ) {

        Set<LabVessel> auditVessels = new HashSet<>();
        MercurySample priorMercurySample;
        switch (revType) {
            case ADD:
                // Add a vessel to a batch is simplest - get all downstream events for the batch vessels
                // Point in time batch vessel data from audit is acceptable
                auditVessels.addAll(baseMercurySample.getLabVessel());
                break;
            case MOD:
                // Point in time batch vessel data from audit
                auditVessels.addAll(baseMercurySample.getLabVessel());
                // Add previous revision vessels
                priorMercurySample = getPreviousEntityRevision(
                        MercurySample.class, auditReader, baseMercurySample.getMercurySampleId(), currentRevision);

                if (OrmUtil.proxySafeIsInstance(priorMercurySample, MercurySample.class)) {
                    auditVessels.addAll(priorMercurySample.getLabVessel());
                }
                break;
            case DEL:
                // Add previous revision vessels
                priorMercurySample = getPreviousEntityRevision(
                        MercurySample.class, auditReader, baseMercurySample.getMercurySampleId(), currentRevision);
                if (OrmUtil.proxySafeIsInstance(priorMercurySample, MercurySample.class)) {
                    auditVessels.addAll(priorMercurySample.getLabVessel());
                }
                break;
        }

        for( LabVessel auditVessel : auditVessels ) {
            coreEvents.addAll(auditVessel.getInPlaceLabEvents());
            coreEvents.addAll(auditVessel.getTransfersFrom());
            coreEvents.addAll(auditVessel.getTransfersToWithReArrays());
        }
    }

    /**
     * Given a list of events pulled from audit trail, return production events which require ETL refresh
     */
    private Collection<LabEvent> getEventsRelatedToAuditEvents(Set<LabEvent> auditEvents ){
        TransferTraverserCriteria.LabEventDescendantCriteria eventCriteria = new TransferTraverserCriteria.LabEventDescendantCriteria();

        for( LabEvent evt : auditEvents ) {

            // Here is where we have to divert from envers.
            // We need to capture events/vessels created AFTER the fixup revision point in time)
            if( evt.getInPlaceLabVessel() != null ) {

                LabVessel oltpVessel = dao.findById( LabVessel.class, evt.getInPlaceLabVessel().getLabVesselId() );

                if( oltpVessel == null ) {
                    continue;
                }

                if( oltpVessel.getContainerRole() != null ) {
                    oltpVessel.getContainerRole().applyCriteriaToAllPositions(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                } else {
                    oltpVessel.evaluateCriteria(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                }

            } else {

                // As of 04/2017 the concept of an embedded class is not handled by envers:
                //        Transfer.getSource/TargetVesselContainer().getEmbedder() returns null
                // Use logic from LabEvent.getSourceLabVessels() and .getTargetLabVessels()
                Set<Long> evtVesselIds = new HashSet<>();

                for (SectionTransfer sectionTransfer : evt.getSectionTransfers()) {
                    LabVessel srcVessel = sectionTransfer.getSourceVessel();
                    evtVesselIds.add(srcVessel.getLabVesselId());

                    LabVessel targetVessel = sectionTransfer.getTargetVessel();
                    evtVesselIds.add(targetVessel.getLabVesselId());
                }
                for (CherryPickTransfer cherryPickTransfer : evt.getCherryPickTransfers()) {
                    LabVessel srcVessel = cherryPickTransfer.getSourceVessel();
                    evtVesselIds.add(srcVessel.getLabVesselId());

                    LabVessel targetVessel = cherryPickTransfer.getTargetVessel();
                    evtVesselIds.add(targetVessel.getLabVesselId());
                }
                for (VesselToSectionTransfer vesselToSectionTransfer : evt.getVesselToSectionTransfers()) {
                    evtVesselIds.add(vesselToSectionTransfer.getSourceVessel().getLabVesselId());

                    LabVessel targetVessel = vesselToSectionTransfer.getTargetVessel();
                    evtVesselIds.add(targetVessel.getLabVesselId());
                }

                for (VesselToVesselTransfer vesselToVesselTransfer : evt.getVesselToVesselTransfers()) {
                    evtVesselIds.add(vesselToVesselTransfer.getSourceVessel().getLabVesselId());
                    evtVesselIds.add(vesselToVesselTransfer.getTargetVessel().getLabVesselId());
                }

                // Run traverser on non-envers entities
                for( LabVessel oltpVessel : dao.findListByList( LabVessel.class, LabVessel_.labVesselId, evtVesselIds ) ) {
                    // Safety first?  Probably never happens.
                    if( oltpVessel == null ) {
                        continue;
                    }

                    if( oltpVessel.getContainerRole() != null ) {
                        oltpVessel.getContainerRole().applyCriteriaToAllPositions(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    } else {
                        oltpVessel.evaluateCriteria(eventCriteria, TransferTraverserCriteria.TraversalDirection.Descendants);
                    }
                }
            }
        }

        return eventCriteria.getAllEvents();
    }

    /**
     * Need to be able to get previous revisions to refresh based upon prior state
     */
    private <T> T getPreviousEntityRevision(Class<T> clazz, AuditReader auditReader, Long auditEventId, long latestRevision ) {
        long priorRevision = 0L;

        // List ordered by older revisions first
        List<Number> revisions = auditReader.getRevisions(clazz, auditEventId);
        T priorEntity = null;

        if( revisions == null || revisions.size() == 0 ) {
            return priorEntity;
        }

        for( Number olderRevision : revisions ) {
            if( olderRevision.longValue() < latestRevision ) {
                priorRevision = olderRevision.longValue();
            } else {
                break;
            }
        }

        if( priorRevision > 0 ) {
            priorEntity = auditReader.find(clazz, auditEventId, Long.valueOf( priorRevision ) );
        }

        return priorEntity;
    }

}

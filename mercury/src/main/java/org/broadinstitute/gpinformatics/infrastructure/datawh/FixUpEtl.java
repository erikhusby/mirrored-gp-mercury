package org.broadinstitute.gpinformatics.infrastructure.datawh;


import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.envers.AuditReaderDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
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
import org.hibernate.envers.exception.AuditException;

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
    public final String arrayProcessFileName = "array_process";


    @Inject
    LabEventEtl labEventEtl;

    @Inject
    SequencingSampleFactEtl sequencingSampleFactEtl;

    @Inject
    ArrayProcessFlowEtl arrayProcessFlowEtl;

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
        DataFile arrayProcessDataFile = new DataFile(dataFilename(etlDateStr, arrayProcessFileName));
        int count = 0;

        try {
            // Writes the records.
            for (FixupCommentary entity : entities) {

                try {
                    for (String record : dataRecords(etlDateStr, false, entity)) {
                        // Split file writes: E = event_fact, A = library_ancestry, S = sequencing_sample_fact, I = array_process
                        if( record.endsWith("E") ) {
                            eventDataFile.write(record);
                            count++;
                        } else if ( record.endsWith("A") ) {
                            ancestryDataFile.write(record);
                            count++;
                        } else if ( record.endsWith("S") ) {
                            seqSampleDataFile.write(record);
                            count++;
                        } else if ( record.endsWith("I") ) {
                            arrayProcessDataFile.write(record);
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
            arrayProcessDataFile.close();
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

        boolean coreEntitiesAreDetached = false;

        // Revision number of oldest (create) fixup is all we're interested in (otherwise we're handling a fixup of a fixup)
        Long fixupId = fixupCommentary.getFixupCommentaryId();
        AuditReader auditReader = AuditReaderFactory.get(dao.getEntityManager());
        List<Number> revisions = auditReader.getRevisions(FixupCommentary.class, fixupId);
        long currentRevision = revisions.iterator().next().longValue();

        /*
         * Can't control envers, a simple lab event delete (See LabEventFixupTest.fixupGplim4104()) makes entries in lab_vessel_aud with dtype='LabVessel'
         * Catch and ignore or entire ETL batch cycle fails repeatedly
         *
         * [hibernate-envers-4.0.1.Final.jar:4.0.1.Final] Blows up on NPE at
         * FixUpEtl for method public int org.broadinstitute.gpinformatics.infrastructure.datawh.GenericEntityEtl.doIncrementalEtl(java.util.Set,java.lang.String) throws java.lang.Exception: java.lang.RuntimeException: org.hibernate.envers.exception.AuditException: java.lang.NullPointerException
         *   Caused by: org.hibernate.envers.exception.AuditException: java.lang.NullPointerException
         *       at org.hibernate.envers.entities.EntityInstantiator.createInstanceFromVersionsEntity(EntityInstantiator.java:92)
         *       at org.hibernate.envers.entities.EntityInstantiator.addInstancesFromVersionsEntities(EntityInstantiator.java:112)
         *       at org.hibernate.envers.query.impl.EntitiesModifiedAtRevisionQuery.list(EntitiesModifiedAtRevisionQuery.java:58)
         *       at org.hibernate.envers.query.impl.AbstractAuditQuery.getResultList(AbstractAuditQuery.java:102)
         *       at org.hibernate.envers.reader.CrossTypeRevisionChangesReaderImpl.findEntitiesGroupByRevisionType(CrossTypeRevisionChangesReaderImpl.java:67)
         *       at org.broadinstitute.gpinformatics.infrastructure.datawh.FixUpEtl.dataRecords(FixUpEtl.java:188) [classes:]
         *   Caused by: java.lang.NullPointerException
	     *       at org.hibernate.envers.entities.EntityInstantiator.createInstanceFromVersionsEntity(EntityInstantiator.java:90)
	     *
         *   org.hibernate.internal.util.ReflectHelper
         *   public static Constructor getDefaultConstructor(Class clazz) throws PropertyNotFoundException {
         *           if ( isAbstractClass( clazz ) ) {  <-- LabVessel is an Interface!!!
         *               return null;  <-- Causes NPE!
         *           }
         *           .....
         */
        try {

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
                    } else if ( OrmUtil.proxySafeIsInstance(entity, BucketEntry.class) ) {
                        BucketEntry auditBucketEntry = OrmUtil.proxySafeCast(entity, BucketEntry.class);
                        getCoreEventsFromAudit( auditBucketEntry, revType, currentRevision, auditReader, coreAuditEvents );
                    }
                }
            }

            // Split the above into batches or this process will hoover up all the server memory on large fixups
            //   , especially on events early in workflows
            int count = 0;
            for( Map.Entry<RevisionType,List<Object>> mapEntry : fixupObjects.entrySet() ) {
                count += mapEntry.getValue().size();
            }
            if( count > ( JPA_CLEAR_THRESHOLD ) ) {
                coreEntitiesAreDetached = true;
                dao.clear();
            }

            // Using base events, traverse target vessel descendant events and refresh all
            Collection<LabEvent> eventsToRefresh = getEventsRelatedToAuditEvents( coreAuditEvents, coreEntitiesAreDetached );
            Set<SequencingRun> sequencingRunsToRefresh = new HashSet<>();
            for( LabEvent evt : eventsToRefresh ) {
                records.addAll( labEventEtl.dataRecords(etlDateStr, isDelete, evt) );
                for( String arrayRecord : arrayProcessFlowEtl.dataRecords(etlDateStr, isDelete, evt) ) {
                    records.add(arrayRecord + ",I");
                }

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
                for (String etlData : sequencingSampleFactEtl.dataRecords(etlDateStr, false, seqRun)) {
                    // Suffix record with S to differentiate sequencing_sample_fact
                    // (vs. E for event_fact and A for library_ancestry)
                    records.add(etlData + ",S");
                }
            }
        } catch ( AuditException ae ) {
            errorException = new Exception( "Envers audit API failure in FixUpEtl - ignoring FixupCommentary ID " + currentRevision, ae );
            errorIds.add(currentRevision);
        }

        return records;
    }

    /**
     * Given an event that was part of a fixup, get any events out of audit to use as a source to
     *   determine which events in production need to be refreshed
     */
    private void getCoreEventsFromAudit( LabEvent auditEvent, RevisionType revType
            , long currentRevision, AuditReader auditReader, Set<LabEvent> coreEvents ) {
        // Add point in time batch vessel data
        if( revType == RevisionType.ADD || revType == RevisionType.MOD ) {
                // Events may be created by a fix-up somewhere in the middle of transfer history
                coreEvents.add(auditEvent);
        }
        // Modify or Deletion needs prior version
        if( revType == RevisionType.MOD || revType == RevisionType.DEL ) {
            LabEvent priorEvent = getPreviousEntityRevision(LabEvent.class, auditReader, auditEvent.getLabEventId(), currentRevision);
            if (OrmUtil.proxySafeIsInstance(priorEvent, LabEvent.class)) {
                coreEvents.add(OrmUtil.proxySafeCast(priorEvent, LabEvent.class));
            }
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
        // Add point in time batch vessel data
        if( revType == RevisionType.ADD || revType == RevisionType.MOD ) {
            for (LabBatchStartingVessel auditBatchVessel : baseAuditBatchVessel.getLabBatch()
                    .getLabBatchStartingVessels()) {
                auditVessels.add(auditBatchVessel.getLabVessel());
            }
        }

        if( revType == RevisionType.MOD || revType == RevisionType.DEL ) {
            // Add previous revision vessels
            priorAuditBatchVessel = getPreviousEntityRevision(
                    LabBatchStartingVessel.class, auditReader, baseAuditBatchVessel.getBatchStartingVesselId(), currentRevision);
            if (OrmUtil.proxySafeIsInstance(priorAuditBatchVessel, LabBatchStartingVessel.class)) {
                for( LabBatchStartingVessel auditBatchVessel : priorAuditBatchVessel.getLabBatch().getLabBatchStartingVessels() ) {
                    auditVessels.add(auditBatchVessel.getLabVessel());
                }
            }
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
        // Add point in time batch vessel data
        if( revType == RevisionType.ADD || revType == RevisionType.MOD ) {
            auditVessels.addAll(baseMercurySample.getLabVessel());
        }
        if( revType == RevisionType.MOD || revType == RevisionType.DEL ) {
            // Add previous revision vessels
            priorMercurySample = getPreviousEntityRevision(
                        MercurySample.class, auditReader, baseMercurySample.getMercurySampleId(), currentRevision);

            if (OrmUtil.proxySafeIsInstance(priorMercurySample, MercurySample.class)) {
                auditVessels.addAll(priorMercurySample.getLabVessel());
            }
        }

        for( LabVessel auditVessel : auditVessels ) {
            coreEvents.addAll(auditVessel.getInPlaceLabEvents());
            coreEvents.addAll(auditVessel.getTransfersFrom());
            coreEvents.addAll(auditVessel.getTransfersToWithReArrays());
        }
    }

    /**
     * Given a BucketEntry that was part of a fixup, get any events out of audit to use as a source to
     *   determine which events in production need to be refreshed
     */
    private void getCoreEventsFromAudit( BucketEntry baseBucketEntry, RevisionType revType
            , long currentRevision, AuditReader auditReader, Set<LabEvent> coreEvents ) {

        Set<LabVessel> auditVessels = new HashSet<>();
        BucketEntry priorBucketEntry;

        // Add point in time batch vessel data
        if( revType == RevisionType.ADD || revType == RevisionType.MOD ) {
            auditVessels.add(baseBucketEntry.getLabVessel());
        }

        if( revType == RevisionType.MOD || revType == RevisionType.DEL ) {
           // Add previous revision vessels
            priorBucketEntry = getPreviousEntityRevision(
                    BucketEntry.class, auditReader, baseBucketEntry.getBucketEntryId(), currentRevision);

            if (OrmUtil.proxySafeIsInstance(priorBucketEntry, BucketEntry.class)) {
                auditVessels.add(priorBucketEntry.getLabVessel());
            }
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
    private Collection<LabEvent> getEventsRelatedToAuditEvents(Set<LabEvent> auditEvents, boolean coreEntitiesAreDetached ){
        TransferTraverserCriteria.LabEventDescendantCriteria eventCriteria = new TransferTraverserCriteria.LabEventDescendantCriteria();
        int count = 0;

        for( LabEvent evt : auditEvents ) {
            count++;
            if( coreEntitiesAreDetached && count % JPA_CLEAR_THRESHOLD == 0 ) {
                dao.clear();
            }

            if( coreEntitiesAreDetached ) {
                evt = dao.findById( LabEvent.class, evt.getLabEventId() );
            }

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
    private <T> T getPreviousEntityRevision(Class < T > clazz, AuditReader auditReader, Long auditEventId, long latestRevision ) {
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

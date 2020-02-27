package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Handles freezer storage functionality
 */
@Stateful
@RequestScoped
public class StorageEjb {

    @Inject
    private StorageLocationDao storageLocationDao;

    /**
     * Create a single in-place storage event for a vessel and optionally an associated rack of tubes <br/>
     * An in-place tube formation is the only vessel which will be persisted if required <br/>
     * Flagged DAOFree because persistence is bypassed when running in DBFree test
     */
    @DaoFree
    public LabEvent createStorageEvent(LabEventType labEventType, LabVessel inPlaceVessel, StorageLocation storageLocation, LabVessel ancillaryInPlaceVessel, Long userId ){
        return createDisambiguatedStorageEvent( labEventType, inPlaceVessel, storageLocation, ancillaryInPlaceVessel, userId, new Date(), 1L );
    }

    /**
     * Create an in-place storage event as part of a group of events for a vessel and optionally an associated rack of tubes <br/>
     * <strong> Assumes any vessels are already persisted if/as required </strong> <br/>
     * Flagged DAOFree because persistence is bypassed when running in DBFree test
     */
    @DaoFree
    public LabEvent createDisambiguatedStorageEvent(LabEventType labEventType, LabVessel inPlaceVessel, StorageLocation storageLocation, LabVessel ancillaryInPlaceVessel, Long userId, Date eventDate, long disambuguator ){
        LabEvent storageEvent = new LabEvent(labEventType, eventDate, LabEvent.UI_PROGRAM_NAME, disambuguator, userId, LabEvent.UI_PROGRAM_NAME );
        storageEvent.setInPlaceLabVessel(inPlaceVessel);
        storageEvent.setStorageLocation(storageLocation);
        storageEvent.setAncillaryInPlaceVessel(ancillaryInPlaceVessel);
        if( storageLocationDao != null ) {
            storageLocationDao.persist(storageEvent);
        }
        return storageEvent;
    }

    /**
     * Find latest check-in event for a lab vessel (tube, rack of tubes, static plate) <br/>
     * If a check-out occurs after a check-in, will not report the check-in event
     */
    public LabEvent findLatestCheckInEvent(LabVessel labVessel) {
        LabEvent latestStorageEvent = getLatestStorageEvent(labVessel);
        if (latestStorageEvent != null && latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN) {
            return latestStorageEvent;
        } else {
            return null;
        }
    }

    /**
     * Given a lab vessel, find the latest storage event <br/>
     * (For this vessel and any containers only - no transfer traversal performed)
     */
    public LabEvent getLatestStorageEvent(LabVessel vessel) {
        LabEvent latestStorageEvent = null;
        SortedSet<LabEvent> sortedTreeSet = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        if (OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
            RackOfTubes rack = OrmUtil.proxySafeCast(vessel, RackOfTubes.class);
            for (LabEvent event : rack.getAncillaryInPlaceEvents()) {
                if (isStorageEvent(event)) {
                    sortedTreeSet.add(event);
                }
            }
        } else {
            // All storage events are in place
            Set<LabEvent> eventsList = vessel.getInPlaceEventsWithContainers();
            for (LabEvent event : eventsList) {
                if (isStorageEvent(event)) {
                    sortedTreeSet.add(event);
                }
            }
        }

        if (sortedTreeSet.size() > 0) {
            latestStorageEvent = sortedTreeSet.last();
        }
        return latestStorageEvent;
    }

    /**
     * Centralize logic for what constitutes a storage event
     */
    public boolean isStorageEvent(LabEvent event) {
        switch (event.getLabEventType()) {
            case STORAGE_CHECK_IN:
            case STORAGE_CHECK_OUT:
            case IN_PLACE:
                return true;
            default:
                return false;
        }
    }

    /**
     * Find rack and tube formation for a tube based upon latest lab event
     */
    public Triple<LabVessel, TubeFormation, LabEvent> findLatestRackAndLayout(LabVessel tube) {
        SortedSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        // Keep track of which event owns which tube formation
        Map<LabEvent, TubeFormation> eventTubes = new HashMap<>();

        // Gather all tube formation events
        for (LabVessel tubeContainer : tube.getContainers()) {
            if (OrmUtil.proxySafeIsInstance(tubeContainer, TubeFormation.class)) {
                TubeFormation formation = OrmUtil.proxySafeCast(tubeContainer, TubeFormation.class);
                for (LabEvent evt : formation.getInPlaceLabEvents()) {
                    sortedEvents.add(evt);
                    eventTubes.put(evt, formation);
                }
                for (LabEvent evt : formation.getTransfersTo()) {
                    sortedEvents.add(evt);
                    eventTubes.put(evt, formation);
                }
                for (LabEvent evt : formation.getTransfersFrom()) {
                    sortedEvents.add(evt);
                    eventTubes.put(evt, formation);
                }
            }
        }

        if (sortedEvents.size() > 0) {
            // Latest event and formation
            LabEvent latestRackEvent = sortedEvents.last();
            TubeFormation latestFormation = eventTubes.get(latestRackEvent);

            // Easy pick for in-place, rack is directly on the event
            if (latestRackEvent.getAncillaryInPlaceVessel() != null) {
                return Triple.of(latestRackEvent.getAncillaryInPlaceVessel(), OrmUtil.proxySafeCast(latestRackEvent.getInPlaceLabVessel(), TubeFormation.class), sortedEvents.last());
            } else {
                // More involved logic for transfers - return the first hit
                LabVessel xferFormation;
                for (CherryPickTransfer xfer : latestRackEvent.getCherryPickTransfers()) {
                    xferFormation = xfer.getTargetVessel();
                    if (xferFormation != null && xferFormation.getLabel().equals(latestFormation.getLabel())) {
                        return Triple.of(xfer.getAncillaryTargetVessel(), OrmUtil.proxySafeCast(xferFormation, TubeFormation.class), latestRackEvent);
                    }
                    xferFormation = xfer.getSourceVessel();
                    if (xferFormation != null && xferFormation.getLabel().equals(latestFormation.getLabel())) {
                        return Triple.of(xfer.getAncillarySourceVessel(), OrmUtil.proxySafeCast(xferFormation, TubeFormation.class), latestRackEvent);
                    }
                }
                for (SectionTransfer xfer : latestRackEvent.getSectionTransfers()) {
                    xferFormation = xfer.getTargetVessel();
                    if (xferFormation != null && xferFormation.getLabel().equals(latestFormation.getLabel())) {
                        return Triple.of(xfer.getAncillaryTargetVessel(), OrmUtil.proxySafeCast(xferFormation, TubeFormation.class), latestRackEvent);
                    }
                    xferFormation = xfer.getSourceVessel();
                    if (xferFormation != null && xferFormation.getLabel().equals(latestFormation.getLabel())) {
                        return Triple.of(xfer.getAncillarySourceVessel(), OrmUtil.proxySafeCast(xferFormation, TubeFormation.class), latestRackEvent);
                    }
                }
                for (VesselToSectionTransfer xfer : latestRackEvent.getVesselToSectionTransfers()) {
                    xferFormation = xfer.getTargetVessel();
                    if (xferFormation != null && xferFormation.getLabel().equals(latestFormation.getLabel())) {
                        return Triple.of(xfer.getAncillaryTargetVessel(), OrmUtil.proxySafeCast(xferFormation, TubeFormation.class), latestRackEvent);
                    }
                }
            }
        }

        // Can't find anything
        return null;
    }

    /**
     * Finds the latest in-place or transfer event and associated TubeFormation for a rack
     */
    private Pair<LabEvent, LabVessel> getLatestRackEvent(RackOfTubes rack) {
        TreeSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        // In-place with rack as ancillary vessel
        sortedEvents.addAll(rack.getAncillaryInPlaceEvents());

        // Get all associated events for the rack's tube formations
        // Note: May not map back to same rack!
        for (TubeFormation tubes : rack.getTubeFormations()) {
            sortedEvents.addAll(tubes.getTransfersTo());
            sortedEvents.addAll(tubes.getTransfersFrom());
        }

        if (sortedEvents.isEmpty()) {
            return null;
        }

        for (Iterator<LabEvent> iter = sortedEvents.descendingIterator(); iter.hasNext(); ) {
            LabEvent latest = iter.next();
            LabVessel xferRack;
            // In-place with rack as ancillary is easiest
            if (latest.getAncillaryInPlaceVessel() != null) {
                return Pair.of(latest, latest.getInPlaceLabVessel());
            }
            for (CherryPickTransfer xfer : latest.getCherryPickTransfers()) {
                xferRack = xfer.getAncillaryTargetVessel();
                if (xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
                xferRack = xfer.getAncillarySourceVessel();
                if (xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getSourceVessel());
                }
            }
            for (SectionTransfer xfer : latest.getSectionTransfers()) {
                xferRack = xfer.getAncillaryTargetVessel();
                if (xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
                xferRack = xfer.getAncillarySourceVessel();
                if (xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getSourceVessel());
                }
            }
            // Only valid for target
            for (VesselToSectionTransfer xfer : latest.getVesselToSectionTransfers()) {
                xferRack = xfer.getAncillaryTargetVessel();
                if (xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
            }
        }

        // Can't find anything
        return null;
    }

    /**
     * A tube formation or rack for a storage event may not exist: <br/>
     * <ul><li>Do not persist if running in DAOFree test (CDI injection unavailable)</li>
     * <li>A RackOfTubes or TubeFormation will be persisted as required</li>
     * <li>A BarcodedTube or StaticPlate is ignored, must always exist in Mercury before attempted storage</li></ul>
     */
    public LabVessel tryPersistRackOrTubes(LabVessel candidate) {
        if (storageLocationDao == null) {
            // Assuming a DAOFree test, do nothing
            return candidate;
        } else if ( OrmUtil.proxySafeIsInstance( candidate, TubeFormation.class )
                 || OrmUtil.proxySafeIsInstance( candidate, RackOfTubes.class ) ) {
            LabVessel persistedVessel = storageLocationDao.findSingleSafely( LabVessel.class, LabVessel_.label, candidate.getLabel(), LockModeType.NONE );
            if( persistedVessel == null ) {
                storageLocationDao.persist(candidate);
                return candidate;
            } else {
                return persistedVessel;
            }
        } else {
            // StaticPlate, BarcodedTube should already exist
            return candidate;
        }
    }

}

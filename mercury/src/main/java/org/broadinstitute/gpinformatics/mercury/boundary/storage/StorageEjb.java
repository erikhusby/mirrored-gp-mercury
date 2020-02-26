package org.broadinstitute.gpinformatics.mercury.boundary.storage;

import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.LockModeType;
import java.util.Date;

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
     * Find latest check-in event for a lab vessel (rack of tubes, static plate) <br/>
     * If a check-out occurs after a check-in, do not report the check-in event
     */
    public LabEvent findLatestCheckInEvent(LabVessel labVessel ) {
        LabEvent latestStorageEvent = labVessel.getLatestStorageEvent();
        if( latestStorageEvent != null && latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN ) {
            return latestStorageEvent;
        } else {
            return null;
        }
    }

    /**
     * A tube formation or rack for a storage event may not exist: <br/>
     * <ul><li>Do not persist if running in DAOFree test (CDI injection unavailable)</li>
     * <li>A RackOfTubes or TubeFormation will be persisted as required</li>
     * <li>A BarcodedTube or StaticPlate is ignored, must always exist in Mercury before attempted storage</li></ul>
     */
    public LabVessel tryPersistRackOrTubes( LabVessel candidate ) {
        if( storageLocationDao == null ) {
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

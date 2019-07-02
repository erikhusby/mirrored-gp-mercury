package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.util.*;
import java.util.logging.Logger;

/**
 * This Action Bean handles bulk SRS storage check-out and check-in operations.
 */
@UrlBinding(value = BulkStorageOpsActionBean.ACTION_BEAN_URL)
public class BulkStorageOpsActionBean extends CoreActionBean {

    private static final Logger logger = Logger.getLogger(BulkStorageOpsActionBean.class.getName());
    public static final String ACTION_BEAN_URL = "/storage/bulkStorageOps.action";

    private static final long MILLIS_PER_DAY = ( 1000 * 60 * 60 * 24 );
    // Wizard functionality for check-in flow
    private static final String CHECK_IN_PHASE_INIT = "INIT";
    private static final String CHECK_IN_PHASE_READY = "READY";
    // Limit the number of locations gathered at a single page hit
    private static int MAX_BULK_CHECKIN_LOCS = 24;

    // Events
    private static final String EVT_INIT_CHECK_OUT = "initCheckOut";
    private static final String EVT_CHECK_OUT = "checkOut";
    private static final String EVT_INIT_CHECK_IN = "initCheckIn";
    private static final String EVT_VALIDATED_CHECK_IN = "validateCheckIn";
    private static final String EVT_CHECK_IN = "checkIn";

    // UI Resolutions
    private static final String UI_CHECK_IN = "/storage/bulk_checkin.jsp";
    private static final String UI_CHECK_OUT = "/storage/bulk_checkout.jsp";

    // Web params
    String barcode;
    Long storageLocationId;
    List<String> proposedLocationIds;

    // Instance/display vars
    String checkInPhase = CHECK_IN_PHASE_INIT;
    List<StorageLocation> validLocations;
    Map<Long,String> storageLocPaths;

    @Inject
    private StorageLocationDao storageLocationDao;

    /**
     * Check-Out page (or non-specified event)
     */
    @DefaultHandler
    @HandlesEvent(EVT_INIT_CHECK_OUT)
    public Resolution eventInitCheckOut(){
        return new ForwardResolution(UI_CHECK_OUT);
    }

    /**
     * Step one of check in:  Present user with ability to choose locations
     */
    @HandlesEvent(EVT_INIT_CHECK_IN)
    public Resolution eventInitCheckIn(){
        checkInPhase = CHECK_IN_PHASE_INIT;
        return new ForwardResolution(UI_CHECK_IN);
    }

    /**
     * Step two of check in:  Present user with list of valid locations
     */
    @HandlesEvent(EVT_VALIDATED_CHECK_IN)
    public Resolution eventValidatedCheckIn(){
        if( proposedLocationIds == null || proposedLocationIds.isEmpty() ) {
            addGlobalValidationError("No storage locations selected");
            checkInPhase = CHECK_IN_PHASE_INIT;
            return new ForwardResolution(UI_CHECK_IN);
        }

        List<StorageLocation> unsortedLocations = new ArrayList();

        validLocations = new ArrayList<>();
        storageLocPaths = new HashMap<>();

        for( String idStr : proposedLocationIds ) {
            StorageLocation storageLocation = storageLocationDao.findById(StorageLocation.class, Long.valueOf(idStr) );
            if( storageLocation == null ) {
                addGlobalValidationError("No storage location found for ID = " + idStr );
                continue;
            }
            switch( storageLocation.getLocationType() ){
                // Should be filtered out on client
                case GAUGERACK:
                    gatherAvailableLocs(storageLocation, unsortedLocations, true);
                    break;
                case LOOSE:
                    gatherAvailableLocs(storageLocation, unsortedLocations, false);
                    break;
                case FREEZER:
                case REFRIGERATOR:
                case SHELVINGUNIT:
                case CABINET:
                    addGlobalValidationError("Nothing is allowed to be stored directly in a "
                            + storageLocation.getLocationType().getDisplayName() + " (" + storageLocation.getLabel() + ")" );
                    break;
                case SECTION:
                case SHELF:
                case DRAWER:
                case RACK:
                case BOX:
                case SLOT:
                    addGlobalValidationError("Bulk storage functionality not allowed for a "
                            + storageLocation.getLocationType().getDisplayName() + " (" + storageLocation.getLabel() + ")" );
                    break;
                default:
                    addGlobalValidationError("Unknown storage location type:  "
                            + storageLocation.getLocationType().getDisplayName() );
                    break;
            }

            if( unsortedLocations.size() >= MAX_BULK_CHECKIN_LOCS ) {
                addMessage( "Limited this bulk check-in location batch to " + MAX_BULK_CHECKIN_LOCS + " locations/vessels.");
                break;
            }
        }

        if( unsortedLocations.isEmpty() ) {
            addGlobalValidationError("No valid storage locations available in selection");
            checkInPhase = CHECK_IN_PHASE_INIT;
            return new ForwardResolution(UI_CHECK_IN);
        }

        // Sort by path name, admittedly a bit ugly
        Map<String,Long> sortedMap = new TreeMap<>();
        for( Map.Entry<Long,String> entry : storageLocPaths.entrySet() ) {
            sortedMap.put(entry.getValue(), entry.getKey());
        }
        for( Long id : sortedMap.values() ){
            for( StorageLocation loc : unsortedLocations ) {
                if( loc.getStorageLocationId().equals(id)) {
                    validLocations.add(loc);
                }
            }
        }

        checkInPhase = CHECK_IN_PHASE_READY;
        return new ForwardResolution(UI_CHECK_IN);
    }

    /**
     * Fills out list of available locations for use in bulk check in <br/>
     * Stops list at MAX_BULK_CHECKIN_LOCS because many legacy locations have a capacity of hundreds, thousands, or more <br/>
     * Since only selectable loc types are GAUGERACK and LOOSE, logic doesn't have to handle when a parent and some children are selected
     */
    private void gatherAvailableLocs( StorageLocation storageLocation, List<StorageLocation> unsortedLocations, boolean useChildren ) {
        List<StorageLocation> locations = new ArrayList<>();
        if(useChildren) {
            locations.addAll(storageLocation.getChildrenStorageLocation());
        } else {
            locations.add(storageLocation);
        }

        for( StorageLocation location : locations ){
            int capacity = location.getStorageCapacity();
            int storedCount = storageLocationDao.getStoredContainerCount( location );

            String locationPath = storageLocPaths.get(location.getStorageLocationId());
            if( locationPath == null ) {
                locationPath = location.getLocationType().getDisplayName() + ":  " + storageLocationDao.getLocationTrail( location.getStorageLocationId() );
                storageLocPaths.put(location.getStorageLocationId(), locationPath );
            }

            if( capacity == 0 ) {
                addGlobalValidationError(locationPath + " has no configured capacity." );
                continue;
            }

            for( int i = 0; i < location.getStorageCapacity(); i++ ) {
                if( storedCount >= capacity ) {
                    addGlobalValidationError(locationPath + " capacity (" + capacity + ") is all either currently in use or fully allocated to this batch." );
                    break;
                } else {
                    storedCount++;
                    unsortedLocations.add(location);
                    if (unsortedLocations.size() >= MAX_BULK_CHECKIN_LOCS) {
                        break;
                    }
                }
            }
        }
    }


    /**
     * Add or remove SRS batch vessels
     */
    @HandlesEvent(EVT_CHECK_IN)
    public Resolution eventCheckIn(){

        if( storageLocationId == null ) {
            return buildAjaxOutcome( Pair.of("danger",  "Barcode : " + barcode + " - No storage location specified.") );
        }

        LabVessel vessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, barcode );
        if( vessel == null ) {
            return buildAjaxOutcome( Pair.of("danger", "Vessel barcode " + barcode + " not found.") );
        } else if( vessel.getStorageLocation() != null ) {
            return buildAjaxOutcome( Pair.of("warning", "Vessel barcode " + barcode + " already in storage.") );
        }

        StorageLocation storageLocation = storageLocationDao.findById(StorageLocation.class,storageLocationId );
        if( storageLocation == null ) {
            return buildAjaxOutcome( Pair.of("danger", "Barcode : " + barcode + " - No storage location exists for ID: " + storageLocationId ) );
        }
        String locationTrail = storageLocationDao.getLocationTrail(storageLocationId);

        Pair<String, String> statusMessage = null;
        if( OrmUtil.proxySafeIsInstance( vessel, BarcodedTube.class ) ) {
            BarcodedTube tube = OrmUtil.proxySafeCast( vessel, BarcodedTube.class );
            if( storageLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                tube.setStorageLocation(storageLocation);
                LabEvent checkInEvent = createStorageEvent( LabEventType.STORAGE_CHECK_IN, tube, storageLocation,null );
                storageLocationDao.persist(checkInEvent);
                statusMessage = Pair.of("success", "Vessel barcode " + barcode
                        + " checked into 'loose' location " + locationTrail + ".");
            } else {
                statusMessage = Pair.of("warning", "Storage location " + locationTrail + " is not configured to store loose vessels - ignoring.");
            }
        } else if( OrmUtil.proxySafeIsInstance( vessel, StaticPlate.class ) ) {
            StaticPlate plate = OrmUtil.proxySafeCast( vessel, StaticPlate.class );
            plate.setStorageLocation(storageLocation);
            LabEvent checkInEvent = createStorageEvent( LabEventType.STORAGE_CHECK_IN, plate, storageLocation,null );
            storageLocationDao.persist(checkInEvent);
            statusMessage = Pair.of("success", "Plate " + barcode
                    + " checked into location " + locationTrail + ".");
        } else if( OrmUtil.proxySafeIsInstance( vessel, RackOfTubes.class ) ) {
            RackOfTubes rack = OrmUtil.proxySafeCast( vessel, RackOfTubes.class );
            statusMessage = doCheckIn( rack, storageLocation );
        } else if( statusMessage == null ) {
            // How did a flowcell get into storage?  Probably not a real case
            statusMessage = Pair.of("danger", "Vessel barcode " + barcode + " type currently not storable.");
        }

        return buildAjaxOutcome(statusMessage);
    }

    /**
     * Check vessel out of storage
     */
    @HandlesEvent(EVT_CHECK_OUT)
    public Resolution eventCheckOut(){

        LabVessel vessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, barcode );

        // Validate the obvious
        if( vessel == null ) {
            return buildAjaxOutcome( Pair.of("danger", "Vessel barcode " + barcode + " not found.") );
        } else if( vessel.getStorageLocation() == null ) {
            return buildAjaxOutcome( Pair.of("warning", "Vessel barcode " + barcode + " not in storage.") );
        }

        Pair<String, String> statusMessage = null;
        if( OrmUtil.proxySafeIsInstance( vessel, BarcodedTube.class ) ) {
            StorageLocation storageLocation = vessel.getStorageLocation();
            BarcodedTube tube = OrmUtil.proxySafeCast( vessel, BarcodedTube.class );
            if( storageLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                statusMessage = doCheckOut(tube);
            } else {
                statusMessage = Pair.of("warning", "Vessel barcode " + barcode + " is a tube not stored in a 'loose' location - ignoring.");
            }
        } else if( OrmUtil.proxySafeIsInstance( vessel, StaticPlate.class ) ) {
            StaticPlate plate = OrmUtil.proxySafeCast( vessel, StaticPlate.class );
            statusMessage = doCheckOut(plate);
        } else if( OrmUtil.proxySafeIsInstance( vessel, RackOfTubes.class ) ) {
            RackOfTubes rack = OrmUtil.proxySafeCast( vessel, RackOfTubes.class );
            statusMessage = doCheckOut(rack);
        } else if( statusMessage == null ) {
            // How did a flowcell get into storage?  Probably not a real case
            statusMessage = Pair.of("danger", "Vessel barcode " + barcode + " type currently not storable.");
        }

        return buildAjaxOutcome(statusMessage);
    }

    /**
     * Produce JSON feedback output to be handled in the client <br/>
     * { barcode: "", feedbackMsg: "", status: "" } <br/>
     * Status value matches bootstrap UI types (primary,secondary,success,danger,warning,info,light,dark)
     */
    private StreamingResolution buildAjaxOutcome( Pair<String,String> statusMessage ) {
        JsonObject result = Json.createObjectBuilder()
                .add( "barcode", barcode )
                .add( "status", statusMessage.getLeft() )
                .add( "feedbackMsg", statusMessage.getRight() )
                .build();
        return new StreamingResolution("text/json", result.toString() );
    }

    /**
     * Checks a given BarcodedTube out of loose storage
     * @return Pair with status code on left and storage location on right <br/>
     *   Status code matches bootstrap UI types (primary,secondary,success,danger,warning,info,light,dark) <br/>
     *   See https://getbootstrap.com/docs/4.0/components/alerts/
     */
    private Pair<String, String> doCheckOut( BarcodedTube tube ) {
        StorageLocation storageLocation = tube.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation.getStorageLocationId() );
        tube.setStorageLocation(null);
        LabEvent checkOutEvent = createStorageEvent( LabEventType.STORAGE_CHECK_OUT, tube, storageLocation,null );
        storageLocationDao.persist(checkOutEvent);
        return Pair.of("success", "Checked out vessel barcode " + barcode
                + " from 'loose' location " + locationTrail + ".");
    }

    private Pair<String, String> doCheckOut( StaticPlate plate ) {
        StorageLocation storageLocation = plate.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation.getStorageLocationId() );
        plate.setStorageLocation(null);
        LabEvent checkOutEvent = createStorageEvent( LabEventType.STORAGE_CHECK_OUT, plate, storageLocation, null );
        storageLocationDao.persist(checkOutEvent);
        return Pair.of("success", "Checked out plate barcode " + barcode + " from " + locationTrail + ".");
    }

    private Pair<String, String> doCheckOut( RackOfTubes rack ) {
        StorageLocation rackLocation = rack.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( rackLocation.getStorageLocationId() );

        Set<LabEvent> inPlaceEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);
        inPlaceEvents.addAll( rack.getInPlaceLabEvents() );

        LabEvent latestCheckInEvent = null;
        for( LabEvent inPlaceEvent : inPlaceEvents ) {
            if( inPlaceEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN ) {
                latestCheckInEvent = inPlaceEvent;
                break;
            }
        }

        if( latestCheckInEvent == null ) {
            // What a mess, still in storage but no check-in event?
            return Pair.of("danger", "Rack barcode " + barcode
                    + " is in storage at " + locationTrail + " without any check-in event.  Tube layout unavailable.");
        } else {
            TubeFormation checkInTubes = OrmUtil.proxySafeCast( latestCheckInEvent.getInPlaceLabVessel(), TubeFormation.class );
            Map<VesselPosition,BarcodedTube> checkOutLayout = new HashMap<>();
            rack.setStorageLocation(null);
            for( Map.Entry<VesselPosition,BarcodedTube> posAndTube : checkInTubes.getContainerRole().getMapPositionToVessel().entrySet() ) {
                BarcodedTube checkedInTube = posAndTube.getValue();
                if( checkedInTube.getStorageLocation() != null
                        && rackLocation.getStorageLocationId().equals(checkedInTube.getStorageLocation().getStorageLocationId())){
                    checkOutLayout.put(posAndTube.getKey(), checkedInTube );
                    checkedInTube.setStorageLocation(null);
                }
            }
            TubeFormation checkOutTubes = new TubeFormation(checkOutLayout, checkInTubes.getRackType());
            if( checkOutTubes.getLabel().equals( checkInTubes.getLabel() ) ){
                // Layout didn't change for checkout - use what was checked in
                checkOutTubes = checkInTubes;
            } else {
                // Layout changed between checkin and checkout - use checkout layout
                LabVessel existing = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, checkOutTubes.getLabel() );
                if( existing == null ) {
                    // Persist new tube formation
                    storageLocationDao.persist(checkOutTubes);
                } else {
                    // Use existing to avoid unique clash on labels if persisted
                    checkOutTubes = OrmUtil.proxySafeCast( existing, TubeFormation.class );
                }
            }

            LabEvent checkOutEvent = createStorageEvent( LabEventType.STORAGE_CHECK_OUT, checkOutTubes, rackLocation, rack );
            storageLocationDao.persist(checkOutEvent);
        }


        return Pair.of("success", "Checked out rack barcode " + barcode
                + " and all tubes from " + locationTrail + ".");
    }

    /**
     * Look for recent events to determine if TubeFormation is trust-able for layout and reject or do check in
     * @return Pair of values, if left not 'success', then nothing was done
     */
    private Pair<String, String> doCheckIn( RackOfTubes rack, StorageLocation storageLocation ) {
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation.getStorageLocationId() );

        TreeSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);
        sortedEvents.addAll( rack.getInPlaceLabEvents() );

        LabEvent latestRackEvent = null;
        for( LabEvent inPlaceEvent : sortedEvents ) {
            if( inPlaceEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN || inPlaceEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT  ) {
                // Nope: check-in and check-out events are not valid to define layout of a rack to be checked in.
                // Something must have changed if it's sitting on the bench after being stored
                continue;
            } else {
                latestRackEvent = inPlaceEvent;
                break;
            }
        }

        // Which event owns which tube formation?
        Map<LabEvent,TubeFormation> eventTubes = new HashMap<>();
        if( latestRackEvent == null ) {
            // Expecting a sample pick XL20 event, none on record? Try to dredge up something from recent transfers
            sortedEvents.clear();
            for (TubeFormation tubes : rack.getTubeFormations()) {
                for( LabEvent evt : tubes.getTransfersTo() ) {
                    sortedEvents.add(evt);
                    eventTubes.put(evt,tubes);
                }
                for( LabEvent evt : tubes.getTransfersFrom() ) {
                    sortedEvents.add(evt);
                    eventTubes.put(evt,tubes);
                }
            }
            if (sortedEvents.size() > 0) {
                latestRackEvent = sortedEvents.last();
            }
        }

        if( latestRackEvent == null ) {
            return Pair.of("danger", "Rack barcode " + barcode
                    + " has no event activity.  Tube layout unavailable.");
        }

        Date twoWorkingDaysAgo = DateUtils.getPastWorkdayStartFromNow(2);
        Date eventDate = latestRackEvent.getEventDate();
        if( eventDate.before( twoWorkingDaysAgo ) ) {
            return Pair.of("danger", "Latest event date (" + DateUtils.formatISO8601Date( eventDate ) + ") for rack " + barcode
                    + " is over 2 working days ago. Tube layout questionable - ignoring.");
        }

        LabEvent checkOutEvent = createStorageEvent( LabEventType.STORAGE_CHECK_IN, eventTubes.get(latestRackEvent), storageLocation, rack );
        storageLocationDao.persist(checkOutEvent);

        return Pair.of("success", "Rack barcode " + barcode
                + " and all tubes checked out of location "
                + locationTrail + ".");
    }

    private LabEvent createStorageEvent(LabEventType labEventType, LabVessel inPlaceVessel, StorageLocation storageLocation, LabVessel ancillaryInPlaceVessel ){
        LabEvent checkOutEvent = new LabEvent(labEventType, new Date(), LabEvent.UI_PROGRAM_NAME, 01L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME );
        checkOutEvent.setInPlaceLabVessel(inPlaceVessel);
        checkOutEvent.setStorageLocation(storageLocation);
        checkOutEvent.setAncillaryInPlaceVessel(ancillaryInPlaceVessel);
        return checkOutEvent;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getStorageLocationId() {
        return storageLocationId;
    }

    public void setStorageLocationId(Long storageLocationId) {
        this.storageLocationId = storageLocationId;
    }

    public String getCheckInPhase() {
        return checkInPhase;
    }

    /**
     * POST sends a comma delimited list
     */
    public void setProposedLocationIds(String proposedLocationIdString) {
        this.proposedLocationIds = new ArrayList<>();
        String[] ids = proposedLocationIdString.split(",");
        for( int i = 0; i < ids.length; i++ ) {
            this.proposedLocationIds.add( ids[i] );
        }
    }

    public List<StorageLocation> getValidLocations() {
        return validLocations;
    }

    public Map<Long, String> getStorageLocPaths() {
        return storageLocPaths;
    }
}

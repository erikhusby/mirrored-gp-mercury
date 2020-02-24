package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.storage.StorageEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.CherryPickTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.SectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.VesselToSectionTransfer;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel_;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.persistence.LockModeType;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * This Action Bean handles bulk SRS storage check-out and check-in operations.
 */
@UrlBinding(value = BulkStorageOpsActionBean.ACTION_BEAN_URL)
public class BulkStorageOpsActionBean extends CoreActionBean {

    private static final Logger logger = Logger.getLogger(BulkStorageOpsActionBean.class.getName());
    public static final String ACTION_BEAN_URL = "/storage/bulkStorageOps.action";

    // An XL20 pick event (type InPlace)can't be any older than this in order to be bulk checked in
    private static final int MAX_WORKDAYS_FOR_PICK_EVENT = 2;

    // Wizard functionality for check-in flow
    private static final String CHECK_IN_PHASE_INIT = "INIT";
    private static final String CHECK_IN_PHASE_READY = "READY";
    // Limit the number of locations gathered at a single page hit
    private static int MAX_BULK_CHECKIN_LOCS = 24;

    // Events
    static final String EVT_INIT_CHECK_OUT = "initCheckOut";
    static final String EVT_CHECK_OUT = "checkOut";
    static final String EVT_INIT_CHECK_IN = "initCheckIn";
    static final String EVT_VALIDATED_CHECK_IN = "validateCheckIn";
    static final String EVT_CHECK_IN = "checkIn";

    // UI Resolutions
    private static final String UI_CHECK_IN = "/storage/bulk_checkin.jsp";
    private static final String UI_CHECK_OUT = "/storage/bulk_checkout.jsp";

    // Web params
    String barcode;
    Long storageLocationId;
    List<String> proposedLocationIds;
    List<String> checkOuts;

    // Instance/display vars
    String checkInPhase = CHECK_IN_PHASE_INIT;
    List<StorageLocation> validLocations; // List of the valid subset of proposed check-in storage locations
    Map<Long,String> storageLocPaths;  // Location path lookup by ID
    Map<String,String> vesselsCheckOutStatus; // Display status of bulk checkouts forwarded from pick list link

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    StorageEjb storageEjb;

    /**
     * Check-Out page (or non-specified event)
     */
    @DefaultHandler
    @HandlesEvent(EVT_INIT_CHECK_OUT)
    public Resolution eventInitCheckOut(){
        if( checkOuts != null && checkOuts.size() > 0 ) {
            buildVesselsCheckOutStatus();
        }
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
            int storedCount = storageLocationDao.getSlotStoredContainerCount( location );

            String locationPath = storageLocPaths.get(location.getStorageLocationId());
            if( locationPath == null ) {
                locationPath = location.getLocationType().getDisplayName() + ":  " + storageLocationDao.getLocationTrail( location );
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
        }

        StorageLocation storageLocation = storageLocationDao.findById(StorageLocation.class,storageLocationId );
        if( storageLocation == null ) {
            return buildAjaxOutcome( Pair.of("danger", "Barcode : " + barcode + " - No storage location exists for ID: " + storageLocationId ) );
        }

        String locationTrail = storageLocationDao.getLocationTrail(storageLocation);
        Long userId = getUserBean().getBspUser().getUserId();

        Pair<String, String> statusMessage = null;
        if( OrmUtil.proxySafeIsInstance( vessel, BarcodedTube.class ) ) {
            BarcodedTube tube = OrmUtil.proxySafeCast( vessel, BarcodedTube.class );
            if( storageLocation.getLocationType() == StorageLocation.LocationType.LOOSE ) {
                tube.setStorageLocation(storageLocation);
                LabEvent checkInEvent = storageEjb.createStorageEvent( LabEventType.STORAGE_CHECK_IN, tube, storageLocation, null, userId );
                statusMessage = Pair.of("success", "Vessel barcode " + barcode
                        + " checked into 'loose' location " + locationTrail + ".");
            } else {
                statusMessage = Pair.of("warning", "Storage location " + locationTrail + " is not configured to store loose vessels - ignoring.");
            }
        } else if( OrmUtil.proxySafeIsInstance( vessel, StaticPlate.class ) ) {
            StaticPlate plate = OrmUtil.proxySafeCast( vessel, StaticPlate.class );
            plate.setStorageLocation(storageLocation);
            LabEvent checkInEvent = storageEjb.createStorageEvent( LabEventType.STORAGE_CHECK_IN, plate, storageLocation, null, userId  );
            statusMessage = Pair.of("success", "Plate " + barcode
                    + " checked into location " + locationTrail + ".");
        } else if( OrmUtil.proxySafeIsInstance( vessel, RackOfTubes.class ) ) {
            RackOfTubes rack = OrmUtil.proxySafeCast( vessel, RackOfTubes.class );
            statusMessage = doCheckIn( rack, storageLocation, userId );
        } else if( statusMessage == null ) {
            // How did a flowcell get into storage?  Probably not a real case
            statusMessage = Pair.of("danger", "Vessel barcode " + barcode + " type currently not storable.");
        }

        return buildAjaxOutcome(statusMessage);
    }

    /**
     * Check vessel out of storage, called from ajax
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
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation );
        tube.setStorageLocation(null);
        LabEvent checkOutEvent = storageEjb.createStorageEvent( LabEventType.STORAGE_CHECK_OUT, tube, storageLocation, null, getUserBean().getBspUser().getUserId() );
        return Pair.of("success", "Checked out vessel barcode " + barcode
                + " from 'loose' location " + locationTrail + ".");
    }

    private Pair<String, String> doCheckOut( StaticPlate plate ) {
        StorageLocation storageLocation = plate.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation );
        plate.setStorageLocation(null);
        LabEvent checkOutEvent = storageEjb.createStorageEvent( LabEventType.STORAGE_CHECK_OUT, plate, storageLocation, null, getUserBean().getBspUser().getUserId() );
        return Pair.of("success", "Checked out plate barcode " + barcode + " from " + locationTrail + ".");
    }

    private Pair<String, String> doCheckOut( RackOfTubes rack ) {
        StorageLocation rackLocation = rack.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( rackLocation );

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
            // In storage but no check-in event?
            return Pair.of("danger", "How did rack barcode " + barcode
                    + " get in storage at " + locationTrail + " without any check-in event?  Ignoring.");
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

            LabEvent checkOutEvent = storageEjb.createStorageEvent( LabEventType.STORAGE_CHECK_OUT, checkOutTubes, rackLocation, rack, getUserBean().getBspUser().getUserId() );
        }

        return Pair.of("success", "Checked out rack barcode " + barcode
                + " and all tubes from " + locationTrail + ".");
    }

    /**
     * Look for recent events to determine if TubeFormation is trust-able for layout and reject or do check in
     * @return Pair of values, if left not 'success', then nothing was done
     */
    private Pair<String, String> doCheckIn( RackOfTubes rack, StorageLocation storageLocation, Long userId ) {

        // Prefix a warning to status message
        String warningMessage = null;

        TreeSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);
        LabVessel tubeFormation = null;
        LabEvent latestRackEvent = null;

        // Find the latest event
        sortedEvents.addAll(rack.getInPlaceLabEvents());
        for (TubeFormation formation : rack.getTubeFormations()) {
            sortedEvents.addAll(formation.getContainerRole().getTransfersTo());
            sortedEvents.addAll(formation.getContainerRole().getTransfersFrom());
        }
        for (Iterator<LabEvent> iter = sortedEvents.descendingIterator(); iter.hasNext(); ) {
            latestRackEvent = iter.next();
            break;
        }

        if (latestRackEvent == null) {
            return Pair.of("danger", "Rack barcode " + barcode
                    + " has no lab event activity.");
        }

        switch (latestRackEvent.getLabEventType()) {
            case IN_PLACE:
                Date twoWorkingDaysAgo = DateUtils.getPastWorkdaysFrom(new Date(), MAX_WORKDAYS_FOR_PICK_EVENT);
                Date eventDate = latestRackEvent.getEventDate();
                // A scan warning should accompany status message if scan occurred more than MAX_WORKDAYS_FOR_PICK_EVENT ago
                if (eventDate.before(twoWorkingDaysAgo)) {
                    // Scan event is old, how?  Left it sitting on the counter?
                    warningMessage = "Scan date (" + DateUtils.formatISO8601Date(eventDate) + ") for rack " + barcode
                            + " is over " + MAX_WORKDAYS_FOR_PICK_EVENT + " working days ago.";
                }
                // Trust tube formation of the (very recent) scan
                tubeFormation = latestRackEvent.getInPlaceLabVessel();
                break;
            case STORAGE_CHECK_IN:
                warningMessage = "Using layout at storage check-in date (" + DateUtils.formatISO8601Date(latestRackEvent.getEventDate())
                        + ") for rack " + barcode + ".";
                tubeFormation = latestRackEvent.getInPlaceLabVessel();
                break;
            case STORAGE_CHECK_OUT:
                // Checked out and now checking back in?
                warningMessage = "Last event for rack " + barcode + " was a check-out, assuming the layout hasn't changed.";
                tubeFormation = latestRackEvent.getInPlaceLabVessel();
                break;
            default:
                // Checked out and now checking back in?
                warningMessage = "Using layout of last event for rack " + barcode + ":  " + latestRackEvent.getLabEventType().getName() + " on " + DateUtils.formatISO8601Date(latestRackEvent.getEventDate());
                tubeFormation = latestRackEvent.getInPlaceLabVessel();
        }

        LabEvent checkInEvent = storageEjb.createStorageEvent(LabEventType.STORAGE_CHECK_IN, tubeFormation, storageLocation, rack, userId);
        rack.setStorageLocation(storageLocation);
        for (LabVessel tube : tubeFormation.getContainerRole().getContainedVessels()) {
            tube.setStorageLocation(storageLocation);
        }
        storageLocationDao.flush();

        if (warningMessage != null) {
            return Pair.of("warning", warningMessage + "  Checked into "
                    + storageLocationDao.getLocationTrail(storageLocation) + ".");
        } else {
            return Pair.of("success", "Rack barcode " + barcode
                    + " and all tubes checked into "
                    + storageLocationDao.getLocationTrail(storageLocation) + ".");
        }
    }

    /**
     * Need to find the latest in-place or transfer event and associated Tubeformation for a rack after a given date
     *   (check-in date) to get layout for a bulk check-in <br/>
     * How much of a leap of faith is it to assume the tube layout was not changed since?
     */
    private Pair<LabEvent,LabVessel> getLatestRackEvent(RackOfTubes rack, Date earliestDate ){
        TreeSet<LabEvent> sortedEvents = new TreeSet<>(LabEvent.BY_EVENT_DATE);

        // Get all associated events for the rack's tube formations
        for (TubeFormation tubes : rack.getTubeFormations()) {
            sortedEvents.addAll( tubes.getTransfersTo() );
            sortedEvents.addAll(tubes.getInPlaceLabEvents());
            sortedEvents.addAll(tubes.getTransfersFrom());
        }

        // Prune all earlier events
        for( Iterator<LabEvent> iter = sortedEvents.iterator(); iter.hasNext(); ) {
            if( iter.next().getEventDate().before(earliestDate)){
                iter.remove();
            }
        }

        if( sortedEvents.isEmpty() ) {
            return null;
        }

        // Use the latest tube formation
        // Make sure rack (ancillary vessel) associated with the tube formation is the same as being attempted to check in
        LabEvent latest = null;
        for( Iterator<LabEvent> iter = sortedEvents.descendingIterator(); iter.hasNext(); ) {
            latest = iter.next();
            LabVessel xferRack;
            if( latest.getAncillaryInPlaceVessel() != null && latest.getAncillaryInPlaceVessel().getLabel().equals(rack.getLabel()) ) {
                return Pair.of(latest, latest.getInPlaceLabVessel());
            }
            for( CherryPickTransfer xfer: latest.getCherryPickTransfers() ) {
                xferRack = xfer.getAncillaryTargetVessel();
                if( xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
                xferRack = xfer.getAncillarySourceVessel();
                if( xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getSourceVessel());
                }
            }
            for( SectionTransfer xfer: latest.getSectionTransfers() ) {
                xferRack = xfer.getAncillaryTargetVessel();
                if( xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
                xferRack = xfer.getAncillarySourceVessel();
                if( xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getSourceVessel());
                }
            }
            // Only valid for target
            for( VesselToSectionTransfer xfer: latest.getVesselToSectionTransfers() ) {
                xferRack = xfer.getAncillaryTargetVessel();
                if( xferRack != null && xferRack.getLabel().equals(rack.getLabel())) {
                    return Pair.of(latest, xfer.getTargetVessel());
                }
            }
        }

        // Still here?  Fail
        return null;
    }


    /**
     * Given a list of barcodes forwarded from pick workspace, build a list of statuses
     */
    private void buildVesselsCheckOutStatus() {
        vesselsCheckOutStatus = new HashMap<>();
        for( String barcode : checkOuts ) {
            LabVessel vessel = storageLocationDao.findSingleSafely( LabVessel.class, LabVessel_.label, barcode, LockModeType.NONE );
            if( vessel == null ) {
                vesselsCheckOutStatus.put(barcode,"Vessel barcode " + barcode + " not found");
                continue;
            }
            StorageLocation storageLocation = vessel.getStorageLocation();
            if( storageLocation != null ) {
                vesselsCheckOutStatus.put(barcode, "Pending: " + barcode + " - " +  storageLocationDao.getLocationTrail( storageLocation ) );
            } else {
                // This had better be check-out if not in storage
                LabEvent checkOutEvent = vessel.getLatestStorageEvent();
                if( checkOutEvent.getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
                    vesselsCheckOutStatus.put(barcode, "N/A: " + barcode + " checked out on " + SimpleDateFormat.getDateInstance().format(checkOutEvent.getEventDate()));
                } else {
                    vesselsCheckOutStatus.put(barcode, "N/A: " + barcode + " not in storage (and never checked out)" );
                }
            }
        }
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

    /**
     * Setter for bulk checkout forward from pick workspace
     */
    public void setCheckOuts(List<String> checkOuts) {
        this.checkOuts = checkOuts;
    }

    /**
     * Display values for each barcode forwarded from pick workspace
     */
    public Map<String, String> getVesselsCheckOutStatus() {
        return vesselsCheckOutStatus == null? Collections.emptyMap():vesselsCheckOutStatus;
    }

}

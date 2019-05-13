package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.*;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * This Action Bean handles bulk SRS storage check-out and check-in operations.
 */
@UrlBinding(value = BulkStorageOpsActionBean.ACTION_BEAN_URL)
public class BulkStorageOpsActionBean extends CoreActionBean {


    private static final Logger logger = Logger.getLogger(BulkStorageOpsActionBean.class.getName());
    public static final String ACTION_BEAN_URL = "/vessel/bulkStorageOps.action";

    // Events
    private static final String EVT_INIT = "init";
    private static final String EVT_CHECK_IN = "bulkCheckIn";
    private static final String EVT_CHECK_OUT = "checkOut";

    // UI Resolutions
    private static final String UI_DEFAULT = "/storage/bulk_ops.jsp";
    private static final String UI_AJAX_MESSAGES = "/storage/bulk_ops.jsp";

    // Web params
    String barcode;

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private LabEventDao labEventDao;

    /**
     * Initial landing - user needs to select or create an existing workspace
     */
    @DefaultHandler
    @HandlesEvent(EVT_INIT)
    public Resolution eventInit(){
        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * Add or remove SRS batch vessels
     */
    @HandlesEvent(EVT_CHECK_IN)
    public Resolution eventProcessBatches(){
        return new ForwardResolution(UI_DEFAULT);
    }

    /**
     * Check vessel out of storage
     */
    @HandlesEvent(EVT_CHECK_OUT)
    public Resolution eventCheckOut(){

        LabVessel vessel = storageLocationDao.findSingle( LabVessel.class, LabVessel_.label, barcode );
        Pair<String, String> statusMessage = null;

        // Validate the obvious
        if( vessel == null ) {
            statusMessage = Pair.of("danger", "Vessel barcode " + barcode + " not found.");
        } else if( vessel.getStorageLocation() == null ) {
            statusMessage = Pair.of("warning", "Vessel barcode " + barcode + " not in storage.");
        }

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

        JsonObject result = Json.createObjectBuilder()
                .add("feedbackLevel", statusMessage.getLeft())
                .add( "feedbackMessage", statusMessage.getRight())
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
        LabEvent checkOutEvent = createCheckOutEvent( tube, storageLocation,null );
        storageLocationDao.persist(checkOutEvent);
        return Pair.of("success", "Vessel barcode " + barcode
                + " checked out of 'loose' location "
                + locationTrail + ".");
    }

    private Pair<String, String> doCheckOut( StaticPlate plate ) {
        StorageLocation storageLocation = plate.getStorageLocation();
        String locationTrail = storageLocationDao.getLocationTrail( storageLocation.getStorageLocationId() );
        plate.setStorageLocation(null);
        LabEvent checkOutEvent = createCheckOutEvent( plate, storageLocation, null );
        storageLocationDao.persist(checkOutEvent);
        return Pair.of("success", "Plate barcode " + barcode
                + " checked out of location "
                + locationTrail + ".");
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

            LabEvent checkOutEvent = createCheckOutEvent( checkOutTubes, rackLocation, rack );
            storageLocationDao.persist(checkOutEvent);
        }


        return Pair.of("success", "Rack barcode " + barcode
                + " and all tubes checked out of location "
                + locationTrail + ".");
    }

    private LabEvent createCheckOutEvent( LabVessel inPlaceVessel, StorageLocation storageLocation, LabVessel ancillaryInPlaceVessel ){
        LabEvent checkOutEvent = new LabEvent(LabEventType.STORAGE_CHECK_OUT, new Date(), LabEvent.UI_PROGRAM_NAME, 01L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME );
        checkOutEvent.setInPlaceLabVessel(inPlaceVessel);
        checkOutEvent.setStorageLocation(storageLocation);
        checkOutEvent.setAncillaryInPlaceVessel(ancillaryInPlaceVessel);
        return checkOutEvent;
    }

    /**
     * Refreshes list of SRS batches and merges with existing
     */
    public void refreshSrsBatchList() {
        // TODO: implement
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }
}

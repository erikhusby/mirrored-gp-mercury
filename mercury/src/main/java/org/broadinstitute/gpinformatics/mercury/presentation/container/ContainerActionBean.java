package org.broadinstitute.gpinformatics.mercury.presentation.container;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateEventType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PlateType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.PositionMapType;
import org.broadinstitute.gpinformatics.mercury.bettalims.generated.ReceptacleType;
import org.broadinstitute.gpinformatics.mercury.control.dao.labevent.LabEventDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.*;


/**
 * Everything that has to do with creating, storing, and removing a rack of tubes or static plate in freezers.
 * */
@UrlBinding(ContainerActionBean.ACTION_BEAN_URL)
public class ContainerActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(ContainerActionBean.class);

    public static final String PAGE_TITLE = "View Container";
    public static final String CREATE_CONTAINER_TITLE = "Create Container";
    public static final String EDIT_CONTAINER_TITLE = "Edit Container";

    public static final String ACTION_BEAN_URL = "/container/container.action";
    public static final String CONTAINER_CREATE_PAGE = "/container/create.jsp";
    public static final String CONTAINER_VIEW_PAGE = "/container/view.jsp";
    public static final String CONTAINER_VIEW_SHIM_PAGE = "/container/container_view.jsp";
    public static final String LOOSE_VESSEL_SHIM_PAGE = "/storage/loose_vessels.jsp";
    public static final String CREATE_CONTAINER_ACTION = "createContainer";
    public static final String VIEW_CONTAINER_ACTION = "viewContainer";
    public static final String VIEW_CONTAINER_SEARCH_ACTION = "viewContainerSearch";
    public static final String VIEW_CONTAINER_AJAX_ACTION = "viewContainerAjax";
    public static final String CONTAINER_PARAMETER = "containerBarcode";
    public static final String SHOW_LAYOUT_PARAMETER = "showLayout";
    public static final String SAVE_LOCATION_ACTION = "saveLocation";
    public static final String REMOVE_LOCATION_ACTION = "removeLocation";
    public static final String REMOVE_LOOSE_LOC_ACTION = "removeLooseLoc";
    public static final String CANCEL_SAVE_ACTION = "cancel";
    public static final String FIRE_RACK_SCAN = "rackScan";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventDao labEventDao;

    private String containerBarcode;
    private String storageName;
    private String storageId;

    private BarcodedTube barcodedTube;
    private StaticPlate staticPlate;
    private RackOfTubes.RackType rackType;
    private RackOfTubes rackOfTubes;
    private Map<VesselPosition, LabVessel> mapPositionToVessel;
    private Map<VesselPosition, String> mapPositionToSampleId;
    private Map<String, String> mapBarcodeToSampleId;

    private List<String> selectedLooseVesselBarcodes;
    private List<LabVessel> looseTubes;
    private boolean editLayout = false;
    private List<ReceptacleType> receptacleTypes;
    private StorageLocation storageLocation;
    private String locationTrail;
    private boolean ajaxRequest;
    private boolean showLayout;
    private boolean ignoreCheckinEvents = false;

    public ContainerActionBean() {
        super(CREATE_CONTAINER_TITLE, EDIT_CONTAINER_TITLE, CONTAINER_PARAMETER);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    @DefaultHandler
    @DontValidate
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    /* *** BEGIN CREATE CONTAINER FUNCTIONALITY *** */
    @DontValidate
    @HandlesEvent(CREATE_ACTION)
    public Resolution viewCreatePage() {
        return new ForwardResolution(CONTAINER_CREATE_PAGE);
    }

    @ValidationMethod(on = CREATE_CONTAINER_ACTION)
    public void validateVesselNotExist(ValidationErrors errors) {
        if (StringUtils.isEmpty(containerBarcode)) {
            addValidationError("containerBarcode", "Container Barcode is required.");
        } else if (labVesselDao.findByIdentifier(containerBarcode) != null) {
            addValidationError("containerBarcode", "Barcode is already associated with another lab vessel");
        }
    }

    /**
     * Creates racks of tubes only!  Static plates MUST exist already via sample import or transfers. <br/>
     * Redirects to 'Check /in / Search Container' - "viewContainer" event page if successful
     */
    @HandlesEvent(CREATE_CONTAINER_ACTION)
    public Resolution createContainer() {
        RackOfTubes rackOfTubes = new RackOfTubes(containerBarcode, rackType);
        labVesselDao.persist(rackOfTubes);
        labVesselDao.flush();
        addMessage("Successfully created new container: " + containerBarcode);
        return new RedirectResolution(ContainerActionBean.class, VIEW_CONTAINER_ACTION)
                .addParameter(CONTAINER_PARAMETER, containerBarcode);
    }
    /* *** END CREATE CONTAINER FUNCTIONALITY *** */

    /**
     * Validates existence of the container only, existence of tubes is handled farther on
     */
    @ValidationMethod(on = {VIEW_CONTAINER_ACTION, EDIT_ACTION, SAVE_ACTION, SAVE_LOCATION_ACTION,
            CANCEL_SAVE_ACTION, FIRE_RACK_SCAN, REMOVE_LOCATION_ACTION, VIEW_CONTAINER_SEARCH_ACTION})
    public void validateVesselExist() {
        if( selectedLooseVesselBarcodes != null ) {
            looseTubes = new ArrayList<>();
            for( String barcode : selectedLooseVesselBarcodes ) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if( labVessel == null ) {
                    addGlobalValidationError("No vessel exists with barcode %s.", barcode);
                } else if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                    looseTubes.add(OrmUtil.proxySafeCast(labVessel, BarcodedTube.class));
                } else {
                    addGlobalValidationError(
                            "Lab vessel barcode [" + barcode + ", type [" + labVessel.getType() + "] currently not allowed to be stored individually.");
                }
                if( looseTubes.size() > 0 ) {
                    storageLocation = looseTubes.get(0).getStorageLocation();
                }
            }
        } else if (StringUtils.isEmpty(containerBarcode)) {
            addValidationError("containerBarcode", "No vessels selected.");
            return;
        } else {
            LabVessel labVessel = labVesselDao.findByIdentifier(containerBarcode);
            if (labVessel == null) {
                addValidationError(containerBarcode, "Failed to find lab vessel: " + containerBarcode);
            } else if (OrmUtil.proxySafeIsInstance(labVessel, RackOfTubes.class)) {
                rackOfTubes = OrmUtil.proxySafeCast(labVessel, RackOfTubes.class);
                storageLocation = rackOfTubes.getStorageLocation();
            } else if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
                storageLocation = staticPlate.getStorageLocation();
            } else if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                storageLocation = barcodedTube.getStorageLocation();
            } else {
                addValidationError(containerBarcode,
                        "Lab vessel barcode [" + containerBarcode + ", type [" + labVessel.getType()
                        + "] currently not allowed to be stored.");
            }
        }
    }

    /**
     * Attempt to determine the position map of the container under the disclaimer that a RackOfTubes
     * layout shown may not reflect the layout when previously checked in due to rearrays.
     * TODO:  JMS Lots of logic unrelated to validation, call this explicitly as needed instead of wiring into Stripes lifecycle
     */
    @After(stages = LifecycleStage.CustomValidation, on = {EDIT_ACTION, CANCEL_SAVE_ACTION, REMOVE_LOCATION_ACTION})
    public void buildPositionMappingForInStorage() {
        mapPositionToVessel = new HashMap<>();
        mapPositionToSampleId = new HashMap<>();

        // Layouts are not shown for static plates (showLayout=false)
        if ( isStaticPlate() ) {
            return;
        }
        // No mapping logic required for loose tubes
        if( looseTubes != null || barcodedTube != null ) {
            return;
        }
        if( !rackOfTubes.getRackType().isRackScannable() ) {
            showLayout = true;
        }

        // Skip event/container logic overhead if no layout to be shown (after 'Find' viewContainer() redirect action)
        // Or forced to use rack scan layout
        if( !showLayout || ignoreCheckinEvents ) {
            return;
        }

        LabEvent latestEvent = findLatestCheckInEvent(rackOfTubes);
        mapPositionToVessel = findStoredTubesFromCheckIn(latestEvent);
        mapPositionToSampleId = getSamplesForTubes(mapPositionToVessel);

    }

    /**
     * Find latest check-in event for a lab vessel (rack of tubes, static plate) <br/>
     * If a check-out occurs after a check-in, do not report the check-in event
     */
    private LabEvent findLatestCheckInEvent(LabVessel labVessel ) {
        LabEvent latestStorageEvent = labVessel.getLatestStorageEvent();
        if( latestStorageEvent.getLabEventType() == LabEventType.STORAGE_CHECK_IN ) {
            return latestStorageEvent;
        } else {
            return null;
        }
    }

    /**
     * Given a storage check-in event for a rack, build a map of positions and tubes in the check-in event tube formation
     * which are still in the same storage location as the rack.  <br/>
     * Handles the case where a tube may have been removed from the rack without a storage checkout.
     */
    private Map<VesselPosition, LabVessel> findStoredTubesFromCheckIn(LabEvent checkInEvent ) {

        Map<VesselPosition, LabVessel> mapPositionToStoredVessel = new HashMap<>();

        // Spare the caller having to test
        if( checkInEvent == null ) {
            return mapPositionToStoredVessel;
        }

        TubeFormation eventTubeFormation = null;

        LabVessel inPlaceVessel = checkInEvent.getInPlaceLabVessel();
        if( OrmUtil.proxySafeIsInstance( inPlaceVessel, TubeFormation.class) ) {
            eventTubeFormation = OrmUtil.proxySafeCast(inPlaceVessel, TubeFormation.class);
        }

        // Shouldn't happen
        if( eventTubeFormation == null ) {
            return mapPositionToStoredVessel;
        }

        Long rackLocId = rackOfTubes.getStorageLocation()==null?null:rackOfTubes.getStorageLocation().getStorageLocationId();
        Long tubeLocId;

        VesselContainer<?> containerRole = eventTubeFormation.getContainerRole();
        if (containerRole != null) {
            for ( Map.Entry<VesselPosition,BarcodedTube> positionAndTube : eventTubeFormation.getContainerRole().getMapPositionToVessel().entrySet() ) {
                BarcodedTube tube = positionAndTube.getValue();
                VesselPosition position = positionAndTube.getKey();
                tubeLocId = tube.getStorageLocation()==null?null:tube.getStorageLocation().getStorageLocationId();

                // Do not show any tubes checked in to other locations
                if( tubeLocId != null && rackLocId != null && !tubeLocId.equals(rackLocId) ) {
                    String locationPath = storageLocationDao.getLocationTrail(tubeLocId);
                    addMessage(tube.getLabel() + " [ " + position + " ] was moved to " + locationPath );
                    continue;
                }

                mapPositionToStoredVessel.put(position, tube);
            }
        }
        return mapPositionToStoredVessel;
    }

    /**
     * Build a map of positions to sample ids corresponding to positions and vessels in a rack
     */
    private Map<VesselPosition,String> getSamplesForTubes(Map<VesselPosition, LabVessel> vesselPositionMap ) {
        Map<VesselPosition, String> mapPositionToSampleId = new HashMap<>();
        if( vesselPositionMap == null ) {
            return mapPositionToSampleId;
        }

        for (Map.Entry<VesselPosition, LabVessel> posVesselEntry : vesselPositionMap.entrySet()) {
            LabVessel vessel = posVesselEntry.getValue();
            Set<MercurySample> samples = vessel.getMercurySamples();
            String display = "";
            if (samples.size() == 1) {
                display = samples.iterator().next().getSampleKey();
                // Don't repeat barcode and sample name if the same
                if( !vessel.getLabel().equals(display)) {
                    mapPositionToSampleId.put(posVesselEntry.getKey(), display);
                }
            } else if (samples.size() > 1) {
                // Not sure if this should ever happen - but code for it
                StringBuilder b = new StringBuilder();
                for (MercurySample sample : samples) {
                    if( !vessel.getLabel().equals(sample.getSampleKey())) {
                        b.append(sample.getSampleKey()).append(" ");
                    }
                }
                display = b.toString().trim();
                if( !display.isEmpty() ) {
                    mapPositionToSampleId.put(posVesselEntry.getKey(), display);
                }
            }
        }
        return mapPositionToSampleId;
    }

    /**
     * Caution:  If vessel barcode is same as sample ID, we put an empty string in placeholder
     * Tightens up display by not being completely redundant
     */
    private String getSampleIdForVessel( LabVessel vessel ) {
        Set<MercurySample> samples = vessel.getMercurySamples();
        String sampleId = "";
        if( samples.size() == 1 ) {
            sampleId =  samples.iterator().next().getSampleKey();
        } else if( samples.size() > 1 ) {
            // Not sure if this should ever happen - but code for it
            StringBuilder b = new StringBuilder();
            for( MercurySample sample : samples ) {
                b.append(sample.getSampleKey()).append(" ");
            }
            sampleId = b.toString().trim();
        }
        if( vessel.getLabel().equals(sampleId) ) {
            return "";
        } else {
            return sampleId;
        }
    }

    private void buildLooseVesselList(){
        if (storageId == null) {
            addGlobalValidationError("Loose vessel location ID is required.");
            return;
        }
        storageLocation = storageLocationDao.findById( StorageLocation.class, Long.valueOf(storageId));
        if (storageLocation == null) {
            addGlobalValidationError("Failed to find storage location by ID: %s", storageId);
            return;
        }

        looseTubes = new ArrayList();
        mapBarcodeToSampleId = new HashMap<>();
        for( LabVessel vessel : storageLocation.getLabVessels() ) {
            looseTubes.add(vessel);
            mapBarcodeToSampleId.put(vessel.getLabel(), getSampleIdForVessel(vessel));
        }

    }

    private boolean isStaticPlate() {
        return staticPlate != null;
    }

    public boolean isContainer() {
        return looseTubes == null && ( staticPlate != null || rackOfTubes != null ) ;
    }

    @HandlesEvent(VIEW_CONTAINER_ACTION)
    public Resolution viewContainer() {
        if (rackOfTubes != null && rackOfTubes.getRackType().isRackScannable()) {
            return new RedirectResolution(ContainerActionBean.class, EDIT_ACTION)
                    .addParameter(CONTAINER_PARAMETER, containerBarcode)
                    .addParameter(SHOW_LAYOUT_PARAMETER, false);
        }

        if( barcodedTube != null ) {
            if( barcodedTube.getTubeType() == BarcodedTube.BarcodedTubeType.CBSStraw_03
                    || barcodedTube.getTubeType() == BarcodedTube.BarcodedTubeType.Slide ) {
                addMessage("Continue only if your intent is to store this vessel in a loose location (e.g. LN2 tank or slide drawer), otherwise add it to a container.");
                storageLocation = barcodedTube.getStorageLocation();
                mapPositionToVessel = new HashMap<>();
                mapPositionToVessel.put(VesselPosition._1_1, barcodedTube);
            } else {
                addGlobalValidationError( "Vessel is a " + barcodedTube.getTubeType() + ". Storage of loose vessels is only allowed for types [CBSStraw_03] and [Slide]");
                barcodedTube = null;
                return new ForwardResolution(CONTAINER_VIEW_PAGE);
            }
        } else {
            LabEvent latestCheckInEvent = findLatestCheckInEvent(getViewVessel());
            mapPositionToVessel = findStoredTubesFromCheckIn(latestCheckInEvent);
        }

        mapPositionToSampleId = getSamplesForTubes(mapPositionToVessel);

        showLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_SEARCH_ACTION)
    public Resolution viewContainerSearch() {
        buildPositionMappingForInStorage();
        showLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_AJAX_ACTION)
    public Resolution viewContainerAjax() {
        ajaxRequest = true;
        showLayout = true;
        // Forward from removeLooseFromLoc()
        if( ( storageLocation != null && storageLocation.getLocationType().equals(StorageLocation.LocationType.LOOSE) )
            ||  StorageLocation.LocationType.LOOSE.name()
                    .equals( getContext().getRequest().getParameter(VIEW_CONTAINER_AJAX_ACTION) ) ) {
            // Show storage location pseudo container loose vessels in a list
            buildLooseVesselList();
            return new ForwardResolution(LOOSE_VESSEL_SHIM_PAGE);
        } else {
            validateVesselExist();
            buildPositionMappingForInStorage();
            if (hasErrors()) {
                throw new RuntimeException("Failed to find lab vessel: " + containerBarcode);
            }
            return new ForwardResolution(CONTAINER_VIEW_SHIM_PAGE);
        }
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution editContainer() {
        editLayout = true;
        if (rackOfTubes != null && !rackOfTubes.getRackType().isRackScannable()) {
            showLayout = true;
        }
        if (storageLocation != null) {
            String locationPath = storageLocationDao.getLocationTrail(storageLocation.getStorageLocationId());
            addMessage("Rack " + getViewVessel().getLabel() + " is currently stored in [" + locationPath + "] and will be removed at layout update.");
        }
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution saveContainer() {
        MessageCollection messageCollection = new MessageCollection();
        handleSaveContainer(messageCollection);
        showLayout = true;
        if (messageCollection.hasErrors()) {
            editLayout = true;
            ignoreCheckinEvents = true;
        } else {
            editLayout = false;
            messageCollection.addInfo("Successfully updated layout.");
            messageCollection.addInfo("Please select storage location.");
        }
        addMessages(messageCollection);
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    /**
     * Confirms a storage update - removes rack and any tubes from storage if applicable
     * User must select a new storage location afterwards
     */
    public void handleSaveContainer(MessageCollection messageCollection) {

        boolean rackWasRemovedFromStorage = false;

        // Save all lab vessels to parent's storage location
        if (receptacleTypes == null) {
            messageCollection.addError("No tube barcodes found.");
            return;
        }

        if (rackOfTubes != null && rackOfTubes.getStorageLocation() != null) {
            // Save this in case user wants to put it right back in same spot
            storageLocation = rackOfTubes.getStorageLocation();
            rackOfTubes.setStorageLocation(null);
            rackWasRemovedFromStorage = true;
            messageCollection.addInfo("Layout update removed rack %s from storage location [%s]."
                    , rackOfTubes.getLabel()
                    , storageLocationDao.getLocationTrail(storageLocation.getStorageLocationId()));

            // Now the part where all tubes associated with rack check-in have to be removed also
            LabEvent checkInEvent = findLatestCheckInEvent( rackOfTubes );
            Collection<LabVessel> vesselsToAlsoRemove = findStoredTubesFromCheckIn( checkInEvent ).values();
            for( LabVessel tube : vesselsToAlsoRemove ) {
                tube.setStorageLocation(null);
            }
            if( vesselsToAlsoRemove.size() > 0 ) {
                messageCollection.addInfo("Also removed %d tubes associated with prior rack %s check-in."
                        , vesselsToAlsoRemove.size()
                        , rackOfTubes.getLabel());
            }
        }

        mapPositionToVessel = new HashMap<>();
        buildTubeLayoutFromPost(messageCollection);

        showLayout = true;

        // Vessel not exist errors
        if (messageCollection.hasErrors()) {
            return;
        }

        for( Map.Entry<VesselPosition,LabVessel> pv : mapPositionToVessel.entrySet() ) {
            if (pv.getValue().getStorageLocation() != null) {
                // Report relocated tubes only when entire rack not relocated
                if( !rackWasRemovedFromStorage ) {
                    messageCollection.addWarning("Layout update removed %s [%s] from [%s]"
                            , pv.getValue().getLabel(), pv.getKey()
                            , storageLocationDao
                                    .getLocationTrail(pv.getValue().getStorageLocation().getStorageLocationId()));
                }
                pv.getValue().setStorageLocation(null);
            }
        }
        // Vessels may have been removed from storage
        labVesselDao.flush();
        labEventDao.flush();
    }

    /**
     * Builds mapPositionToVessel from web post
     * @param messageCollection Holds any errors (vessel not found)
     */
    private void buildTubeLayoutFromPost(MessageCollection messageCollection) {

        mapPositionToVessel = new HashMap<>();

        if (rackOfTubes == null) {
            return;
        }
        if (receptacleTypes == null) {
            messageCollection.addError("You must scan the rack.");
            return;
        }

        // Get vessel barcodes, remove empty positions from raw input
        if( receptacleTypes != null ) {
            for (Iterator<ReceptacleType> iterator = receptacleTypes.iterator(); iterator.hasNext(); ) {
                ReceptacleType receptacleType = iterator.next();
                if (!StringUtils.isEmpty(receptacleType.getBarcode())) {
                    String barcode = receptacleType.getBarcode();
                    LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                    if (labVessel == null) {
                        // Won't even add non-existent vessel to layout map  TODO: JMS Should we?
                        messageCollection.addError("No vessel with barcode %s found.", barcode);
                        continue;
                    }
                    mapPositionToVessel.put(VesselPosition.getByName(receptacleType.getPosition()), labVessel);
                } else {
                    iterator.remove();
                }
            }
        }

        // Some critical validation
        Set<String> barcodes = new HashSet<>();
        for( Map.Entry<VesselPosition,LabVessel> ptov : mapPositionToVessel.entrySet() ) {
            String barcode = ptov.getValue().getLabel();
            if (!OrmUtil.proxySafeIsInstance(ptov.getValue(), BarcodedTube.class)) {
                messageCollection.addError("Barcode isn't a tube type: " + barcode);
            } else if (barcodes.contains(barcode)) {
                messageCollection.addError("Duplicate tube barcode found: " + barcode);
            }
            barcodes.add(barcode);
        }

    }

    @HandlesEvent(FIRE_RACK_SCAN)
    public Resolution fireRackScan() throws ScannerException {
        scan();
        boolean rackScanEmpty = true;
        MessageCollection messageCollection = new MessageCollection();

        mapPositionToVessel = new HashMap<>();
        mapPositionToSampleId = new HashMap<>();

        if( rackScan == null || rackScan.isEmpty() ) {
            messageCollection.addError("No results from rack scan");
        } else {
            if( rackOfTubes != null && rackOfTubes.getStorageLocation() != null ) {
                messageCollection.addWarning("Rack %s will be removed from location [%s] when layout update performed."
                        , rackOfTubes.getLabel()
                        , storageLocationDao.getLocationTrail( rackOfTubes.getStorageLocation().getStorageLocationId()));
            }

            List<String> barcodes = new ArrayList<>(rackScan.values());
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);

            for (Map.Entry<String, String> entry : rackScan.entrySet()) {
                if (StringUtils.isNotEmpty(entry.getValue())) {
                    rackScanEmpty = false;
                    String position = entry.getKey();
                    String barcode = entry.getValue();

                    LabVessel labVessel = mapBarcodeToVessel.get(barcode);
                    if (labVessel == null) {
                        messageCollection.addError("Unrecognized tube barcode: " + barcode);
                        continue;
                    }

                    VesselPosition vesselPosition = VesselPosition.getByName(position);
                    if (vesselPosition == null) {
                        messageCollection.addError("Unrecognized position: " + position);
                        continue;
                    }

                    if( mapPositionToVessel.put(vesselPosition, labVessel) != null ) {
                        messageCollection.addError("Duplicate position: " + position);
                    }

                    if( labVessel.getStorageLocation() != null ) {
                        messageCollection.addWarning("%s [ %s ] will be removed from [%s] when layout update performed."
                                , labVessel.getLabel(), vesselPosition
                                , storageLocationDao.getLocationTrail( labVessel.getStorageLocation().getStorageLocationId()));
                    }
                }
            }
        }

        if (rackScanEmpty) {
            messageCollection.addError("No results from rack scan");
        }

        addMessages(messageCollection);
        if (messageCollection.hasErrors()) {
            mapPositionToVessel.clear();
            showLayout = false;
        } else {
            showLayout = true;
        }
        editLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    /**
     * Builds a bettalims plate event from posting a web layout
     */
    private PlateEventType buildPlateEventFromPost(LabEventType eventType) {
        PlateEventType plateEventType = new PlateEventType();
        plateEventType.setEventType(eventType.getName());
        plateEventType.setStart(new Date());
        plateEventType.setDisambiguator(1L);
        plateEventType.setOperator(getUserBean().getLoginUserName());
        plateEventType.setProgram(LabEvent.UI_PROGRAM_NAME);
        plateEventType.setStation(LabEvent.UI_PROGRAM_NAME);
        PlateType plateType = new PlateType();
        plateEventType.setPlate(plateType);
        plateType.setBarcode(getViewVessel().getLabel());
        plateType.setSection("ALL96");
        if (rackOfTubes != null) {
            PositionMapType positionMapType = new PositionMapType();
            positionMapType.getReceptacle().addAll(receptacleTypes);
            plateEventType.setPositionMap(positionMapType);
            positionMapType.setBarcode(getViewVessel().getLabel());
            plateType.setPhysType(rackOfTubes.getRackType().getDisplayName());
        } else if (staticPlate != null) {
            plateType.setPhysType(staticPlate.getAutomationName());
        }
        return plateEventType;
    }

    /**
     * Builds a bettalims plate event from rack and vessel positions
     */
    private PlateEventType buildPlateEventFromVessels(
            LabVessel vessel, Map<VesselPosition,LabVessel> mapPositionVessel, LabEventType eventType) {
        PlateEventType plateEventType = new PlateEventType();
        plateEventType.setEventType(eventType.getName());
        plateEventType.setStart(new Date());
        plateEventType.setDisambiguator(1L);
        plateEventType.setOperator(getUserBean().getLoginUserName());
        plateEventType.setProgram(LabEvent.UI_PROGRAM_NAME);
        plateEventType.setStation(LabEvent.UI_PROGRAM_NAME);
        PlateType plateType = new PlateType();
        plateEventType.setPlate(plateType);
        plateType.setBarcode(vessel.getLabel());
        plateType.setSection("ALL96");
        PositionMapType positionMapType = new PositionMapType();
        positionMapType.setBarcode(vessel.getLabel());
        for( Map.Entry<VesselPosition,LabVessel> posVesselEntry : mapPositionVessel.entrySet() ) {
            ReceptacleType receptacleType = new ReceptacleType();
            receptacleType.setBarcode(posVesselEntry.getValue().getLabel());
            receptacleType.setPosition(posVesselEntry.getKey().name());
            positionMapType.getReceptacle().add(receptacleType);
        }
        plateEventType.setPositionMap(positionMapType);
        plateType.setPhysType(rackOfTubes.getRackType().getDisplayName());
        return plateEventType;
    }

    @HandlesEvent(CANCEL_SAVE_ACTION)
    public Resolution cancelSave() {
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(SAVE_LOCATION_ACTION)
    public Resolution saveToLocation() {
        MessageCollection messageCollection = new MessageCollection();
        boolean isLooseTubes = false;
        if (StringUtils.isEmpty(storageId)) {
            addValidationError("storageName", "Storage Location Required.");
            return new ForwardResolution(CONTAINER_VIEW_PAGE);
        }
        storageLocation = storageLocationDao.findById(StorageLocation.class, Long.valueOf(storageId));

        LabEvent checkInEvent;

        if( looseTubes != null ) {
            isLooseTubes = true;

            // Put loose tubes in 'Loose' pseudo location
            StorageLocation looseSubLocation = null;
            if( !storageLocation.getLocationType().equals(StorageLocation.LocationType.LOOSE ) ) {
                // Is there a loose child?
                for( StorageLocation child : storageLocation.getChildrenStorageLocation() ) {
                    if( child.getLocationType().equals(StorageLocation.LocationType.LOOSE ) ) {
                        looseSubLocation = child;
                        break;
                    }
                }
                if( looseSubLocation == null ) {
                    looseSubLocation =
                            new StorageLocation("Loose", StorageLocation.LocationType.LOOSE, storageLocation);
                    storageLocationDao.persist(looseSubLocation);
                }
                storageLocation = looseSubLocation;
            }
            for( LabVessel tube : looseTubes ) {
                tube.setStorageLocation(storageLocation);
                checkInEvent = new LabEvent(LabEventType.STORAGE_CHECK_IN, new Date(), LabEvent.UI_PROGRAM_NAME
                        , 1L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME );
                checkInEvent.setInPlaceLabVessel(tube);
                storageLocationDao.persist(checkInEvent);
            }
        } else if ( rackOfTubes != null ) {
            mapPositionToVessel = new HashMap<>();
            buildTubeLayoutFromPost(messageCollection);

            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
                return new ForwardResolution(CONTAINER_VIEW_PAGE);
            }
            rackOfTubes.setStorageLocation(storageLocation);
            for( LabVessel vessel : mapPositionToVessel.values() ) {
                vessel.setStorageLocation(storageLocation);
            }
            PlateEventType plateEventType =
                    buildPlateEventFromVessels(rackOfTubes, mapPositionToVessel, LabEventType.STORAGE_CHECK_IN);
            checkInEvent = labEventFactory.buildFromBettaLims(plateEventType);
            storageLocationDao.persist(checkInEvent);
            messageCollection.addInfo("Rack of tubes %s checked in to [%s]."
                    , rackOfTubes.getLabel()
                    , storageLocationDao.getLocationTrail(storageLocation.getStorageLocationId()));
        } else {
            // static plate
            LabVessel checkInVessel = getViewVessel();
            checkInVessel.setStorageLocation(storageLocation);
            checkInEvent = new LabEvent(LabEventType.STORAGE_CHECK_IN, new Date(), LabEvent.UI_PROGRAM_NAME
                    , 1L, getUserBean().getBspUser().getUserId(), LabEvent.UI_PROGRAM_NAME );
            checkInEvent.setInPlaceLabVessel(checkInVessel);
            storageLocationDao.persist(checkInEvent);
        }

        storageLocationDao.flush();
        labVesselDao.flush();
        addMessage("Successfully added to storage.");
        if( isLooseTubes ) {
            buildLooseVesselList();
            editLayout = false;
            showLayout = true;
            return new ForwardResolution(LOOSE_VESSEL_SHIM_PAGE);
        } else {
            showLayout = true;
            return new ForwardResolution(CONTAINER_VIEW_PAGE);
        }
    }

    @HandlesEvent(REMOVE_LOCATION_ACTION)
    public Resolution removeFromLocation() {
        MessageCollection messageCollection = new MessageCollection();
        if (storageLocation != null) {
            LabVessel storedVessel = getViewVessel();
            buildTubeLayoutFromPost(messageCollection);
            if (!messageCollection.hasErrors()) {
                storedVessel.setStorageLocation(null);
                messageCollection.addInfo("Removed %s from storage.", storedVessel.getLabel());
                if (mapPositionToVessel != null) {
                    for (LabVessel barcodedTube : mapPositionToVessel.values()) {
                        barcodedTube.setStorageLocation(null);
                    }
                }
                PlateEventType plateEventType = buildPlateEventFromPost(LabEventType.STORAGE_CHECK_OUT);
                LabEvent labEvent = labEventFactory.buildFromBettaLims(plateEventType);

                storageLocationDao.persist(labEvent);
                storageLocationDao.flush();
                labVesselDao.flush();
                storageLocation = null;
            }
            addMessages(messageCollection);
        } else {
            addValidationError("storageName","Lab Vessel not in storage.");
        }

        return new RedirectResolution(ContainerActionBean.ACTION_BEAN_URL)
                .addParameter(CONTAINER_PARAMETER, containerBarcode)
                .addParameter(VIEW_CONTAINER_ACTION, "");
    }

    @HandlesEvent(REMOVE_LOOSE_LOC_ACTION)
    public Resolution removeLooseFromLoc() {

        // This needs to be present for redisplay - should always be present anyways
        if ( storageId == null ) {
            addGlobalValidationError("Loose vessel location is required.");
        }

        Long userId = getUserBean().getBspUser().getUserId();
        if( selectedLooseVesselBarcodes != null && !selectedLooseVesselBarcodes.isEmpty()) {
            for (String barcode : selectedLooseVesselBarcodes) {
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    addGlobalValidationError("Failed to find lab vessel: %s", barcode);
                } else {
                    if (OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                        labVessel.setStorageLocation(null);
                        LabEvent checkInEvent = new LabEvent(LabEventType.STORAGE_CHECK_OUT, new Date(), LabEvent.UI_PROGRAM_NAME
                                , 1L, userId, LabEvent.UI_PROGRAM_NAME );
                        checkInEvent.setInPlaceLabVessel(labVessel);
                        storageLocationDao.persist(checkInEvent);
                        addMessage("Vessel " + labVessel.getLabel() + " removed from storage.");
                    } else {
                        // Loose tubes are allowed to be stored ONLY in LOOSE location! How did this get here?
                        addGlobalValidationError(
                                "Vessel [%s] is not allowed to be stored outside of a container!  How did this get into location ID: %s"
                                , barcode, storageId);
                    }
                }
            }
            storageLocationDao.flush();
        } else {
            addGlobalValidationError("No vessels selected.");
        }

        buildLooseVesselList();
        editLayout = false;
        showLayout = true;
        return new ForwardResolution(LOOSE_VESSEL_SHIM_PAGE);
    }

    public boolean isMoveAllowed() {
        Collection<Role> roles = getUserBean().getRoles();
        return roles.contains(Role.LabManager) || roles.contains(Role.Developer);
    }

    @Override
    public boolean isEditAllowed() {
        return rackOfTubes != null;
    }

    public String getContainerBarcode() {
        return containerBarcode;
    }

    public void setContainerBarcode(String containerBarcode) {
        this.containerBarcode = containerBarcode;
    }

    /**
     * Multi checkbox UI input (vessel barcodes)
     */
    public void setSelectedLooseVessels( String[] selectedLooseVesselBarcodes) {
        if( this.selectedLooseVesselBarcodes == null ) {
            this.selectedLooseVesselBarcodes = new ArrayList<>();
        }
        for( String input : selectedLooseVesselBarcodes ) {
            this.selectedLooseVesselBarcodes.add( input );
        }
    }

    public RackOfTubes.RackType getRackType() {
        return rackType;
    }

    public void setRackType(RackOfTubes.RackType rackType) {
        this.rackType = rackType;
    }

    public RackOfTubes getRackOfTubes() {
        return rackOfTubes;
    }

    public LabVessel getViewVessel() {
        if (rackOfTubes != null) {
            return rackOfTubes;
        } else if (staticPlate != null){
            return staticPlate;
        } else if (barcodedTube != null){
            return barcodedTube;
        }
        return null;
    }

    public String getContainerTypeDisplayName() {
        if (rackOfTubes != null) {
            return rackOfTubes.getRackType().getDisplayName();
        } else if (staticPlate != null){
            return staticPlate.getAutomationName();
        } else if (barcodedTube != null){
            return barcodedTube.getTubeType().getDisplayName();
        }
        return null;
    }

    public void setRackOfTubes(RackOfTubes rackOfTubes) {
        this.rackOfTubes = rackOfTubes;
    }

    public Map<VesselPosition, ?> getMapPositionToVessel() {
        return mapPositionToVessel;
    }

    public Map<VesselPosition, String> getMapPositionToSampleId() {
        return mapPositionToSampleId;
    }

    public List<LabVessel> getLooseTubes() {
        return looseTubes;
    }

    public Map<String, String> getMapBarcodeToSampleId() {
        return mapBarcodeToSampleId;
    }

    public boolean isEditLayout() {
        return editLayout;
    }

    public boolean isAjaxRequest() {
        return ajaxRequest;
    }

    public List<ReceptacleType> getReceptacleTypes() {
        return receptacleTypes;
    }

    public void setReceptacleTypes(
            List<ReceptacleType> receptacleTypes) {
        this.receptacleTypes = receptacleTypes;
    }

    public String getLocationTrail() {
        if (locationTrail == null && storageLocation != null) {
            locationTrail = storageLocationDao.getLocationTrail(storageLocation.getStorageLocationId());
        }
        return locationTrail;
    }

    public boolean viewReady() {
        return rackOfTubes != null || staticPlate != null;
    }

    public String getStorageId() {
        return storageId;
    }

    public void setStorageId(String storageId) {
        this.storageId = storageId;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    /**
     * Test only
     */
    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public boolean isShowLayout() {
        // Never for static plate
        if (staticPlate != null) {
            return false;
        }
        return showLayout;
    }

    public StaticPlate getStaticPlate() {
        return staticPlate;
    }

    public void setShowLayout(boolean showLayout) {
        this.showLayout = showLayout;
    }

    /**
     * This flag stops logic from over-writing layouts with the last checkin event layout
     * Used when confirming a new layout (after rack scan or manual barcode scanning)
     */
    public void setIgnoreCheckins(boolean ignoreCheckins) {
        this.ignoreCheckinEvents = ignoreCheckins;
    }
    public boolean getIgnoreCheckins() {
       return ignoreCheckinEvents;
    }

    /** For testing. **/
    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    /** For testing. **/
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /** For testing. **/
    public void setLabEventFactory(LabEventFactory labEventFactory) {
        this.labEventFactory = labEventFactory;
    }

    /** For testing. **/
    public void setLabEventDao(LabEventDao labEventDao) {
        this.labEventDao = labEventDao;
    }

    /** For testing. **/
    public void setStorageLocationDao(StorageLocationDao storageLocationDao) {
        this.storageLocationDao = storageLocationDao;
    }
}

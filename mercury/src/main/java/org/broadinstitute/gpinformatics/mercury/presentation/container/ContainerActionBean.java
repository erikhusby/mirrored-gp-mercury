package org.broadinstitute.gpinformatics.mercury.presentation.container;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
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
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.LabEventFactory;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;


/**
 * Create a Rack Of Tubes with a specified barcode to be used for Storage.
 */
@UrlBinding(ContainerActionBean.ACTION_BEAN_URL)
public class ContainerActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(ContainerActionBean.class);

    public static final String PAGE_TITLE = "View Container";
    public static final String ACTION_BEAN_URL = "/container/container.action";
    public static final String CONTAINER_CREATE_PAGE = "/container/create.jsp";
    public static final String CONTAINER_VIEW_PAGE = "/container/view.jsp";
    public static final String CONTAINER_VIEW_SHIM_PAGE = "/container/container_view.jsp";
    public static final String CREATE_CONTAINER_ACTION = "createContainer";
    public static final String VIEW_CONTAINER_ACTION = "viewContainer";
    public static final String VIEW_CONTAINER_AJAX_ACTION = "viewContainerAjax";
    public static final String CONTAINER_PARAMETER = "containerBarcode";
    public static final String SAVE_LOCATION_ACTION = "saveLocation";
    public static final String REMOVE_LOCATION_ACTION = "removeLocation";
    public static final String CANCEL_SAVE_ACTION = "cancel";
    public static final String FIRE_RACK_SCAN = "rackScan";

    private static final String CONTAINER = "Container";
    public static final String CREATE_CONTAINER = CoreActionBean.CREATE + CONTAINER;
    public static final String EDIT_CONTAINER = CoreActionBean.EDIT + CONTAINER;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private StorageLocationDao storageLocationDao;

    @Inject
    private TubeFormationDao tubeFormationDao;

    @Inject
    private LabEventFactory labEventFactory;

    @Inject
    private LabEventDao labEventDao;

    private String containerBarcode;
    private String storageName;

    private RackOfTubes.RackType rackType;
    private RackOfTubes rackOfTubes;
    private Map<VesselPosition, LabVessel> mapPositionToVessel;
    private boolean editLayout = false;
    private List<ReceptacleType> receptacleTypes;
    private StorageLocation storageLocation;
    private String locationTrail;
    private String storageId;
    private StaticPlate staticPlate;
    private boolean ajaxRequest;

    public ContainerActionBean() {
        super(CREATE_CONTAINER, EDIT_CONTAINER, CONTAINER_PARAMETER);
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    @DontValidate
    @HandlesEvent(CREATE_ACTION)
    public Resolution viewCreatePage() {
        return new ForwardResolution(CONTAINER_CREATE_PAGE);
    }

    @ValidationMethod(on = CREATE_CONTAINER_ACTION)
    public void labVesselDoesntExist(ValidationErrors errors) {
        if (labVesselDao.findByIdentifier(containerBarcode) != null) {
            errors.add(containerBarcode, new SimpleError("Barcode is already associated with another lab vessel"));
        }
    }

    @HandlesEvent(CREATE_CONTAINER_ACTION)
    public Resolution createContainer() {
        RackOfTubes rackOfTubes = new RackOfTubes(containerBarcode, rackType);
        labVesselDao.persist(rackOfTubes);
        labVesselDao.flush();
        addMessage("Successfully created new container: " + containerBarcode);
        return new RedirectResolution(ContainerActionBean.class, VIEW_CONTAINER_ACTION)
                .addParameter(CONTAINER_PARAMETER, containerBarcode);
    }

    @ValidationMethod(on = {VIEW_CONTAINER_ACTION, EDIT_ACTION, SAVE_ACTION, SAVE_LOCATION_ACTION,
            CANCEL_SAVE_ACTION, FIRE_RACK_SCAN, REMOVE_LOCATION_ACTION, VIEW_CONTAINER_AJAX_ACTION})
    public void labVesselExist() {
        if (StringUtils.isEmpty(containerBarcode)) {
            addValidationError(containerBarcode, "Container Barcode is required.");
        }
        LabVessel labVessel = labVesselDao.findByIdentifier(containerBarcode);
        if (labVessel == null) {
            addValidationError(containerBarcode, "Failed to find lab vessel: " + containerBarcode);
        } else if (OrmUtil.proxySafeIsInstance(labVessel, RackOfTubes.class)) {
            rackOfTubes = OrmUtil.proxySafeCast(labVessel, RackOfTubes.class);
            storageLocation = rackOfTubes.getStorageLocation();
        } else if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
            staticPlate = OrmUtil.proxySafeCast(labVessel, StaticPlate.class);
            storageLocation = staticPlate.getStorageLocation();
        } else {
            rackOfTubes = OrmUtil.proxySafeCast(labVessel, RackOfTubes.class);
            storageLocation = rackOfTubes.getStorageLocation();
            addValidationError(containerBarcode,
                    "Lab Vessel Type must be a Static Plate or a Rack Of Tubes: " + containerBarcode);
        }
    }

    /**
     * Attempt to determine the position map of the container under the disclaimer that the RackOfTubes
     */
    @After(stages = LifecycleStage.CustomValidation, on = {VIEW_CONTAINER_ACTION, EDIT_ACTION, SAVE_ACTION,
            SAVE_LOCATION_ACTION, CANCEL_SAVE_ACTION, REMOVE_LOCATION_ACTION, VIEW_CONTAINER_AJAX_ACTION})
    public void buildPositionMapping() {
        mapPositionToVessel = new HashMap<>();
        if (rackOfTubes == null && staticPlate == null) {
            addValidationError("containerBarcode", "Failed to find lab vessel: " + containerBarcode);
            return;
        }
        if (isStaticPlate()){
            buildStaticPlatePositionMapping();
            return;
        }
        SortedMap<Date, Pair<LabEvent, TubeFormation>> sortedTreeMap = new TreeMap<>();
        for (TubeFormation tubeFormation: rackOfTubes.getTubeFormations()) {
            Set<LabEvent> allEvents = tubeFormation.getEvents();
            for (LabEvent labEvent: allEvents) {
                LabVessel inPlaceLabVessel = labEvent.getInPlaceLabVessel();
                boolean foundRackOfTubesInEvent = false;
                if (inPlaceLabVessel != null) {
                    foundRackOfTubesInEvent = isRackOfTubesInEvent( labEvent, inPlaceLabVessel, rackOfTubes.getLabel() );
                }
                for (LabVessel vessel : labEvent.getTargetLabVessels()) {
                    if( OrmUtil.proxySafeIsInstance( vessel, TubeFormation.class )) {
                        foundRackOfTubesInEvent = isRackOfTubesInEvent( labEvent, vessel, rackOfTubes.getLabel() );
                    } else {
                        if (vessel.getLabel().equals(rackOfTubes.getLabel())) {
                            foundRackOfTubesInEvent = true;
                        }
                    }
                }
                if( !foundRackOfTubesInEvent && inPlaceLabVessel != null ) {
                    if( inPlaceLabVessel.getContainerRole() != null ) {
                        if (inPlaceLabVessel.getContainerRole().getEmbedder().getLabel().equals(rackOfTubes.getLabel())) {
                            foundRackOfTubesInEvent = true;
                        }
                    } else if (inPlaceLabVessel.getLabel().equals(rackOfTubes.getLabel())){
                        foundRackOfTubesInEvent = true;
                    }
                }
                if (foundRackOfTubesInEvent) {
                    sortedTreeMap.put(labEvent.getEventDate(), Pair.of(labEvent, tubeFormation));
                }
            }
        }
        if (!sortedTreeMap.isEmpty()) {
            Pair<LabEvent, TubeFormation> labEventTubeFormationPair = sortedTreeMap.get(sortedTreeMap.lastKey());
            LabEvent latestEvent = labEventTubeFormationPair.getLeft();
            TubeFormation tubeFormation = labEventTubeFormationPair.getRight();
            VesselContainer<?> containerRole = tubeFormation.getContainerRole();
            if (containerRole != null) {
                for (VesselPosition vesselPosition : rackOfTubes.getVesselGeometry().getVesselPositions()) {
                    LabVessel barcodedTube = containerRole.getImmutableVesselAtPosition(vesselPosition);
                    if (barcodedTube != null) {
                        LabEvent barcodesLatestEvent = barcodedTube.getLatestEvent();
                        if (barcodesLatestEvent.equals(latestEvent)) {
                            mapPositionToVessel.put(vesselPosition, barcodedTube);
                        }
                    }
                }
            }
        }
    }

    /**
     * If Static Plate has PlateWells already mapped such as in an Index Plate then use them, otherwise
     * just display the boxes.
     */
    private void buildStaticPlatePositionMapping() {
        VesselContainer<PlateWell> containerRole = staticPlate.getContainerRole();
        if (containerRole != null) {
            Map<VesselPosition, PlateWell> plateWellMap = containerRole.getMapPositionToVessel();
            for (Map.Entry<VesselPosition, PlateWell> entry : plateWellMap.entrySet()) {
                if (entry.getValue() != null) {
                    mapPositionToVessel.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    private boolean isStaticPlate() {
        return staticPlate != null;
    }

    private boolean isRackOfTubesInEvent( LabEvent labEvent, LabVessel vessel, String searchRackBarcode ){
        TubeFormation tubes = OrmUtil.proxySafeCast(vessel, TubeFormation.class);
        LabVessel rack = null;
        if( labEvent.getSectionTransfers().iterator().hasNext() ) {
            rack = labEvent.getSectionTransfers().iterator().next().getAncillaryTargetVessel();
        } else if ( labEvent.getCherryPickTransfers().iterator().hasNext() ) {
            rack = labEvent.getCherryPickTransfers().iterator().next().getAncillaryTargetVessel();
        } else if ( labEvent.getVesselToSectionTransfers().iterator().hasNext() ) {
            rack = labEvent.getVesselToSectionTransfers().iterator().next().getAncillaryTargetVessel();
        }
        if( rack != null ) {
            return rack.getLabel().equals(searchRackBarcode);
        } else {
            // Ancillary vessel logic was added around Aug 2014.  This handles any earlier cases
            for ( LabVessel oldLogicRack : tubes.getRacksOfTubes()) {
                if (oldLogicRack.getLabel().equals(searchRackBarcode)) {
                    return true;
                }
            }
        }
        return false;
    }

    @DefaultHandler
    @DontValidate
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_ACTION)
    public Resolution viewContainer() {
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_AJAX_ACTION)
    public Resolution viewContainerAjax() {
//        editLayout = true;
        ajaxRequest = true;
        return new ForwardResolution(CONTAINER_VIEW_SHIM_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution editContainer() {
        editLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    private void savePositionMapToLocation(MessageCollection messageCollection, StorageLocation storageLocation) {
        for (ReceptacleType receptacleType : receptacleTypes) {
            if (!StringUtils.isEmpty(receptacleType.getBarcode())) {
                String barcode = receptacleType.getBarcode();
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    messageCollection.addError("Unrecognized barcode: " + barcode);
                } else if (!OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                    messageCollection.addError("Barcode isn't a tube type: " + barcode);
                } else {
                    BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                    barcodedTube.setStorageLocation(storageLocation);
                }
            }
        }
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution saveContainer() {
        // Save all new lab vessels to parents storage location
        if (receptacleTypes == null) {
            addValidationError("containerBarcode", "No tube barcodes found.");
            return new ForwardResolution(CONTAINER_VIEW_PAGE);
        }
        if (storageLocation != null) {
            MessageCollection messageCollection = new MessageCollection();
            savePositionMapToLocation(messageCollection, storageLocation);
            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
                return new ForwardResolution(CONTAINER_VIEW_PAGE);
            }
        }

        for (Iterator<ReceptacleType> iterator = receptacleTypes.iterator(); iterator.hasNext();) {
            ReceptacleType receptacleType = iterator.next();
            if (StringUtils.isEmpty(receptacleType.getBarcode())) {
                iterator.remove();
            }
        }

        PlateEventType plateEventType = buildPlateEvent(LabEventType.STORAGE_CHECK_IN);
        LabEvent labEvent = labEventFactory.buildFromBettaLims(plateEventType);
        LabVessel inPlaceLabVessel = labEvent.getInPlaceLabVessel();
        if (inPlaceLabVessel != null) {
            if (OrmUtil.proxySafeIsInstance(inPlaceLabVessel, TubeFormation.class)) {
                TubeFormation tubeFormation = OrmUtil.proxySafeCast(inPlaceLabVessel, TubeFormation.class);
                tubeFormation.addRackOfTubes(rackOfTubes);
            }
        }
        rackOfTubes.setStorageLocation(storageLocation);
        labEventDao.persist(labEvent);
        labEventDao.flush();
        return new RedirectResolution(ContainerActionBean.class, VIEW_CONTAINER_ACTION)
                .addParameter(CONTAINER_PARAMETER, containerBarcode);
    }

    @HandlesEvent(FIRE_RACK_SCAN)
    public Resolution fireRackScan() throws ScannerException {
        scan();
        MessageCollection messageCollection = new MessageCollection();
        Map<VesselPosition, LabVessel> scanPositionToVessel = new HashMap<>();
        if(getRackScan() != null) {
            List<String> barcodes = new ArrayList<>(rackScan.values());
            Map<String, LabVessel> mapBarcodeToVessel = labVesselDao.findByBarcodes(barcodes);
            for (Map.Entry<String, String> entry: rackScan.entrySet()) {
                String position = entry.getKey();
                String barcode = entry.getValue();
                LabVessel labVessel = mapBarcodeToVessel.get(barcode);
                if (labVessel == null) {
                    messageCollection.addError("Unrecognized tube barcode: " + barcode);
                } else {
                    VesselPosition vesselPosition = VesselPosition.getByName(position);
                    if (vesselPosition == null) {
                        messageCollection.addError("Unrecognized position: " + position);
                    } else {
                        scanPositionToVessel.put(vesselPosition, labVessel);
                    }
                }
            }
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        } else {
            mapPositionToVessel = scanPositionToVessel;
        }
        editLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE)
                .addParameter(CONTAINER_PARAMETER, containerBarcode)
                .addParameter(VIEW_ACTION, "");
    }

    private PlateEventType buildPlateEvent(LabEventType eventType) {
        PlateEventType plateEventType = new PlateEventType();
        plateEventType.setEventType(eventType.getName());
        plateEventType.setStart(new Date());
        plateEventType.setDisambiguator(1L);
        plateEventType.setOperator(getUserBean().getLoginUserName());
        plateEventType.setProgram(LabEvent.UI_PROGRAM_NAME);
        plateEventType.setStation(LabEvent.UI_PROGRAM_NAME);
        PositionMapType positionMapType = new PositionMapType();
        positionMapType.getReceptacle().addAll(receptacleTypes);
        plateEventType.setPositionMap(positionMapType);
        positionMapType.setBarcode(rackOfTubes.getLabel());
        PlateType plateType = new PlateType();
        plateEventType.setPlate(plateType);
        plateType.setBarcode(rackOfTubes.getLabel());
        plateType.setPhysType(rackOfTubes.getRackType().getDisplayName());
        plateType.setSection("ALL96");
        return plateEventType;
    }

    @HandlesEvent(CANCEL_SAVE_ACTION)
    public Resolution cancelSave() {
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(SAVE_LOCATION_ACTION)
    public Resolution saveToLocation() {
        if (!StringUtils.isEmpty(storageId)) {
            storageLocation = storageLocationDao.findById(StorageLocation.class, Long.valueOf(storageId));
            MessageCollection messageCollection = new MessageCollection();
            getViewVessel().setStorageLocation(storageLocation);
            savePositionMapToLocation(messageCollection, storageLocation);
            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
                return new ForwardResolution(CONTAINER_VIEW_PAGE);
            }
            storageLocationDao.persist(getViewVessel());
            storageLocationDao.flush();
            addMessage("Successfully added to storage.");
        } else {
            addValidationError("storageName","Storage Location Required.");
        }
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(REMOVE_LOCATION_ACTION)
    public Resolution removeFromLocation() {
        MessageCollection messageCollection = new MessageCollection();
        if (storageLocation != null) {
            getViewVessel().setStorageLocation(null);
            savePositionMapToLocation(messageCollection, null);
            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
            } else {
                storageLocationDao.persist(getViewVessel());
                storageLocationDao.flush();
                storageLocation = null;
            }
        } else {
            addValidationError("storageName","Lab Vessel not in storage.");
        }

        return new RedirectResolution(ContainerActionBean.ACTION_BEAN_URL)
                .addParameter(CONTAINER_PARAMETER, containerBarcode)
                .addParameter(VIEW_CONTAINER_ACTION, "");
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
        }
        return null;
    }

    public String getContainerTypeDisplayName() {
        if (rackOfTubes != null) {
            return rackOfTubes.getRackType().getDisplayName();
        } else if (staticPlate != null){
            return staticPlate.getAutomationName();
        }
        return null;
    }

    public void setRackOfTubes(RackOfTubes rackOfTubes) {
        this.rackOfTubes = rackOfTubes;
    }

    public Map<VesselPosition, ?> getMapPositionToVessel() {
        return mapPositionToVessel;
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
            locationTrail = storageLocation.buildLocationTrail();
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

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
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

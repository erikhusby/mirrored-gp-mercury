package org.broadinstitute.gpinformatics.mercury.presentation.container;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.DontValidate;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
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
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
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
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
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
    public static final String VIEW_CONTAINER_SEARCH_ACTION = "viewContainerSearch";
    public static final String VIEW_CONTAINER_AJAX_ACTION = "viewContainerAjax";
    public static final String CONTAINER_PARAMETER = "containerBarcode";
    public static final String SHOW_LAYOUT_PARAMETER = "showLayout";
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

    @Inject
    private BSPRestSender bspRestSender;

    private String containerBarcode;
    private String storageName;

    private RackOfTubes.RackType rackType;
    private RackOfTubes rackOfTubes;
    private Map<VesselPosition, LabVessel> mapPositionToVessel;
    private Map<VesselPosition, String> mapPositionToSampleId;
    private boolean editLayout = false;
    private List<ReceptacleType> receptacleTypes;
    private StorageLocation storageLocation;
    private String locationTrail;
    private String storageId;
    private StaticPlate staticPlate;
    private boolean ajaxRequest;
    private boolean showLayout;
    private boolean inBsp;
    private String receptacleType;

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
        if (StringUtils.isEmpty(containerBarcode) && !inBsp) {
            addValidationError("containerBarcode", "Container Barcode is required.");
            return;
        }
        if (labVesselDao.findByIdentifier(containerBarcode) != null  && !inBsp) {
            errors.add(containerBarcode, new SimpleError("Barcode is already associated with another lab vessel"));
        }

        if (inBsp && StringUtils.isEmpty(receptacleType)) {
            errors.add(receptacleType, new SimpleError("Receptacle Type is required for BSP"));
        }
    }

    @HandlesEvent(CREATE_CONTAINER_ACTION)
    public Resolution createContainer() {
        if (inBsp) {
            containerBarcode = bspRestSender.registerEmptyPlate(rackType.getDisplayName(), "");
            if (containerBarcode == null) {
                throw new RuntimeException("Failed to create container barcode in BSP");
            }
        }

        RackOfTubes rackOfTubes = new RackOfTubes(containerBarcode, rackType);
        labVesselDao.persist(rackOfTubes);
        labVesselDao.flush();

        if (!inBsp) {
            addMessage("Successfully created new container: " + containerBarcode);
            return new RedirectResolution(ContainerActionBean.class, VIEW_CONTAINER_ACTION)
                    .addParameter(CONTAINER_PARAMETER, containerBarcode);
        } else {
            try {
                ObjectMapper mapper = new ObjectMapper();
                ObjectNode objectNode = mapper.createObjectNode();
                objectNode.put("containerBarcode", containerBarcode);
                return new StreamingResolution("application/json", new StringReader(mapper.writeValueAsString(objectNode)));
            } catch (IOException e) {
                logger.error("Failed to generate json", e);
                throw new RuntimeException("Failed to generate JSON");
            }
        }
    }

    @ValidationMethod(on = {VIEW_CONTAINER_ACTION, EDIT_ACTION, SAVE_ACTION, SAVE_LOCATION_ACTION,
            CANCEL_SAVE_ACTION, FIRE_RACK_SCAN, REMOVE_LOCATION_ACTION, VIEW_CONTAINER_SEARCH_ACTION})
    public void labVesselExist() {
        if (StringUtils.isEmpty(containerBarcode)) {
            addValidationError("containerBarcode", "Container Barcode is required.");
            return;
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
            addValidationError(containerBarcode,
                    "Lab Vessel Type must be a Static Plate or a Rack Of Tubes: " + containerBarcode);
        }
    }

    /**
     * Attempt to determine the position map of the container under the disclaimer that the RackOfTubes
     * may not be accurate under rearrays.
     */
    @After(stages = LifecycleStage.CustomValidation, on = {VIEW_CONTAINER_ACTION, EDIT_ACTION, SAVE_ACTION,
            SAVE_LOCATION_ACTION, CANCEL_SAVE_ACTION, REMOVE_LOCATION_ACTION, VIEW_CONTAINER_SEARCH_ACTION})
    public void buildPositionMapping() {
        mapPositionToVessel = new HashMap<>();
        mapPositionToSampleId = new HashMap<>();
        if (rackOfTubes == null && staticPlate == null) {
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
                for (LabVessel vessel : labEvent.getSourceLabVessels()) {
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
            TreeMap<Date, Pair<LabEvent, TubeFormation>> sortedDateDesc =
                    new TreeMap<>(Collections.reverseOrder());
            sortedDateDesc.putAll(sortedTreeMap);
            Pair<LabEvent, TubeFormation> labEventTubeFormationPair = null;
            for (Map.Entry<Date, Pair<LabEvent, TubeFormation>> entry: sortedDateDesc.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getLeft() != null &&
                    entry.getValue().getLeft().getLabEventType() == LabEventType.STORAGE_CHECK_IN ||
                    entry.getValue().getLeft().getLabEventType() == LabEventType.STORAGE_CHECK_OUT ) {
                    labEventTubeFormationPair = entry.getValue();
                    break;
                }
            }
            if (labEventTubeFormationPair == null) {
                return;
            }

            LabEvent latestEvent = labEventTubeFormationPair.getLeft();
            TubeFormation tubeFormation = labEventTubeFormationPair.getRight();
            VesselContainer<?> containerRole = tubeFormation.getContainerRole();
            if (containerRole != null) {
                for (VesselPosition vesselPosition : rackOfTubes.getVesselGeometry().getVesselPositions()) {
                    LabVessel barcodedTube = containerRole.getImmutableVesselAtPosition(vesselPosition);
                    if (barcodedTube != null) {
                        if (rackOfTubes.getStorageLocation() == null && barcodedTube.getStorageLocation() != null) {
                            continue;
                        } else if (rackOfTubes.getStorageLocation() == null && barcodedTube.getStorageLocation() != null) {
                            continue;
                        }
                        if (rackOfTubes.getStorageLocation() != null && barcodedTube.getStorageLocation() == null) {
                            continue;
                        }
                        if (rackOfTubes.getStorageLocation() != null && barcodedTube.getStorageLocation() != null
                                && !barcodedTube.getStorageLocation().equals(rackOfTubes.getStorageLocation())) {
                            continue;
                        }
                        LabEvent barcodesLatestEvent = barcodedTube.getLatestStorageEvent();
                        if (barcodesLatestEvent != null && barcodesLatestEvent.equals(latestEvent)) {
                            mapPositionToVessel.put(vesselPosition, barcodedTube);
                            Set<MercurySample> samples = barcodedTube.getMercurySamples();
                            String display = "";
                            if( samples.size() == 1 ) {
                                display = samples.iterator().next().getSampleKey();
                            } else if( samples.size() > 1 ) {
                                // Not sure if this should ever happen - but code for it
                                StringBuilder b = new StringBuilder();
                                for( MercurySample sample : samples ) {
                                    b.append(sample.getSampleKey()).append(" ");
                                }
                                display = b.toString().trim();
                            }
                            mapPositionToSampleId.put( vesselPosition, display );
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
        if (rackOfTubes != null && rackOfTubes.getRackType().isRackScannable()) {
            return new RedirectResolution(ContainerActionBean.class, EDIT_ACTION)
                    .addParameter(CONTAINER_PARAMETER, containerBarcode)
                    .addParameter(SHOW_LAYOUT_PARAMETER, false);
        }
        showLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_SEARCH_ACTION)
    public Resolution viewContainerSearch() {
        showLayout = true;
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    @HandlesEvent(VIEW_CONTAINER_AJAX_ACTION)
    public Resolution viewContainerAjax() {
        ajaxRequest = true;
        showLayout = true;
        labVesselExist();
        buildPositionMapping();
        if (hasErrors()) {
            throw new RuntimeException("Failed to find lab vessel: " + containerBarcode);
        }
        return new ForwardResolution(CONTAINER_VIEW_SHIM_PAGE);
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution editContainer() {
        editLayout = true;
        if (rackOfTubes != null && !rackOfTubes.getRackType().isRackScannable()) {
            showLayout = true;
        }
        if (storageLocation != null) {
            addMessage("After updating the container layout you will need to add it back to storage.");
        }
        return new ForwardResolution(CONTAINER_VIEW_PAGE);
    }

    private List<BarcodedTube> savePositionMapToLocation(MessageCollection messageCollection) {
        Set<String> barcodes = new HashSet<>();
        if (rackOfTubes == null) {
            return null;
        }
        if (receptacleTypes == null) {
            messageCollection.addError("You must scan the rack.");
            return null;
        }
        List<BarcodedTube> tubesToAddToStorage = new ArrayList<>();
        for (ReceptacleType receptacleType : receptacleTypes) {
            if (!StringUtils.isEmpty(receptacleType.getBarcode())) {
                String barcode = receptacleType.getBarcode();
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                if (labVessel == null) {
                    messageCollection.addError("Unrecognized barcode: " + barcode);
                } else if (!OrmUtil.proxySafeIsInstance(labVessel, BarcodedTube.class)) {
                    messageCollection.addError("Barcode isn't a tube type: " + barcode);
                } else if (barcodes.contains(barcode)) {
                    messageCollection.addError("Duplicate tube barcode found: " + barcode);
                } else {
                    BarcodedTube barcodedTube = OrmUtil.proxySafeCast(labVessel, BarcodedTube.class);
                    tubesToAddToStorage.add(barcodedTube);
                    barcodes.add(barcode);
                }
            }
        }

        // Remove any tubes that are no longer in this storage location
        for (LabVessel labVessel: mapPositionToVessel.values()) {
            if (!barcodes.contains(labVessel.getLabel())) {
                labVessel.setStorageLocation(null);
            }
        }
        return tubesToAddToStorage;
    }

    @HandlesEvent(SAVE_ACTION)
    public Resolution saveContainer() {
        MessageCollection messageCollection = new MessageCollection();
        handleSaveContainer(messageCollection);
        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
            showLayout = true;
            editLayout = true;
            return new ForwardResolution(CONTAINER_VIEW_PAGE);
        } else if (messageCollection.hasWarnings()) {
            addMessages(messageCollection);
        }
        addMessage("Successfully updated layout.");
        addMessage("Please select storage location.");
        return new RedirectResolution(ContainerActionBean.class, VIEW_CONTAINER_SEARCH_ACTION)
                .addParameter(CONTAINER_PARAMETER, containerBarcode)
                .addParameter(SHOW_LAYOUT_PARAMETER, true);
    }

    public void handleSaveContainer(MessageCollection messageCollection) {
        // Save all new lab vessels to parents storage location
        if (receptacleTypes == null) {
            messageCollection.addError("No tube barcodes found.");
            return;
        }
        storageLocation = null;
        List<BarcodedTube> barcodedTubesToAddToStorage = null;
        Set<String> barcodes = new HashSet<>();
        for (ReceptacleType receptacleType : receptacleTypes) {
            if (!StringUtils.isEmpty(receptacleType.getBarcode())) {
                String barcode = receptacleType.getBarcode();
                LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
                labVessel.setStorageLocation(null);
                barcodes.add(barcode);
            }
        }

        if (messageCollection.hasErrors()) {
            return;
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
        if (barcodedTubesToAddToStorage != null) {
            for (BarcodedTube barcodedTube : barcodedTubesToAddToStorage) {
                barcodedTube.setStorageLocation(storageLocation);
            }
        }

        // Remove any tubes that are no longer in this storage location
        // Sort to display orphans warnings in order
        List<Map.Entry<VesselPosition, LabVessel>> entries = new ArrayList<>(mapPositionToVessel.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<VesselPosition, LabVessel>>() {
            @Override
            public int compare(Map.Entry<VesselPosition, LabVessel> o1, Map.Entry<VesselPosition, LabVessel> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (Map.Entry<VesselPosition, LabVessel> entry: entries) {
            LabVessel labVessel = entry.getValue();
            if (!barcodes.contains(labVessel.getLabel())) {
                VesselPosition vesselPosition = entry.getKey();
                labVessel.setStorageLocation(null);
                messageCollection.addWarning("Lab Vessel " + labVessel.getLabel() + " was orphaned from position " + vesselPosition.name());
            }
        }
        showLayout = true;
        labEventDao.persist(labEvent);
        labEventDao.flush();
    }

    @HandlesEvent(FIRE_RACK_SCAN)
    public Resolution fireRackScan() throws ScannerException {
        scan();
        boolean rackScanEmpty = true;
        MessageCollection messageCollection = new MessageCollection();
        Map<VesselPosition, LabVessel> scanPositionToVessel = new HashMap<>();
        if(getRackScan() != null) {
            if (rackScan == null || rackScan.isEmpty()) {
                messageCollection.addError("No results from rack scan");
            } else {
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
            }
        }

        if (rackScanEmpty) {
            messageCollection.addError("No results from rack scan");
        }

        if (messageCollection.hasErrors()) {
            showLayout = false;
            addMessages(messageCollection);
        } else {
            showLayout = true;
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
            List<BarcodedTube> barcodedTubes = savePositionMapToLocation(messageCollection);
            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
                return new ForwardResolution(CONTAINER_VIEW_PAGE);
            }
            if (barcodedTubes != null) {
                for (BarcodedTube barcodedTube : barcodedTubes) {
                    barcodedTube.setStorageLocation(storageLocation);
                }
            }
            storageLocationDao.persist(getViewVessel());
            storageLocationDao.flush();
            showLayout = true;
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
            List<BarcodedTube> barcodedTubes = savePositionMapToLocation(messageCollection);
            if (messageCollection.hasErrors()) {
                addMessages(messageCollection);
            } else {
                if (barcodedTubes != null) {
                    for (BarcodedTube barcodedTube : barcodedTubes) {
                        barcodedTube.setStorageLocation(null);
                    }
                }
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

    public Map<VesselPosition, String> getMapPositionToSampleId() {
        return mapPositionToSampleId;
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

    public boolean isShowLayout() {
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

    public boolean isInBsp() {
        return inBsp;
    }

    public void setInBsp(boolean inBsp) {
        this.inBsp = inBsp;
    }

    public String getReceptacleType() {
        return receptacleType;
    }

    public void setReceptacleType(String receptacleType) {
        this.receptacleType = receptacleType;
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

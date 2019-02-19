package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.After;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.security.Role;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(StorageLocationActionBean.ACTION_BEAN_URL)
public class StorageLocationActionBean extends CoreActionBean {
    private static final Log logger = LogFactory.getLog(StorageLocationActionBean.class);
    public static final String ACTION_BEAN_URL = "/storage/storage.action";
    public static final String CREATE_STORAGE = "Create Storage Location";
    public static final String STORAGE_LIST_PAGE = "/storage/list_storage.jsp";
    public static final String CREATE_STORAGE_PAGE = "/storage/create_storage.jsp";
    public static final String EDIT_STORAGE_PAGE = "/storage/edit_storage.jsp";
    public static final String STORAGE_ID_PARAM = "storageId";
    public static final String LOAD_TREE_ACTION = "loadTree";
    public static final String LOAD_TREE_AJAX_ACTION = "loadTreeAjax";
    public static final String SEARCH_NODE_ACTION = "searchNode";
    public static final String MOVE_NODE_ACTION = "moveNodeAction";
    public static final String RENAME_NODE_ACTION = "renameNodeAction";
    public static final String SAVE_BARCODES_ACTION = "saveStorageBarcodes";
    public static final String FIND_LOCATION_TRAIL_ACTION = "findLocationTrail";
    public static final String ROOT_NODE = "#";

    @Inject
    private StorageLocationDao storageLocationDao;

    // Test, via setter
    private LabVesselDao labVesselDao;

    private MessageCollection messageCollection = new MessageCollection();
    private StorageLocation.LocationType locationType;
    private String searchKey;
    private String name;
    private long storageId;
    private String storageName;
    private String storageJson;
    private String locationTrailString = "";
    private VesselGeometry vesselGeometry;
    private StorageLocation storageLocation;

    // Move/Rename Action Data Types
    private String nodeName;
    private String newParentName;
    private String id;
    private String oldName;

    //Node Search
    private String searchTerm;

    private List<StorageLocation> childStorageLocations;
    private Map<Long, String> mapIdToBarcode = new HashMap<>();
    private Map<Long, StorageLocation> mapIdToStorageLocation = new LinkedHashMap<>();

    public StorageLocationActionBean() {
        super(CREATE_STORAGE, null, null);
    }

    @DefaultHandler
    @HandlesEvent(LIST_ACTION)
    public Resolution list() {
        return new ForwardResolution(STORAGE_LIST_PAGE);
    }

    /**
     * Children nodes loaded on demand
     */
    @HandlesEvent(LOAD_TREE_AJAX_ACTION)
    public Resolution loadTreeAjax() throws Exception {
        List<StorageLocation> childNodeStorageLocations;
        StorageLocation parentNodeStorageLocation = null;
        if (id.equals(ROOT_NODE)) {
            childNodeStorageLocations = storageLocationDao.findByLocationTypes(
                    StorageLocation.LocationType.getTopLevelLocationTypes());
        } else {
            long parentId = Long.parseLong(id);
            parentNodeStorageLocation = storageLocationDao.findById(StorageLocation.class, parentId);
            childNodeStorageLocations = new ArrayList<>(parentNodeStorageLocation.getChildrenStorageLocation());
        }
        Collections.sort(childNodeStorageLocations, new StorageLocation.StorageLocationLabelComparator());
        String storageJson = generateJsonFromRoots(parentNodeStorageLocation, childNodeStorageLocations, true);
        return new StreamingResolution("text", new StringReader(storageJson));
    }

    @HandlesEvent(LOAD_TREE_ACTION)
    public Resolution loadTree() throws Exception {
        List<StorageLocation> rootStorageLocations = storageLocationDao.findByLocationTypes(
                StorageLocation.LocationType.getTopLevelLocationTypes());
        String storageJson = generateJsonFromRoots(null, rootStorageLocations, false);
        return new StreamingResolution("text", new StringReader(storageJson));
    }

    @HandlesEvent(SEARCH_NODE_ACTION)
    public Resolution searchForNode() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode currentNode = new ObjectMapper().createObjectNode();
        StorageLocation storageLocation = null;
        if (searchTerm != null) {
            storageLocation = storageLocationDao.findByBarcode(searchTerm);
            if (storageLocation != null) {
                generateJSON(storageLocation, currentNode, true);
                createOpenState(mapper, currentNode, false, true);
                currentNode.put("id", "selected_node");
                while (storageLocation.getParentStorageLocation() != null) {
                    StorageLocation parentLocation = storageLocation.getParentStorageLocation();
                    ObjectNode parentNode = mapper.createObjectNode();
                    generateJSON(parentLocation, parentNode, true);
                    createOpenState(mapper, parentNode, true, false);
                    ArrayNode arrayNode = mapper.createArrayNode();
                    Set<StorageLocation> childrenStorageLocation = parentLocation.getChildrenStorageLocation();
                    List<StorageLocation> childrenStorageLocationList = new ArrayList<>(childrenStorageLocation);
                    Collections.sort(childrenStorageLocationList, new StorageLocation.StorageLocationLabelComparator());
                    for (StorageLocation childLocation: childrenStorageLocationList) {
                        if (childLocation.getStorageLocationId().equals(storageLocation.getStorageLocationId())) {
                            arrayNode.add(currentNode);
                        } else {
                            arrayNode.add(generateJSON(childLocation, mapper.createObjectNode(), true));
                        }
                    }
                    parentNode.put("children", arrayNode);
                    currentNode = parentNode;
                    storageLocation = parentLocation;
                }
            }
        }

        ArrayNode arrayNode = mapper.createArrayNode();
        if (storageLocation == null) {
            throw new RuntimeException("Failed to find storage location with barcode: " + storageId);
        } else {
            //Add all other top level locations as well
            List<StorageLocation> rootStorageLocations = storageLocationDao.findByLocationTypes(
                    StorageLocation.LocationType.getTopLevelLocationTypes());
            Collections.sort(rootStorageLocations, new StorageLocation.StorageLocationLabelComparator());
            for (StorageLocation rootStorageLocation : rootStorageLocations) {
                ObjectNode rootNode = null;
                if (rootStorageLocation.getStorageLocationId().equals(storageLocation.getStorageLocationId())) {
                    rootNode = currentNode;
                } else {
                    rootNode = generateJSON(rootStorageLocation, mapper.createObjectNode(), true);
                }
                arrayNode.add(rootNode);
            }
        }

        return new StreamingResolution("application/json", new StringReader(mapper.writeValueAsString(arrayNode)));
    }

    public void createOpenState(ObjectMapper mapper, ObjectNode parentNode, boolean opened, boolean selected) {
        ObjectNode currentNodeState = mapper.createObjectNode();
        currentNodeState.put("opened", opened);
        currentNodeState.put("selected", selected);
        parentNode.put("state", currentNodeState);
    }

    @HandlesEvent(RENAME_NODE_ACTION)
    public Resolution renameNode() throws JSONException {
        JSONObject retObj = new JSONObject();
        try {
            if (StringUtils.isEmpty(nodeName)) {
                retObj.put("error", "Location to rename not specified");
                retObj.put("hasError", true);
            } else if (StringUtils.isEmpty(storageName)) {
                retObj.put("error", "Storage Name is required.");
                retObj.put("hasError", true);
            } else {
                long nodeId = Long.parseLong(nodeName);
                StorageLocation location = storageLocationDao.findById(StorageLocation.class, nodeId);
                if (!location.getLocationType().canRename()) {
                    retObj.put("error", "Cannot rename location of this type: " + location.getLocationType().getDisplayName());
                    retObj.put("hasError", true);
                } else {
                    location.setLabel(storageName);
                    storageLocationDao.persist(location);
                    retObj.put("hasError", false);
                }
            }
            return new StreamingResolution("text", new StringReader(retObj.toString()));
        } catch (Exception e) {
            logger.error("Error occured when attempting to move location", e);
            retObj.put("error", e.getMessage());
            retObj.put("hasError", true);
            return new StreamingResolution("text", new StringReader(retObj.toString()));
        }
    }

    @HandlesEvent(MOVE_NODE_ACTION)
    public Resolution moveNode() throws JSONException {
        JSONObject retObj = new JSONObject();
        try {
            if (StringUtils.isEmpty(nodeName)) {
                retObj.put("error", "Location to move not specified");
                retObj.put("hasError", true);
            } else if (StringUtils.isEmpty(newParentName)) {
                retObj.put("error", "New Location Not Specified");
                retObj.put("hasError", true);
            } else {
                long nodeId = Long.parseLong(nodeName);
                long nodeParentId = Long.valueOf(newParentName);
                StorageLocation location = storageLocationDao.findById(StorageLocation.class, nodeId);
                StorageLocation newParent = storageLocationDao.findById(StorageLocation.class, nodeParentId);
                if (!location.getLocationType().isMoveable()) {
                    retObj.put("error", "Cannot move location of this type: " + location.getLocationType().getDisplayName());
                    retObj.put("hasError", true);
                } else {
                    location.setParentStorageLocation(newParent);
                    storageLocationDao.persist(location);
                    retObj.put("hasError", false);
                }
            }
            return new StreamingResolution("text", new StringReader(retObj.toString()));
        } catch (Exception e) {
            logger.error("Error occured when attempting to move location", e);
            retObj.put("error", e.getMessage());
            retObj.put("hasError", true);
            return new StreamingResolution("text", new StringReader(retObj.toString()));
        }
    }

    @HandlesEvent(FIND_LOCATION_TRAIL_ACTION)
    public Resolution findLocationTrail() throws IOException {
        locationTrailString = "";
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        try {
            String storageParam = getContext().getRequest().getParameter("storageId");
            if (StringUtils.isEmpty(storageParam)) {
                objectNode.put("hasErrors", true);
                objectNode.put("errors", "storage ID is a required parameter.");
            } else {
                storageId = Long.parseLong(storageParam);
                storageLocation = storageLocationDao.findById(StorageLocation.class, storageId);
                if (storageLocation == null) {
                    objectNode.put("hasErrors", true);
                    objectNode.put("errors", "Failed to find location trail for storage id " + storageId);
                } else {
                    locationTrailString = storageLocationDao.getLocationTrail( storageLocation.getStorageLocationId());
                    if (StringUtils.isEmpty(locationTrailString)) {
                        objectNode.put("hasErrors", true);
                        objectNode.put("errors", "Failed to find location trail for storage id " + storageId);
                    } else {
                        objectNode.put("hasErrors", false);
                        objectNode.put("locationTrail", locationTrailString);
                    }
                }
            }
        } catch (Exception e) {
            objectNode.put("hasErrors", true);
            objectNode.put("errors", "Error finding location trail.");
        }
        return new StreamingResolution("application/json", mapper.writeValueAsString(objectNode));
    }

    @HandlesEvent(CREATE_ACTION)
    public Resolution createStorage() {
        return new ForwardResolution(CREATE_STORAGE_PAGE);
    }


    @After(stages = LifecycleStage.BindingAndValidation, on = {EDIT_ACTION, SAVE_BARCODES_ACTION})
    public void editInit() {
        storageLocation = storageLocationDao.findById(StorageLocation.class, storageId);
        childStorageLocations = new ArrayList<>(storageLocation.getChildrenStorageLocation());
        Collections.sort(childStorageLocations, new StorageLocation.StorageLocationLabelComparator());
        for (StorageLocation storageLocation: childStorageLocations) {
            mapIdToStorageLocation.put(storageLocation.getStorageLocationId(), storageLocation);
        }
    }

    @HandlesEvent(EDIT_ACTION)
    public Resolution editStorage() {
        if (storageLocation == null) {
            addValidationError(storageName, "Failed to find storage location with id " + storageId);
            return new RedirectResolution(LOAD_TREE_AJAX_ACTION);
        } else if (storageLocation.getLocationType() != StorageLocation.LocationType.GAUGERACK) {
            addValidationError(storageName, "Can only edit Gauge Racks.");
            return new RedirectResolution(LOAD_TREE_AJAX_ACTION);
        }
        return new ForwardResolution(EDIT_STORAGE_PAGE);
    }

    @HandlesEvent(SAVE_BARCODES_ACTION)
    public Resolution saveBarcodesAction() {
        Set<String> uniqueBarcodes = new HashSet<>();
        if (mapIdToStorageLocation != null) {
            for (Map.Entry<Long, StorageLocation> entry : mapIdToStorageLocation.entrySet()) {
                StorageLocation storageLocation = entry.getValue();
                String barcode = storageLocation.getBarcode();
                if (!StringUtils.isEmpty(barcode)) {
                    if (!uniqueBarcodes.add(barcode)) {
                        addGlobalValidationError("All barcodes must be unique.");
                        return new ForwardResolution(EDIT_STORAGE_PAGE).addParameter(STORAGE_ID_PARAM, storageId);
                    }
                }
            }
        }

        List<StorageLocation> storageLocationList = storageLocationDao.findByListBarcodes(
                new ArrayList<>(uniqueBarcodes));

        for (StorageLocation storageLocation: storageLocationList) {
            if (!mapIdToStorageLocation.containsKey(storageLocation.getStorageLocationId())) {
                messageCollection.addError("Barcode is already in use: " + storageLocation.getBarcode());
            }
        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
            return new ForwardResolution(EDIT_STORAGE_PAGE).addParameter(STORAGE_ID_PARAM, storageId);
        }

        storageLocationDao.persistAll(mapIdToStorageLocation.values());
        addMessage("Successfully updated barcodes.");
        return new RedirectResolution(StorageLocationActionBean.class, EDIT_ACTION)
                .addParameter(STORAGE_ID_PARAM, storageId);
    }

    @Override
    public boolean isCreateAllowed() {
        Collection<Role> roles = getUserBean().getRoles();
        return roles.contains(Role.LabManager) || roles.contains(Role.Developer);
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getOldName() {
        return oldName;
    }

    public void setOldName(String oldName) {
        this.oldName = oldName;
    }

    public String getNewParentName() {
        return newParentName;
    }

    public void setNewParentName(String newParentName) {
        this.newParentName = newParentName;
    }

    public String generateJsonFromRoots(StorageLocation parentStorageLocation, List<StorageLocation> childStorageLocations,
                                        boolean ajax) throws IOException {
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode root = mapper.createObjectNode();
        ArrayNode arrayNode = mapper.createArrayNode();
        for (StorageLocation storageLocation: childStorageLocations) {

            ObjectNode rootNode = generateJSON(storageLocation, mapper.createObjectNode(), ajax);
            arrayNode.add(rootNode);
        }
        if (parentStorageLocation != null) {
            if (!parentStorageLocation.getLabVessels().isEmpty()) {
                for (LabVessel labVessel : parentStorageLocation.getLabVessels()) {
                    if (OrmUtil.proxySafeIsInstance(labVessel, RackOfTubes.class)) {
                        ObjectNode objectNode = root.objectNode();
                        objectNode.put("text", labVessel.getLabel());
                        objectNode.put("type", RackOfTubes.class.getSimpleName());
                        arrayNode.add(objectNode);
                    } else if (OrmUtil.proxySafeIsInstance(labVessel, StaticPlate.class)) {
                        ObjectNode objectNode = root.objectNode();
                        objectNode.put("text", labVessel.getLabel());
                        objectNode.put("type", StaticPlate.class.getSimpleName());
                        arrayNode.add(objectNode);
                    }
                }
            }
        }
        root.put("children", arrayNode);
        storageJson = mapper.writeValueAsString(arrayNode);
        return storageJson;
    }

    /**
     * Recursively build the node tree starting from a root through its children
     * @param storageLocation - current storage location starting initially at root
     * @param obN - json object node that's being built
     * @return object node if the storage location no longer had any more children
     */
    public ObjectNode generateJSON(StorageLocation storageLocation, ObjectNode obN, boolean ajax) {
        if (storageLocation == null) {
            return obN;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        obN.put("id", storageLocation.getStorageLocationId());
        obN.put("text", storageLocation.getLabel());
        obN.put("type", storageLocation.getLocationType().toString());
        ObjectNode dataNode = objectMapper.createObjectNode();
        dataNode.put("storageLocationId", storageLocation.getStorageLocationId());
        obN.put("data", dataNode);
        if (ajax) {
            if (!storageLocation.getChildrenStorageLocation().isEmpty() ||
                !storageLocation.getLabVessels().isEmpty()) {
                obN.put("children", true);
            }
        } else {
            ArrayNode childN = obN.arrayNode();
            obN.put("children", childN);
            if (storageLocation.getChildrenStorageLocation() == null ||
                storageLocation.getChildrenStorageLocation().isEmpty()) {
                return obN;
            }

            List<StorageLocation> childLocations = new ArrayList<>(storageLocation.getChildrenStorageLocation());
            Collections.sort(childLocations, new StorageLocation.StorageLocationLabelComparator());
            for (StorageLocation childLocation : childLocations) {
                childN.add(generateJSON(childLocation, objectMapper.createObjectNode(), ajax));
            }
        }

        return obN;
    }

    public StorageLocation.LocationType getLocationType() {
        return locationType;
    }

    public void setLocationType(
            StorageLocation.LocationType locationType) {
        this.locationType = locationType;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) {
        this.vesselGeometry = vesselGeometry;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStorageName() {
        return storageName;
    }

    public void setStorageName(String storageName) {
        this.storageName = storageName;
    }

    public long getStorageId() {
        return storageId;
    }

    public void setStorageId(long storageId) {
        this.storageId = storageId;
    }

    public String getLocationTrailString() {
        return locationTrailString;
    }

    public void setLocationTrailString(String locationTrailString) {
        this.locationTrailString = locationTrailString;
    }

    public StorageLocation getStorageLocation() {
        return storageLocation;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        this.storageLocation = storageLocation;
    }

    public String getSearchTerm() {
        return searchTerm;
    }

    public void setSearchTerm(String searchTerm) {
        this.searchTerm = searchTerm;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<StorageLocation> getChildStorageLocations() {
        return childStorageLocations;
    }

    public void setChildStorageLocations(
            List<StorageLocation> childStorageLocations) {
        this.childStorageLocations = childStorageLocations;
    }

    public Map<Long, String> getMapIdToBarcode() {
        return mapIdToBarcode;
    }

    public void setMapIdToBarcode(Map<Long, String> mapIdToBarcode) {
        this.mapIdToBarcode = mapIdToBarcode;
    }

    public Map<Long, StorageLocation> getMapIdToStorageLocation() {
        return mapIdToStorageLocation;
    }

    public void setMapIdToStorageLocation(
            Map<Long, StorageLocation> mapIdToStorageLocation) {
        this.mapIdToStorageLocation = mapIdToStorageLocation;
    }

    /** For testing. */
    void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }

    /** For testing. */
    public void setStorageLocationDao(StorageLocationDao storageLocationDao) {
        this.storageLocationDao = storageLocationDao;
    }

    /** For testing. */
    public void setLabVesselDao(LabVesselDao labVesselDao) {
        this.labVesselDao = labVesselDao;
    }

    public MessageCollection getMessageCollection() {
        return messageCollection;
    }

}

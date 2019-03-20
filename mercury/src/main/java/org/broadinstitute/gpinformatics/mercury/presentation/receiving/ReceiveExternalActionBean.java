package org.broadinstitute.gpinformatics.mercury.presentation.receiving;

import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.collection.Group;
import org.broadinstitute.bsp.client.collection.SampleCollection;
import org.broadinstitute.bsp.client.site.Site;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGroupCollectionList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSiteList;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleInfo;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.ExternalSamplesRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.IdNames;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.SampleKitReceivedBean;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.labevent.eventhandlers.BSPRestSender;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import javax.inject.Inject;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@UrlBinding(ReceiveExternalActionBean.ACTION_BEAN_URL)
public class ReceiveExternalActionBean extends ReceivingActionBean {
    private static final Log logger = LogFactory.getLog(ReceivingActionBean.class);

    public static final String ACTION_BEAN_URL = "/receiving/receiveByExternal.action";
    public static final String RECEIEVE_EXT_TUBE = "byReceiveExternalTube";
    public static final String RECEIVE_BY_EXTERNAL_PAGE = "/receiving/receive_external_tubes.jsp";
    public static final String EXTERNAL_TUBES_TABLE = "/receiving/external_tubes_table.jsp";
    public static final String LOAD_COLLECTION_ACTION = "loadCollectionSelect";
    public static final String LOAD_SITE_SELECT_ACTION = "loadSiteSelect";
    public static final String VALIDATE_PLATE_TUBE_ACTION = "validatePlateTube";
    public static final String GET_NEXT_OPEN_POSITION = "getNextOpenPosition";
    public static final String LOAD_LABELS_ACTION = "loadLabels";
    public static final String UPLOAD_KIT = "uploadKit";
    public static final String ADD_TUBE = "addTube";
    private  String tubeData = "{}";

    @Inject
    private BSPGroupCollectionList bspGroupCollectionList;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private BSPRestSender bspRestSender;

    @Inject
    private BSPConfig bspConfig;

    private Map<Group, ArrayList<SampleCollection>> mapGroupToCollection;

    private Map<String, ArrayList<SampleCollection>> mapGroupNameToCollection;

    private String groupName;

    private long collectionId;

    private List<SampleCollection> collections;

    private List<Site> sites;

    private Collection<Pair<Long, String>> organisms;

    private Map<VesselPosition, String> positionToBarcode;

    private MessageCollection messageCollection = new MessageCollection();

    @Validate(required = true, on = {GET_NEXT_OPEN_POSITION, VALIDATE_PLATE_TUBE_ACTION})
    private String containerBarcode;

    @Validate(required = true, on = {VALIDATE_PLATE_TUBE_ACTION, LOAD_LABELS_ACTION})
    private String receptacleType;

    @HandlesEvent(RECEIEVE_EXT_TUBE)
    public Resolution receiveExternalTubes() {
        init();
        return new ForwardResolution(RECEIVE_BY_EXTERNAL_PAGE);
    }

    @Before(stages = LifecycleStage.BindingAndValidation, on = {LOAD_COLLECTION_ACTION})
    public void init() {
        mapGroupToCollection = new HashMap<>();
        mapGroupNameToCollection = new HashMap<>();
        for (SampleCollection sampleCollection: bspGroupCollectionList.getCollections().values()) {
            if (!mapGroupToCollection.containsKey(sampleCollection.getGroup())) {
                mapGroupToCollection.put(sampleCollection.getGroup(), new ArrayList<>());
                mapGroupNameToCollection.put(sampleCollection.getGroup().getGroupName(), new ArrayList<>());
            }
            mapGroupToCollection.get(sampleCollection.getGroup()).add(sampleCollection);
            mapGroupNameToCollection.get(sampleCollection.getGroup().getGroupName()).add(sampleCollection);
        }

        // Load First Group > collection > site if non selected
        if (groupName == null) {
            groupName = mapGroupToCollection.keySet().iterator().next().getGroupName();
            collections = mapGroupNameToCollection.get(groupName);
            SampleCollection sampleCollection = collections.iterator().next();
            sites = bspGroupCollectionList.getSitesForCollection(sampleCollection.getCollectionId());
            organisms = sampleCollection.getOrganisms();
        }
    }

    @HandlesEvent(LOAD_COLLECTION_ACTION)
    public Resolution loadCollectionSelect() throws IOException {
        collections = mapGroupNameToCollection.get(groupName);
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode arrayNode = mapper.createArrayNode();
        collections.forEach(c -> {
            ObjectNode objectNode = mapper.createObjectNode();
            objectNode.put("label", c.getCollectionName());
            objectNode.put("value", c.getCollectionId());
            arrayNode.add(objectNode);
        });
        return new StreamingResolution("application/json", new StringReader(mapper.writeValueAsString(arrayNode)));
    }

    @HandlesEvent(LOAD_SITE_SELECT_ACTION)
    public Resolution loadSiteSelect() throws IOException {
        sites = bspGroupCollectionList.getSitesForCollection(collectionId);
        ObjectMapper mapper = new ObjectMapper();

        ObjectNode parentNode = mapper.createObjectNode();

        // Create Sites Array
        ArrayNode sitesArray = mapper.createArrayNode();
        sites.forEach(c -> {
            ObjectNode objectNode = mapper.createObjectNode();
            objectNode.put("label", c.getName());
            objectNode.put("value", c.getId());
            sitesArray.add(objectNode);
        });
        parentNode.put("sites", sitesArray);

        // Create Organism Array
        SampleCollection sampleCollection = bspGroupCollectionList.getById(collectionId);
        ArrayNode organismArray = mapper.createArrayNode();
        Collection<Pair<Long, String>> organisms = sampleCollection.getOrganisms();
        organisms.forEach(pair -> {
            ObjectNode objectNode = mapper.createObjectNode();
            objectNode.put("label", pair.getValue());
            objectNode.put("value", pair.getKey());
            organismArray.add(objectNode);
        });
        parentNode.put("organisms", organismArray);

        return new StreamingResolution("application/json", new StringReader(mapper.writeValueAsString(parentNode)));
    }

    @HandlesEvent(GET_NEXT_OPEN_POSITION)
    public Resolution nextOpenPosition() throws IOException {
        RackOfTubes rackOfTubes = rackOfTubesDao.findByBarcode(containerBarcode);
        if (rackOfTubes == null) {
            throw new RuntimeException("Failed to find Rack Of Tubes in Mercury");
        }

        GetSampleInfo.SampleInfos sampleInfos =
                bspRestSender.getSampleInfo(containerBarcode);

        List<String> bspOccupiedPositions = sampleInfos.getSampleInfos().stream()
                .map(GetSampleInfo.SampleInfo::getPosition)
                .collect(Collectors.toList());

        List<VesselPosition> vesselPositions =
                Arrays.asList(rackOfTubes.getVesselGeometry().getVesselPositions());

        List<String> availablePositions = vesselPositions.stream()
                .filter(vp -> !bspOccupiedPositions.contains(vp.name()))
                .map(VesselPosition::name)
                .collect(Collectors.toList());

        ObjectMapper mapper = new ObjectMapper();
        ContainerWellInfo containerWellInfo = new ContainerWellInfo(containerBarcode, availablePositions);
        String json = mapper.writeValueAsString(containerWellInfo);
        return new StreamingResolution("application/json", json);
    }

    @HandlesEvent(VALIDATE_PLATE_TUBE_ACTION)
    public Resolution validatePlateTube() throws IOException {
        IdNames childReceptacleTypesForBarcode = bspRestService.getChildReceptacleTypesForBarcode(containerBarcode);
        BarcodedTube.BarcodedTubeType barcodedTubeType =
                BarcodedTube.BarcodedTubeType.getByAutomationName(receptacleType);
        if (barcodedTubeType == null) {
            return createErrorObject(true, "Unknown Receptacle Type " + receptacleType);
        }
        for (IdNames.IdName idName: childReceptacleTypesForBarcode.getIdNames()) {
            if (idName.getName().equals(barcodedTubeType.getAutomationName()) ||
                idName.getName().equals(barcodedTubeType.getDisplayName())) {
                return createErrorObject(false, null);
            }
        }
        String errMsg = String.format("%s is not a valid child receptacle for %s", receptacleType,
                containerBarcode);
        return createErrorObject(true, errMsg);
    }

    private StreamingResolution createErrorObject(boolean hasError, String msg) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("hasErrors", hasError);
        objectNode.put("msg", msg);
        String json = mapper.writeValueAsString(objectNode);
        return new StreamingResolution("application/json", json);
    }

    @HandlesEvent(LOAD_LABELS_ACTION)
    public Resolution loadLabels() throws IOException {
        IdNames labelFormats = bspRestService.getLabelFormats(receptacleType);
        ObjectMapper mapper = new ObjectMapper();
        return new StreamingResolution("application/json", mapper.writeValueAsString(labelFormats));
    }

    @HandlesEvent(UPLOAD_KIT)
    public Resolution validateUpload() throws IOException {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
            ExternalSamplesRequest request = objectMapper.readValue(tubeData, ExternalSamplesRequest.class);
            request.setUsername(getUserBean().getBspUser().getUsername());
            SampleKitReceivedBean response = receiveSamplesEjb.receiveExternalTubes(request, messageCollection);
            for (String error : response.getMessages()) {
                addGlobalValidationError(error);
            }

            return createErrorObject(false, "Sucessfully received samples in BSP");
        } catch (Exception e) {
            logger.error("Failed to receive external tubes", e);
            return createErrorObject(true, e.getMessage());
        }
    }

    @HandlesEvent(ADD_TUBE)
    public Resolution addTube() {
        return new ForwardResolution(EXTERNAL_TUBES_TABLE);
    }

    @Override
    public String getRackScanPageUrl() {
        return null;
    }

    @Override
    public String getPageTitle() {
        return null;
    }

    public Map<Group, ArrayList<SampleCollection>> getMapGroupToCollection() {
        return mapGroupToCollection;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public long getCollectionId() {
        return collectionId;
    }

    public void setCollectionId(long collectionId) {
        this.collectionId = collectionId;
    }

    public List<SampleCollection> getCollections() {
        return collections;
    }

    public void setCollections(List<SampleCollection> collections) {
        this.collections = collections;
    }

    public List<Site> getSites() {
        return sites;
    }

    public Map<VesselPosition, String> getPositionToBarcode() {
        return positionToBarcode;
    }

    public void setPositionToBarcode(
            Map<VesselPosition, String> positionToBarcode) {
        this.positionToBarcode = positionToBarcode;
    }

    public String getContainerBarcode() {
        return containerBarcode;
    }

    public void setContainerBarcode(String containerBarcode) {
        this.containerBarcode = containerBarcode;
    }

    public Collection<Pair<Long, String>> getOrganisms() {
        return organisms;
    }

    public void setOrganisms(
            Collection<Pair<Long, String>> organisms) {
        this.organisms = organisms;
    }

    public BSPConfig getBspConfig() {
        return bspConfig;
    }

    public String getReceptacleType() {
        return receptacleType;
    }

    public void setReceptacleType(String receptacleType) {
        this.receptacleType = receptacleType;
    }

    public String getTubeData() {
        return tubeData;
    }

    public void setTubeData(String tubeData) {
        this.tubeData = tubeData;
    }

    public static class ContainerWellInfo {
        private String containerBarcode;
        private List<String> availableWells;

        public ContainerWellInfo() {
        }

        public ContainerWellInfo(String containerBarcode, List<String> availableWells) {
            this.containerBarcode = containerBarcode;
            this.availableWells = availableWells;
        }

        public String getContainerBarcode() {
            return containerBarcode;
        }

        public void setContainerBarcode(String containerBarcode) {
            this.containerBarcode = containerBarcode;
        }

        public List<String> getAvailableWells() {
            return availableWells;
        }

        public void setAvailableWells(List<String> availableWells) {
            this.availableWells = availableWells;
        }
    }

    private class WellBarcode {
        private String wellPosition;
        private String manufacturerBarcode;

        public String getWellPosition() {
            return wellPosition;
        }

        public void setWellPosition(String wellPosition) {
            this.wellPosition = wellPosition;
        }

        public String getManufacturerBarcode() {
            return manufacturerBarcode;
        }

        public void setManufacturerBarcode(String manufacturerBarcode) {
            this.manufacturerBarcode = manufacturerBarcode;
        }
    }

    public class KitCreationInfo {
        private String collection;
        private BarcodedTube.BarcodedTubeType barcodedTubeType;
        private String materialType;
        private String originalMaterialType;

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getMaterialType() {
            return collection;
        }

        public void setMaterialType(String materialType) {
            this.collection = materialType;
        }

        public BarcodedTube.BarcodedTubeType getBarcodedTubeType() {
            return barcodedTubeType;
        }

        public void setBarcodedTubeType(
                BarcodedTube.BarcodedTubeType barcodedTubeType) {
            this.barcodedTubeType = barcodedTubeType;
        }
    }

    public enum TubeLabelType implements Displayable {
        PARTICIPANT_ID("Collaborator Participant ID"),
        PARTICIPANT_ID_GENERATE_SAMPLE("Collaborator Participant ID (Sample ID Generated)"),
        SAMPLE_ID("Collaborator Sample ID");

        private String displayName;

        TubeLabelType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }

    public enum ShippingType implements Displayable {
        FEDEX("Fedex"),
        FEDEX_2DAY_DOMESTIC_AUTO("Fedex 2Day Domestic"),
        FEDEX_PRIORITY_OVERNIGHT_DOMESTIC_AUTO("Fedex Priority Overnight Domestic"),
        FEDEX_FIRST_OVERNIGHT_DOMESTIC_AUTO("Fedex First Overnight Domestic"),
        UPS("UPS"),
        DHL("DHL"),
        PICK_UP("Pick Up"),
        LASERSHIP("LaserShip"),
        SKYCOM("SkyCom"),
        BROAD_TRUCK("Broad Truck"),
        WORLD_COURIER("World Courier"),
        TRANSFER("Kit Awaiting Transfer"),
        MNX("MNX"),
        DDP_SHIPPER("DDP Shipper");

        private String displayName;

        ShippingType(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }
    }
}

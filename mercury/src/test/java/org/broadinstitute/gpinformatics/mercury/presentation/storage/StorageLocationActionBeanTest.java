package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = TestGroups.DATABASE_FREE)
public class StorageLocationActionBeanTest {
    private static final long FREEZER_ID = 1;
    private static final long FRIDGE_ID = 2;
    private static final long FREEZER_SHELF_ID = 3;
    private static final long GAUGE_RACK_ID = 4;
    private StorageLocationActionBean actionBean;
    private StorageLocationDao mockStorageLocationDao;
    private LabVesselDao mockLabVesselDao;
    private StorageLocation freezer;
    private StorageLocation deliFridge;
    private StorageLocation freezerShelf1;
    private StaticPlate staticPlate;
    private BarcodedTube barcodedTube;
    private ObjectMapper objectMapper = new ObjectMapper();
    private StorageLocation gaugeRack;


    @BeforeMethod
    public void setUp() throws Exception {
        actionBean = new StorageLocationActionBean();
        actionBean.setContext(new CoreActionBeanContext());

        mockStorageLocationDao = mock(StorageLocationDao.class);
        actionBean.setStorageLocationDao(mockStorageLocationDao);
        freezer = createNewStorage(StorageLocation.LocationType.FREEZER, "Freezer", 1, 3);
        freezer.setStorageLocationId(FREEZER_ID);
        freezerShelf1 = freezer.getChildrenStorageLocation().iterator().next().getChildrenStorageLocation().
                iterator().next();
        freezerShelf1.setStorageLocationId(FREEZER_SHELF_ID);
        deliFridge = createNewStorage(StorageLocation.LocationType.REFRIGERATOR, "Deli Fridge", 2, 4);
        deliFridge.setStorageLocationId(FRIDGE_ID);
        deliFridge.setBarcode("DeliFridgeBarcode");
        when(mockStorageLocationDao.findRootLocations()).
                thenReturn(Arrays.asList(freezer, deliFridge));
        when(mockStorageLocationDao.findByBarcode(deliFridge.getBarcode())).thenReturn(deliFridge);
        when(mockStorageLocationDao.findById(StorageLocation.class, FREEZER_ID)).thenReturn(freezer);

        gaugeRack = new StorageLocation("Gage Rack", StorageLocation.LocationType.GAUGERACK, null);
        gaugeRack.setStorageLocationId(GAUGE_RACK_ID);
        when(mockStorageLocationDao.findById(StorageLocation.class, GAUGE_RACK_ID)).thenReturn(gaugeRack);

        mockLabVesselDao = mock(LabVesselDao.class);
        staticPlate = new StaticPlate("TestPlate", StaticPlate.PlateType.Plate96Well200PCR);
        when(mockLabVesselDao.findByIdentifier("TestPlate")).thenReturn(staticPlate);
        actionBean.setLabVesselDao(mockLabVesselDao);

        UserBean userBean = Mockito.mock(UserBean.class);
        BspUser qaDudeUser = new BSPUserList.QADudeUser(RoleType.PM.name(), 1L);
        Mockito.when(userBean.getBspUser()).thenReturn(qaDudeUser);
        actionBean.setUserBean(userBean);

        barcodedTube = new BarcodedTube("TestTube");
        when(mockLabVesselDao.findByIdentifier(barcodedTube.getLabel())).thenReturn(barcodedTube);
    }

    private StorageLocation createNewStorage(StorageLocation.LocationType locationType, String label, int numSections,
                                             int numShelves) {
        StorageLocation storageLocation = new StorageLocation(label, locationType, null);
        Set<StorageLocation> sections = new HashSet<>();
        for (int i = 0; i < numSections; i++) {
            StorageLocation section = new StorageLocation(label + " Section " + i, StorageLocation.LocationType.SECTION,
                    storageLocation);
            sections.add(section);
            Set<StorageLocation> shelves = new HashSet<>();
            for (int j = 0; j < numShelves; j++) {
                StorageLocation shelf = new StorageLocation(label + " Shelf " + i, StorageLocation.LocationType.SHELF,
                        storageLocation);
                shelves.add(shelf);
            }
            section.setChildrenStorageLocation(shelves);
        }
        storageLocation.setChildrenStorageLocation(sections);
        return storageLocation;
    }

    @Test
    public void testLoadTreeAjax() throws Exception {
        actionBean.setId(StorageLocationActionBean.ROOT_NODE);
        StreamingResolution resolution = (StreamingResolution) actionBean.loadTreeAjax();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolution.execute(request, response);
        String jsonString = response.getOutputString();
        ArrayNode arrayNode = (ArrayNode) objectMapper.readValue(jsonString, JsonNode.class);
        Assert.assertEquals(arrayNode.size(), 2);
        for (JsonNode jsonNode: arrayNode) {
            if (jsonNode.get("id").asInt() == 1) {
                Assert.assertEquals(jsonNode.get("text").asText(), "Freezer");
                Assert.assertEquals(jsonNode.get("type").asText(), "FREEZER");
                Assert.assertEquals(jsonNode.get("children").asText(), "true");
            } else if (jsonNode.get("id").asInt() == 2) {
                Assert.assertEquals(jsonNode.get("text").asText(), "Deli Fridge");
                Assert.assertEquals(jsonNode.get("type").asText(), "REFRIGERATOR");
                Assert.assertEquals(jsonNode.get("children").asText(), "true");
            }
        }
    }

    @Test
    public void testSearch() throws Exception {
        actionBean.setSearchTerm(deliFridge.getBarcode());
        StreamingResolution resolution = (StreamingResolution) actionBean.searchForNode();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolution.execute(request, response);
        String jsonString = response.getOutputString();
        ArrayNode arrayNode = (ArrayNode) objectMapper.readValue(jsonString, JsonNode.class);
        Assert.assertEquals(arrayNode.size(), 2);
        boolean foundSelectedNode = false;
        for (JsonNode jsonNode: arrayNode) {
            if (jsonNode.get("id").asText().equals("selected_node")) {
                foundSelectedNode = true;
                Assert.assertEquals(jsonNode.get("text").asText(), "Deli Fridge");
                Assert.assertEquals(jsonNode.get("state").get("selected").asBoolean(), true);
            }
        }
        Assert.assertEquals(true, foundSelectedNode);
    }

    @Test
    public void testMoveNodeBadArguments() throws Exception {
        StreamingResolution resolution = (StreamingResolution) actionBean.moveNode();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolution.execute(request, response);
        JsonNode jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "Location to move not specified");

        actionBean.setNodeName(String.valueOf(FREEZER_ID));
        resolution = (StreamingResolution) actionBean.moveNode();
        request = new MockHttpServletRequest("foo", "bar");
        response = new MockHttpServletResponse();
        resolution.execute(request, response);
        jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "New Location Not Specified");

        actionBean.setNodeName(String.valueOf(FREEZER_ID));
        actionBean.setNewParentName(String.valueOf(FRIDGE_ID));
        resolution = (StreamingResolution) actionBean.moveNode();
        request = new MockHttpServletRequest("foo", "bar");
        response = new MockHttpServletResponse();
        resolution.execute(request, response);
        jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "Cannot move location of this type: " +
                                                             StorageLocation.LocationType.FREEZER.getDisplayName());
    }

    @Test
    public void testRenameNode() throws Exception {
        StreamingResolution resolution = (StreamingResolution) actionBean.renameNode();
        HttpServletRequest request = new MockHttpServletRequest("foo", "bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolution.execute(request, response);
        JsonNode jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "Location to rename not specified");

        actionBean.setNodeName(String.valueOf(FREEZER_ID));
        resolution = (StreamingResolution) actionBean.renameNode();
        request = new MockHttpServletRequest("foo", "bar");
        response = new MockHttpServletResponse();
        resolution.execute(request, response);
        jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "Storage Name is required.");

        actionBean.setStorageName("New Storage Name");
        resolution = (StreamingResolution) actionBean.renameNode();
        request = new MockHttpServletRequest("foo", "bar");
        response = new MockHttpServletResponse();
        resolution.execute(request, response);
        jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), true);
        Assert.assertEquals(jsonNode.get("error").asText(), "Cannot rename location of this type: Freezer");

        String newName = "New Gauge Rack Storage Name";
        actionBean.setStorageName(newName);
        actionBean.setNodeName(String.valueOf(gaugeRack.getStorageLocationId()));
        resolution = (StreamingResolution) actionBean.renameNode();
        request = new MockHttpServletRequest("foo", "bar");
        response = new MockHttpServletResponse();
        resolution.execute(request, response);
        jsonNode = objectMapper.readValue(response.getOutputString(), JsonNode.class);
        Assert.assertEquals(jsonNode.get("hasError").asBoolean(), false);
        Assert.assertEquals(gaugeRack.getLabel(), newName);
    }
}
package org.broadinstitute.gpinformatics.mercury.presentation.storage;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.rackscan.ScannerException;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.storage.StorageLocationDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.storage.StorageLocation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;
import org.codehaus.jackson.map.ObjectMapper;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check in/out LabVessels and TubeFormations from Storage
 */
@UrlBinding(StorageManagerActionBean.ACTION_BEAN_URL)
public class StorageManagerActionBean extends RackScanActionBean {
    private static final Log logger = LogFactory.getLog(StorageManagerActionBean.class);

    public static final String PAGE_TITLE = "Manage Storage";
    public static final String ACTION_BEAN_URL = "/storage/storageManager.action";
    public static final String VESSEL_SEARCH = "vesselBarcodeSearch";
    public static final String FIRE_RACK_SCAN = "fireRackScan";
    public static final String FREEZER_SEARCH = "freezerSearch";
    public static final String SLOT_BARCODE_SEARCH = "slotBarcodeSearch";
    public static final String STORE = "store";

    private static final String VIEW_PAGE = "/storage/manage_storage.jsp";
    private static final String LOCATION_SELECT = "/storage/location_select.jsp";

    private String searchKey;
    private String vesselBarcode;
    private String containerBarcode;
    private boolean resultsAvailable = false;
    private boolean isSearchDone = false;
    private Set<LabVessel> foundVessels = new HashSet<>();
    private VesselGeometry vesselGeometry;
    private String slotSearchKey;
    private String freezerSearchKey;
    private List<StorageLocation> rackLocations;
    private List<StorageLocation> freezerLocations;
    private List<StorageLocation> locations;
    private List<String> storageLocations;
    private SlotSearchResult searchResult;
    private MessageCollection messageCollection = new MessageCollection();

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private StorageLocationDao storageLocationDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(STORE)
    public Resolution store() {
        if (storageLocations == null || storageLocations.isEmpty()) {
            messageCollection.addError("Need to select a storage location.");
        } else {

        }

        if (messageCollection.hasErrors()) {
            addMessages(messageCollection);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(VESSEL_SEARCH)
    public Resolution vesselBarcodeSearch() throws Exception {
        doSearch();
        if(searchKey == null) {
            setSearchDone(false);
            messageCollection.addError("Please provide a barcode");
            addMessages(messageCollection);
        }
        if(!isResultsAvailable()) {
            setSearchDone(false);
            messageCollection.addError("No results found for: " + getSearchKey());
            addMessages(messageCollection);
        }
        containerBarcode = searchKey;
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(FIRE_RACK_SCAN)
    public Resolution fireRackScan() throws ScannerException {
        scan();
        setRackScanGeometry();
        if(getRackScan() != null) {
            verifyRackUpload();
            if(messageCollection.hasErrors())  {
                addMessages(messageCollection);
            }
            else {
                setRackScanGeometry();
                resultsAvailable = true;
                isSearchDone = true;
            }
        }

        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(FREEZER_SEARCH)
    public Resolution freezerSearch() {
        logger.info("Looking up freezer children: " + freezerSearchKey);
        final StringBuilder errors = new StringBuilder();
        String json = "";
        if (freezerSearchKey == null || freezerSearchKey.isEmpty()) {
            errors.append("Freezer Search Key required.");
        } else {
            StorageLocation storageLocation = storageLocationDao.findByIdentifier(freezerSearchKey);
            if (storageLocation == null) {
                errors.append("Failed to find freezer.");
            } else {
                setLocations(new ArrayList<>(storageLocation.getChildrenStorageLocation()));
                ObjectMapper mapper = new ObjectMapper();
                try {
                    json = mapper.writeValueAsString(storageLocation);
                } catch (IOException e) {
                    logger.error("Failed to serialize Location to json", e);
                    errors.append("Failed to serialize location to json");
                }
            }
        }
        return new ForwardResolution(LOCATION_SELECT);
    }

    /**
     * TODO slot needs to return its Rack, Rack needs to return its parent history
     * @return
     */
    @HandlesEvent(SLOT_BARCODE_SEARCH)
    public Resolution slotBarcodeSearch() {
        logger.info("Slot Barcode search is actually being called?/");
        final StringBuilder errors = new StringBuilder();
        String json = "";
        if (slotSearchKey == null || slotSearchKey.isEmpty()) {
            errors.append("Slot Search Key required.");
        } else {
            StorageLocation storageLocation = storageLocationDao.findByIdentifier(slotSearchKey);
            if (storageLocation == null) {
                errors.append("Failed to find slot.");
            } else {
                searchResult = new SlotSearchResult(storageLocation);
                ObjectMapper mapper = new ObjectMapper();
                try {
                    json = mapper.writeValueAsString(searchResult);
                } catch (IOException e) {
                    logger.error("Failed to serialize Location to json", e);
                    errors.append("Failed to serialize location to json");
                }
            }
        }
        return new ForwardResolution(LOCATION_SELECT);
    }

    private Resolution buildAjaxStream(final StringBuilder errors, final String json) {
        return new StreamingResolution("text/plain") {
            @Override
            public void stream(HttpServletResponse response) throws Exception {
                ServletOutputStream out = response.getOutputStream();
                if (errors.length() > 0) {
                    out.write("Failure: ".getBytes());
                    out.write(errors.toString().getBytes());
                } else {
                    out.write(json.getBytes());
                }
                out.close();
            }
        };
    }

    @Override
    public String getRackScanPageUrl() {
        return ACTION_BEAN_URL;
    }

    @Override
    public String getPageTitle() {
        return PAGE_TITLE;
    }

    /**
     * This method creates a list of found vessels for the manage_storage.jsp page.
     *
     */
    protected void doSearch()
    {
        List<String> searchList = new ArrayList<String>();
        searchList.add(searchKey);
        LabVessel vessel = labVesselDao.findByIdentifier(searchList.get(0));
        if (vessel == null) {
            resultsAvailable = false;
            return;
        }
        if(isRackOfTubes(vessel) && getSearchKey() != null){
            resultsAvailable = false;
            messageCollection.addError("You must perform a rack scan to add tubes in a rack to storage.");
            addMessages(messageCollection);
            return;
        }
        vesselGeometry = vessel.getVesselGeometry();
        foundVessels.add(vessel);
        resultsAvailable = true;
        isSearchDone = true;
    }

    public void verifyRackUpload()
    {
        if(rackScan.size() == 0) {
            messageCollection.addError("No barcodes scanned.");
            return;
        }

        List<String> barcodes = new ArrayList<>();
        for (Map.Entry<String, String> entry: rackScan.entrySet()) {
            if (!entry.getKey().equals("rack")) {
                barcodes.add(entry.getValue());
            } else {
                containerBarcode = entry.getKey();
            }
        }

        Map<String, LabVessel> labVesselMap = labVesselDao.findByBarcodes(barcodes);
        for (Map.Entry<String, LabVessel> entry: labVesselMap.entrySet()) {
            if (entry.getValue() == null) {
                messageCollection.addError("Unknown barcode " + entry.getKey());
            }
        }
    }

    public void setRackScanGeometry()
    {
        LinkedHashMap<String, String> rackScan = getRackScan();
        int rackSize = rackScan.size();
        switch (rackSize) {
        case 96:  setVesselGeometry(RackOfTubes.RackType.Matrix96.getVesselGeometry());
            break;
        case 48:  setVesselGeometry(RackOfTubes.RackType.Matrix48SlotRack2mL.getVesselGeometry());
            break;
        default: // Needed for the simulator since it does not provide a default geometry.
            setVesselGeometry(RackOfTubes.RackType.Matrix96.getVesselGeometry());
            break;
        }
    }

    private boolean isRackOfTubes(LabVessel labVessel) {
        if (labVessel.getType().equals(LabVessel.ContainerType.RACK_OF_TUBES)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isMatrixTube(LabVessel labVessel) {
        if (labVessel.getType().equals(LabVessel.ContainerType.TUBE)) {
            return true;
        } else {
            return false;
        }
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public String getVesselBarcode() {
        return vesselBarcode;
    }

    public void setVesselBarcode(String vesselBarcode) {
        this.vesselBarcode = vesselBarcode;
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) {
        this.vesselGeometry = vesselGeometry;
    }

    public boolean isResultsAvailable() {
        return resultsAvailable;
    }

    public void setResultsAvailable(boolean resultsAvailable) {
        this.resultsAvailable = resultsAvailable;
    }

    public boolean isSearchDone() {
        return isSearchDone;
    }

    public void setSearchDone(boolean searchDone) {
        isSearchDone = searchDone;
    }

    public Set<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(Set<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public String getContainerBarcode() {
        return containerBarcode;
    }

    public void setContainerBarcode(String containerBarcode) {
        this.containerBarcode = containerBarcode;
    }

    public String getSlotSearchKey() {
        return slotSearchKey;
    }

    public void setSlotSearchKey(String slotSearchKey) {
        this.slotSearchKey = slotSearchKey;
    }

    public String getFreezerSearchKey() {
        return freezerSearchKey;
    }

    public void setFreezerSearchKey(String freezerSearchKey) {
        this.freezerSearchKey = freezerSearchKey;
    }

    public List<StorageLocation> getFreezerLocations() {
        if (freezerLocations == null) {
            freezerLocations = storageLocationDao.findByLocationType(StorageLocation.LocationType.Freezer);
        }
        return freezerLocations;
    }

    public void setFreezerLocations(
            List<StorageLocation> freezerLocations) {
        this.freezerLocations = freezerLocations;
    }

    public List<StorageLocation> getLocations() {
        return locations;
    }

    public List<StorageLocation> getRackLocations() {
        return rackLocations;
    }

    public void setRackLocations(List<StorageLocation> rackLocations) {
        this.rackLocations = rackLocations;
    }

    public void setLocations(List<StorageLocation> locations) {
        this.locations = locations;
    }

    public SlotSearchResult getSearchResult() {
        return searchResult;
    }

    public void setSearchResult(
            SlotSearchResult searchResult) {
        this.searchResult = searchResult;
    }

    public List<String> getStorageLocations() {
        return storageLocations;
    }

    public void setStorageLocations(
            List<String> storageLocations) {
        this.storageLocations = storageLocations;
    }

    public class SlotSearchResult {
        private StorageLocation rack;
        private Deque<StorageLocation> locationTrail;

        public SlotSearchResult(StorageLocation storageLocation) {
            rack = storageLocation.getParentStorageLocation();
            locationTrail = new ArrayDeque<>();
            StorageLocation parentLocation = storageLocation.getParentStorageLocation();
            while (parentLocation != null) {
                parentLocation = parentLocation.getParentStorageLocation();
                if (parentLocation != null) {
                    locationTrail.addFirst(parentLocation);
                }
            }
        }

        public StorageLocation getRack() {
            return rack;
        }

        public void setRack(StorageLocation rack) {
            this.rack = rack;
        }

        public Deque<StorageLocation> getLocationTrail() {
            return locationTrail;
        }

        public void setLocationTrail(Deque<StorageLocation> locationTrail) {
            this.locationTrail = locationTrail;
        }
    }
}

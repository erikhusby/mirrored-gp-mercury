package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectReader;
import org.codehaus.jackson.map.ObjectWriter;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abandon / Un-Abandon vessel / plastic logic.
 */

@UrlBinding(value = "/workflow/AbandonVessel.action")
public class AbandonVesselActionBean extends CoreActionBean {

    private static final Log log = LogFactory.getLog(AbandonVesselActionBean.class);

    private static final String DEFAULT_PAGE = "/workflow/abandon_vessel.jsp";
    private static final String VESSEL_SEARCH_EVENT = "vesselBarcodeSearch";
    private static final String RACK_SCAN_EVENT = "processRackScan";
    private static final String ABANDON_EVENT = "abandon";
    private static final String UNABANDON_EVENT = "unabandon";

    // JSON handlers, guaranteed threadsafe
    private static final ObjectReader LAYOUT_JSON_READER;
    private static final ObjectWriter LAYOUT_JSON_WRITER;
    static {
        ObjectMapper mapper = new ObjectMapper();
        LAYOUT_JSON_READER = mapper.reader(LayoutMap.class);
        LAYOUT_JSON_WRITER = mapper.writer();
        mapper = null;
    }

    // Form fields
    private String vesselBarcode;
    private String redisplayVesselBarcode;
    private String rackScanData;
    private String abandonActionJson;
    private AbandonVessel.Reason abandonActionReason;

    // Display and logic fields
    private VesselGeometry vesselGeometry;
    private LabVessel labVessel;
    private boolean isTube;
    private LayoutMap layoutMap;

    @Inject
    private LabVesselDao labVesselDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * Do a browser vessel search request and generate layout data
     */
    @HandlesEvent(VESSEL_SEARCH_EVENT)
    public Resolution vesselBarcodeSearch() {

        rackScanData = null;
        redisplayVesselBarcode = null;

        if( vesselBarcode == null ) {
            addGlobalValidationError("Please provide a barcode");
            return new ForwardResolution(DEFAULT_PAGE);
        }
        labVessel = labVesselDao.findByIdentifier(vesselBarcode);
        if( labVessel == null ) {
            addGlobalValidationError("Vessel not found for barcode " + vesselBarcode);
            return new ForwardResolution(DEFAULT_PAGE);
        }

        Map<VesselPosition, LabVessel> positionVesselMap = new HashMap<>();
        if( labVessel.getContainerRole() != null ) {
            VesselContainer vesselContainer = labVessel.getContainerRole();
            vesselGeometry = vesselContainer.getEmbedder().getVesselGeometry();
            if( vesselContainer.hasAnonymousVessels()) {
                for( VesselPosition vesselPosition: vesselGeometry.getVesselPositions()){
                    positionVesselMap.put(vesselPosition, labVessel );
                }
            } else {
                for( VesselPosition vesselPosition: vesselGeometry.getVesselPositions()){
                    positionVesselMap.put(vesselPosition, vesselContainer.getVesselAtPosition(vesselPosition) );
                }
            }
        } else if(labVessel.getType() == LabVessel.ContainerType.RACK_OF_TUBES) {
            // A rack of tubes may have been manually manipulated - force a rack scan to insure the actual contents are viewed
            addGlobalValidationError("Vessel barcode refers to a rack of tubes, a rack scan is required.");
            return new ForwardResolution(DEFAULT_PAGE);
        } else {
            isTube = true;
            vesselGeometry = VesselGeometry.TUBE;
            positionVesselMap.put(VesselPosition._1_1, labVessel );
        }
        buildLayout(positionVesselMap);
        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * Rack scan handler using rackScanData value posted from browser
     */
    @HandlesEvent(RACK_SCAN_EVENT)
    public Resolution rackScan() throws Exception {
        vesselGeometry = VesselGeometry.G12x8;

        JSONObject rackJson = new JSONObject(rackScanData);
        JSONArray jsonPositionScan = rackJson.getJSONArray("scans");

        // Get barcodes and batch fetch
        List<String> scanBarcodes = new ArrayList<>();
        if( jsonPositionScan != null ) {
            for( int i = 0; i < jsonPositionScan.length(); i++ ) {
                scanBarcodes.add(jsonPositionScan.getJSONObject(i).getString("barcode"));
            }
        } else {
            // Should have been captured at scan
            addGlobalValidationError("No results from rack scan" );
            return new ForwardResolution(DEFAULT_PAGE);
        }
        Map<String,LabVessel> barcodeVesselMap = labVesselDao.findByBarcodes(scanBarcodes);

        // Assign fetched vessels to positions
        Map<VesselPosition, LabVessel> positionVesselMap = new HashMap<>();
        Map<VesselPosition, String> positionErrorMap = new HashMap<>();
        // Fill with empty flags
        for( VesselPosition position :  vesselGeometry.getVesselPositions()) {
            positionVesselMap.put(position, null);
        }
        for( int i = 0; i < jsonPositionScan.length(); i++ ) {
            JSONObject node = jsonPositionScan.getJSONObject(i);
            VesselPosition position = VesselPosition.getByName(node.getString("position"));
            LabVessel labVessel = barcodeVesselMap.get(node.getString("barcode" ));
            if( labVessel != null ) {
                positionVesselMap.put(position, labVessel);
            } else {
                // Need to handle unregistered tubes!
                positionErrorMap.put(position, "(" + node.getString("barcode" ) + " not found)");
            }
        }

        buildLayout(positionVesselMap, positionErrorMap);

        return new ForwardResolution(DEFAULT_PAGE);
    }

    /**
     * Abandon vessel(s) and position(s).
     */
    @HandlesEvent(ABANDON_EVENT)
    public Resolution abandonPosition() throws Exception {
        LayoutMap abandonMap = LAYOUT_JSON_READER.readValue(abandonActionJson);
        if( abandonMap.abandonCells == null || abandonMap.abandonCells.size() == 0 ) {
            addGlobalValidationError("No cells selected to abandon");
            return new ForwardResolution(DEFAULT_PAGE);
        }
        if( abandonActionReason == null ) {
            addGlobalValidationError("Abandon reason required");
            return new ForwardResolution(DEFAULT_PAGE);
        }
        abandonAction(abandonMap, true);
        return postAbandonAction();
    }

    /**
     * Un-Abandon a specific vessel or positions associated with it.
     */
    @HandlesEvent(UNABANDON_EVENT)
    public Resolution unAbandonVessel() throws Exception {
        LayoutMap abandonMap = LAYOUT_JSON_READER.readValue(abandonActionJson);
        if( abandonMap.abandonCells == null || abandonMap.abandonCells.size() == 0 ) {
            addGlobalValidationError("No cells selected to un-abandon");
            return new ForwardResolution(DEFAULT_PAGE);
        }
        abandonAction(abandonMap, false);
        return postAbandonAction();
    }

    /**
     * Perform the requested abandon or un-abandon action
     * @param abandonMap Generated from JSON cells posted from browser as abandonActionJson
     * @param doAbandon Flag to either abandon or un-abandon
     */
    private void abandonAction( LayoutMap abandonMap, boolean doAbandon ) {
        boolean isAnonymousContainer = false;
        Date abandonedOn = new Date();

        // Vessels in request (only one if container or tube)
        List<LabVessel> vessels = new ArrayList<>();
        for( AbandonCell abandonCell : abandonMap.abandonCells ) {
            LabVessel selectedVessel = labVesselDao.findByIdentifier(abandonCell.barcode);
            vessels.add(selectedVessel);
            if( selectedVessel.getContainerRole() != null ) {
                isAnonymousContainer = selectedVessel.getContainerRole().hasAnonymousVessels();
                if( isAnonymousContainer ){
                    break;
                }
            }
        }

        if( isAnonymousContainer ) {
            // Have to dig through specific positions
            LabVessel vesselToAbandon = vessels.iterator().next();
            Set<AbandonVessel> vesselAbandonedPositions = vesselToAbandon.getAbandonVessels();
            for( AbandonCell abandonCell : abandonMap.abandonCells ) {
                VesselPosition position = abandonCell.getVesselPosition();

                // Sanity check - do an overwrite
                AbandonVessel abandonPositionToOverwrite = null;
                for( AbandonVessel abandonPosition : vesselAbandonedPositions ) {
                    if( position.equals( abandonPosition.getVesselPosition() ) ) {
                        abandonPositionToOverwrite = abandonPosition;
                        break;
                    }
                }
                vesselAbandonedPositions.remove( abandonPositionToOverwrite );

                // If there are physical vessels at plate well positions, abandon them too
                LabVessel well = vesselToAbandon.getContainerRole().getVesselAtPosition(position);
                if( well != null ) {
                    well.getAbandonVessels().clear();
                }

                if( doAbandon ) {
                    AbandonVessel abandonVessel = new AbandonVessel();
                    abandonVessel.setReason(abandonActionReason);
                    abandonVessel.setAbandonedOn(abandonedOn);
                    abandonVessel.setVesselPosition(position);
                    vesselToAbandon.addAbandonedVessel(abandonVessel);
                    if( well != null ) {
                        AbandonVessel wellAbandonVessel = new AbandonVessel();
                        wellAbandonVessel.setReason(abandonActionReason);
                        wellAbandonVessel.setAbandonedOn(abandonedOn);
                        well.addAbandonedVessel(wellAbandonVessel);
                    }
                }
                if( abandonPositionToOverwrite == null ) {
                    if( doAbandon ) {
                        addMessage("Abandoned " + vesselToAbandon.getLabel() + ", " + position );
                    } else {
                        addMessage("Warning: " + vesselToAbandon.getLabel() + ", " + position + " was not abandoned");
                    }
                } else {
                    if( doAbandon ) {
                        addMessage("Abandoned " + vesselToAbandon.getLabel() + ", " + position + " (overwrite " + abandonPositionToOverwrite.getReason().getDisplayName() + ")" );
                    } else {
                        addMessage("Un-Abandoned " + vesselToAbandon.getLabel() + ", " + position );
                    }
                }
            }
        } else {
            for( LabVessel vesselToAbandon : vessels ) {
                Set<AbandonVessel> vesselAbandonedPositions = vesselToAbandon.getAbandonVessels();
                // Sanity check - do an overwrite
                AbandonVessel abandonPositionToOverwrite = null;
                if( vesselAbandonedPositions.size() > 0 ) {
                    abandonPositionToOverwrite = vesselAbandonedPositions.iterator().next();
                    vesselAbandonedPositions.clear();
                }

                if( doAbandon ) {
                    AbandonVessel abandonVessel = new AbandonVessel();
                    abandonVessel.setReason(abandonActionReason);
                    abandonVessel.setAbandonedOn(abandonedOn);
                    vesselToAbandon.addAbandonedVessel(abandonVessel);
                    if( abandonPositionToOverwrite == null ) {
                        addMessage( "Abandoned " + vesselToAbandon.getLabel() );
                    } else {
                        addMessage(
                                "Abandoned " + vesselToAbandon.getLabel() + " (overwrite " + abandonPositionToOverwrite
                                        .getReason().getDisplayName() + ")");
                    }
                } else {
                    if( abandonPositionToOverwrite != null ) {
                        addMessage( "Un-Abandoned " + vesselToAbandon.getLabel() );
                    } else {
                        addMessage( "Warning: " + vesselToAbandon.getLabel() + " was not abandoned.");
                    }
                }
            }
        }

        labVesselDao.flush();
    }

    /**
     * After an abandon/un-abandon action, perform the applicable page refresh logic and refresh
     */
    private Resolution postAbandonAction() throws Exception {
        if( rackScanData != null && rackScanData.length() > 0 ) {
            // Redisplay rack scan data layout
            return rackScan();
        } else {
            // Redisplay existing vessel
            vesselBarcode = redisplayVesselBarcode;
            return vesselBarcodeSearch();
        }
    }

    /**
     * Display purpose only - layout header
     */
    public String getVesselBarcode() {
        if( rackScanData != null ) {
            return ("(Rack Scan)");
        } else {
            return vesselBarcode;
        }
    }

    /**
     * Control table layout, either a container(row and column headers) or a single vessel
     */
    public boolean isTubeLayout(){
        return isTube;
    }

    public void setVesselBarcode(String vesselBarcode) {
        this.vesselBarcode = vesselBarcode;
    }

    public void setRackScanData(String rackScanData) {
        this.rackScanData = rackScanData;
    }

    public String getRackScanData() {
        if( rackScanData != null ) {
            return rackScanData;
        } else {
            return "";
        }
    }

    public VesselGeometry getVesselGeometry() {
        return vesselGeometry;
    }

    public void setRedisplayVesselBarcode(String redisplayVesselBarcode) {
        this.redisplayVesselBarcode = redisplayVesselBarcode;
    }

    public void setAbandonActionJson(String abandonActionJson) {
        this.abandonActionJson = abandonActionJson;
    }

    public void setAbandonActionReason(
            AbandonVessel.Reason abandonActionReason) {
        this.abandonActionReason = abandonActionReason;
    }

    /**
     * Generate and return JSON layout map (assume test via isDoLayout() before calling)
     */
    public String getLayoutMap(){
        String layoutMapJson;
        try {
            layoutMapJson = LAYOUT_JSON_WRITER.writeValueAsString(layoutMap);
        } catch (Exception ex ) {
            log.error(ex);
            addGlobalValidationError("Failure building layout map: " + ex.getMessage() );
            return "{}";
        }
        return layoutMapJson;
    }

    /**
     * Control display of vessel layout after search and abandon action functionality
     */
    public boolean isDoLayout() {
        return layoutMap != null;
    }

    /**
     * Is data from a rack scan?
     */
    public boolean isLayoutFromRackScan() {
        return rackScanData != null;
    }

    public AbandonVessel.Reason[] getReasonCodes() { return AbandonVessel.Reason.values(); }

    /**
     * Workhorse method to generate browser container layout when all positions are stored in Mercury
     * @param positionVesselMap Vessels and positions to present, a tube is represented by pseudo-position _1_1
     */
    private void buildLayout( Map<VesselPosition, LabVessel> positionVesselMap ) {
        buildLayout( positionVesselMap, Collections.EMPTY_MAP );
    }

    /**
     * Workhorse method to generate browser container layout
     * @param positionVesselMap Vessels and positions to present, a tube is represented by pseudo-position _1_1
     * @param positionErrorMap Problem cells, typically when a rack scan includes barcodes which aren't stored in Mercury
     */
    private void buildLayout( Map<VesselPosition, LabVessel> positionVesselMap
            , Map<VesselPosition, String> positionErrorMap ) {

        List<AbandonCell> abandonCells = new ArrayList<>();
        for( Map.Entry<VesselPosition, LabVessel> mapEntry : positionVesselMap.entrySet()) {
            VesselPosition vesselPosition = mapEntry.getKey();
            LabVessel labVessel = mapEntry.getValue();
            VesselGeometry.RowColumn rowColumn = vesselGeometry.getRowColumnForVesselPosition(vesselPosition);
            Pair<Integer,Integer> rowAndColumn = rowColumn==null?Pair.of(1,1):Pair.of(rowColumn.getRow(), rowColumn.getColumn() );
            AbandonCell abandonCell;
            if( labVessel == null ) {
                if( positionErrorMap.containsKey(vesselPosition)) {
                    // Build error cell
                    abandonCell = new AbandonCell(positionErrorMap.get(vesselPosition), vesselPosition, rowAndColumn.getLeft(), rowAndColumn.getRight() );
                    // Flag as empty to disable functionality
                    abandonCell.setEmpty(true);
                } else {
                    // Build empty cell
                    abandonCell = new AbandonCell(null, vesselPosition, rowAndColumn.getLeft(), rowAndColumn.getRight() );
                }
            } else {
                abandonCell = new AbandonCell( labVessel.getLabel(), vesselPosition, rowAndColumn.getLeft(), rowAndColumn.getRight() );
                if( !labVessel.getAbandonVessels().isEmpty() ) {
                    if( labVessel.getContainerRole() == null ) {
                        abandonCell.setAbandonReason( labVessel.getAbandonVessels().iterator().next().getReason());
                    } else {
                        // Parse for position
                        for( AbandonVessel abandonVessel : labVessel.getAbandonVessels() ) {
                            if( vesselPosition == abandonVessel.getVesselPosition() ) {
                                abandonCell.setAbandonReason( abandonVessel.getReason());
                                break;
                            }
                        }
                    }
                }
            }
            abandonCells.add(abandonCell);
            if( vesselGeometry == VesselGeometry.TUBE ) {
                layoutMap = new LayoutMap("", abandonCells);
            } else {
                layoutMap = new LayoutMap(getVesselBarcode(), abandonCells);
            }
        }
    }

    /**
     * Passed to browser as JSON to generate display layout. </br>
     * Browser creates JavaScript object to post back as JSON for abandon/unabandon cell(s) functionality
     */
    public static class LayoutMap {
        private String containerBarcode;
        private List<AbandonCell> abandonCells;

        public LayoutMap(){}

        public LayoutMap( String containerBarcode, List<AbandonCell> abandonCells ){
            this.containerBarcode = containerBarcode;
            this.abandonCells = abandonCells;
        }

        public String getContainerBarcode() {
            return containerBarcode;
        }

        public void setContainerBarcode(String containerBarcode) {
            this.containerBarcode = containerBarcode;
        }

        public List<AbandonCell> getAbandonCells() {
            return abandonCells;
        }

        public void setAbandonCells(
                List<AbandonCell> abandonCells) {
            this.abandonCells = abandonCells;
        }
    }

    /**
     * A representation of a cell's contents to pass back and forth from browser as JSON
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AbandonCell {
        // Only used if no mapPositionToVessel entries (e.g. StaticPlate)
        private String barcode;
        private boolean isEmpty;
        private VesselPosition vesselPosition;
        private int row;
        private int column;
        private AbandonVessel.Reason abandonReason;

        public AbandonCell(){}

        public AbandonCell( String barcode, VesselPosition vesselPosition, int row, int column ){
            this.barcode = barcode;
            this.vesselPosition = vesselPosition;
            this.row = row;
            this.column = column;
            if( barcode == null || barcode.length() == 0 ) {
                this.isEmpty = true;
                this.barcode = "";
            } else {
                this.isEmpty = false;
            }
        }

        public VesselPosition getVesselPosition() {
            return vesselPosition;
        }

        public void setVesselPosition(VesselPosition vesselPosition) {
            this.vesselPosition = vesselPosition;
        }

        public int getRow() {
            return row;
        }

        public void setRow(int row) {
            this.row = row;
        }

        public int getColumn() {
            return column;
        }

        public void setColumn(int column) {
            this.column = column;
        }

        public String getBarcode() {
            return barcode;
        }

        public void setBarcode(String barcode) {
            this.barcode = barcode;
        }

        public boolean isEmpty() {
            return isEmpty;
        }

        public void setEmpty(boolean empty) {
            isEmpty = empty;
        }

        public boolean isAbandoned() {
            return abandonReason != null;
        }

        public AbandonVessel.Reason getAbandonReason() {
            return abandonReason;
        }

        public void setAbandonReason(AbandonVessel.Reason abandonReason) {
            this.abandonReason = abandonReason;
        }

        public String getAbandonReasonDisplay() {
            return abandonReason==null?"":abandonReason.getDisplayName();
        }

    }
}

package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.weld.util.collections.ArraySet;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abandon / Un-Abandon vessel / plastic logic.
 *
 */

@UrlBinding(value = "/workflow/AbandonVessel.action")
public class AbandonVesselActionBean  extends RackScanActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/vessel.action";
    public static final String VESSEL_SEARCH = "vesselBarcodeSearch";
    public static final String ABANDON_POSITION = "abandonPosition";
    public static final String UN_ABANDON_POSITION = "unAbandonPosition";
    public static final String UN_ABANDON_VESSEL = "unAbandonVessel";
    public static final String ABANDON_VESSEL = "abandonVessel";
    public static final String ABANDON_ALL_POSITIONS = "abandonAllPositions";
    public static final String RACK_SCAN_EVENT = "rackScan";
    private Set<LabVessel> foundVessels = new HashSet<>();
    private static final String SESSION_LIST_PAGE = "/workflow/abandon_vessel.jsp";
    private static final String SESSION_LIST_REDIRECT_PAGE = "/workflow/AbandonVessel.action";
    public static final String PAGE_TITLE = "Abandon Vessel";
    private String resultSummaryString;
    private boolean resultsAvailable = false;
    private boolean isSearchDone = false;
    private String abandonComment;
    private String unAbandonComment;
    private String vesselBarcode;
    private String vesselLabel;
    private String barcode;
    private VesselGeometry vesselGeometry;
    private String vesselPosition;
    private String vesselPositionReason;
    private boolean isMultiplePositions = false;
    private LabVessel labVessel;
    private String searchKey;
    private MessageCollection messageCollection = new MessageCollection();
    private String reasonSelect = new AbandonVessel().getReasonList()[0].getDisplayName();

    @Inject
    private LabVesselDao labVesselDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(VESSEL_SEARCH)
    public Resolution vesselBarcodeSearch() throws Exception {
        doSearch();
        orderResults();
        if(searchKey == null) {
            setSearchDone(false);
            messageCollection.addError("Please provide a barcode");
            addMessages(messageCollection);
        }
        if(!getResultsAvailable()) {
            setSearchDone(false);
            if(getSearchKey() != null) {
                messageCollection.addError("No results found for: " + getSearchKey());
                addMessages(messageCollection);
            }
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    public Resolution vesselSearch() throws Exception {
        doSearch();
        orderResults();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Rack scan handler.
     *
     */
    @HandlesEvent(RACK_SCAN_EVENT)
    public Resolution rackScan() throws Exception {
        scan();
        setRackScanGeometry();
        if(getRackScan() != null) {
            if(!verifyRackUpload())  {
                messageCollection.addError("Unable to parse simulator file");
                addMessages(messageCollection);
            }
            else {
                setRackScanGeometry();
                resultsAvailable = true;
                isSearchDone = true;
                isMultiplePositions = true;
            }
        }

        return new ForwardResolution(SESSION_LIST_PAGE);

    }

    /**
     *
     * Verify basic format of uploaded rack.
     *
     */

    public boolean verifyRackUpload()
    {
        if(rackScan.size() == 0)
            return false;

        boolean emptyRack = true;
        for (Map.Entry<String, String> entry : rackScan.entrySet()) {
            String key = entry.getKey();
            if(entry.getValue().length() > 0)
                emptyRack = false;
            if(!key.equals("rack"))  {
                if(key.length() != 3 ) {
                    return false;
                }
                if(key.length() == 3 ) {
                    boolean isChar = key.substring(0,1).matches("[a-zA-z]");
                    if(!isChar) {
                        return isChar;
                    }
                }
            }
        }
        if(emptyRack) {
            return false;
        }
        return true;
    }
    /**
     *
     * Abandon a specific vessel position.
     *
     */
    @HandlesEvent(ABANDON_POSITION)
    public Resolution abandonPosition() throws Exception {

        setSearchKey(vesselLabel);
        doSearch();
        String vesselPosition = getVesselPosition();
        String reason = getVesselPositionReason();
        String responseLabel = "";
        boolean vesselsFound = false;
        isRackScan(vesselPosition);

        if(reason.equals(reasonSelect)) {
            messageCollection.addError("Please select a reason for abandoning the well.");
            addMessages(messageCollection);
            return vesselSearch();
        }

        for (LabVessel vessel : getFoundVessels()) {
             if(vessel != null && vessel.getLabel() != null) {
                 AbandonVessel abandonVessel = getVesselToAbandon(vessel);
                 //If this is a barcoded tube persist the labvessel and reason in abandon_vessel
                 if(isMatrixTube(vessel)) {
                     abandonVessel.setReason(AbandonVessel.Reason.valueOf(reason));
                 }
                //Even for a barcoded tube we still need to persist the position since this
                //is used to by the .JSP page to display the state of an abandoned tube.
                 abandonVessel.setAbandonedOn(true);
                 AbandonVesselPosition abandonVesselPosition = new AbandonVesselPosition();
                 abandonVesselPosition.setAbandonedOn(true);
                 abandonVesselPosition.setReason(AbandonVessel.Reason.valueOf(reason));
                 abandonVesselPosition.setPosition(vesselPosition);
                 abandonVessel.addAbandonVesselPosition(abandonVesselPosition);
                 vessel.addAbandonedVessel(abandonVessel);
                 vesselsFound = true;
                 responseLabel = vessel.getLabel();
             }
       }

        if(!vesselsFound){
            //Error message for rack scans with missing vessels in the database.
            messageCollection.addError("No valid vessels found.");
            addMessages(messageCollection);
        }
        else {
            labVesselDao.flush();
            messageCollection.addInfo("Position: " + vesselPosition + " For Vessel: " + responseLabel + " Successfully Abandoned. ");
            addMessages(messageCollection);
        }
        return vesselSearch();
    }

    /**
     *
     * If this is a rack scan then add it to the list of found lab vessels.
     *
     */
    private void isRackScan(String vesselPosition)
    {
        if(getRackScan() != null) {
            foundVessels.clear();
            setLabVessel(labVesselDao.findByIdentifier( getRackScan().get(vesselPosition)));
            foundVessels.add(getLabVessel());
        }
    }

    /**
     *
     * Abandon ALL positions in a vessel
     *
     */
    @HandlesEvent(ABANDON_ALL_POSITIONS)
    public RedirectResolution abandonAllPositions() throws Exception {
        setSearchKey(vesselLabel);
        doSearch();
        String reason = getVesselPositionReason();


        if(reason.equals(AbandonVessel.Reason.SELECT.name())) {
            messageCollection.addError("Please select a reason for abandoning all wells.");
            addMessages(messageCollection);
            return new RedirectResolution(SESSION_LIST_REDIRECT_PAGE);
        }

        if(getRackScan() != null) {
            setRackScanGeometry();
            for (VesselPosition position : getVesselGeometry().getVesselPositions()) {
                setLabVessel(labVesselDao.findByIdentifier(getRackScan().get(position.toString())));
                LabVessel vessel = getLabVessel();
                if(vessel != null) {
                    AbandonVessel abandonVessel = getVesselToAbandon(getLabVessel());
                    abandonVessel.setAbandonedOn(true);
                    AbandonVesselPosition abandonVesselPosition = new AbandonVesselPosition();
                    abandonVesselPosition.setAbandonedOn(true);
                    abandonVesselPosition.setReason(AbandonVessel.Reason.valueOf(reason));
                    abandonVesselPosition.setPosition(position.toString());
                    abandonVessel.addAbandonVesselPosition(abandonVesselPosition);
                    vessel.addAbandonedVessel(abandonVessel);
                }
            }
        }
        else {
            for (LabVessel vessel : getFoundVessels()) {
                AbandonVessel abandonVessel = getVesselToAbandon(vessel);
                abandonVessel.setAbandonedOn(true);

                for (VesselPosition position : vessel.getVesselGeometry().getVesselPositions()) {
                    AbandonVesselPosition abandonVesselPosition = new AbandonVesselPosition();
                    abandonVesselPosition.setAbandonedOn(true);
                    abandonVesselPosition.setReason(AbandonVessel.Reason.valueOf(reason));
                    abandonVesselPosition.setPosition(position.toString());
                    abandonVessel.addAbandonVesselPosition(abandonVesselPosition);
                }
                vessel.addAbandonedVessel(abandonVessel);
            }
        }
        labVesselDao.flush();
        messageCollection.addInfo("All Positions Successfully Abandoned." );
        addMessages(messageCollection);
        return new RedirectResolution(SESSION_LIST_REDIRECT_PAGE);
    }

    /**
     *
     * Un-Abandon a specific position.
     *
     */
    @HandlesEvent(UN_ABANDON_POSITION)
    public Resolution unAbandonPosition() throws Exception {
        setSearchKey(vesselLabel);
        doSearch();
        String vesselPosition = getVesselPosition();

        isRackScan(vesselPosition);

        for (LabVessel vessel : getFoundVessels()) {
            if(vessel != null) {
                for (AbandonVessel abandonVessel : vessel.getAbandonVessels()) {
                    for (AbandonVesselPosition abandonVesselPosition : abandonVessel.getAbandonedVesselPosition()) {
                        if (abandonVesselPosition.getPosition().equals(vesselPosition)) {
                            abandonVessel.removeAbandonedWells(abandonVesselPosition);
                            if (vessel.getParentAbandonVessel().getAbandonedVesselPosition().size() == 0) {
                                vessel.removeAbandonedVessel(vessel.getAbandonVessels());
                            }
                            labVesselDao.flush();
                            messageCollection.addInfo("Position Successfully Unabandoned. ");
                            addMessages(messageCollection);
                            return vesselSearch();
                        }
                    }
                }
            }
        }
        labVesselDao.flush();
        return vesselSearch();
    }

    /**
     *
     * Abandon a specific vessel.
     *
     */
    @HandlesEvent(ABANDON_VESSEL)
    public RedirectResolution abandonVessel() throws Exception {

        setSearchKey(vesselLabel);
        doSearch();

        if(abandonComment.equals(AbandonVessel.Reason.SELECT.name())) {
            messageCollection.addError("Please select a reason for abandoning the vessel.");
            addMessages(messageCollection);
            return new RedirectResolution(SESSION_LIST_PAGE);
        }

        for (LabVessel vessel : getFoundVessels()) {
            if(vessel != null) {
                AbandonVessel abandonVessel = getVesselToAbandon(vessel);
                abandonVessel.setReason(AbandonVessel.Reason.valueOf(abandonComment));
                abandonVessel.setAbandonedOn(true);
                vessel.addAbandonedVessel(abandonVessel);
            }
        }
        labVesselDao.flush();
        messageCollection.addInfo("Vessel(s): " + vesselBarcode + " Successfully Abandoned. " );
        addMessages(messageCollection);
        return new RedirectResolution(SESSION_LIST_REDIRECT_PAGE);

    }

    /**
     *
     * Un-Abandon a specific vessel and remove any positions associated with it.
     *
     */
    @HandlesEvent(UN_ABANDON_VESSEL)
    public RedirectResolution unAbandonVessel() throws Exception {

        setSearchKey(vesselLabel);

        if(getRackScan() != null) {
            setRackScanGeometry();
            for (VesselPosition position : getVesselGeometry().getVesselPositions()) {
                LabVessel vessel = labVesselDao.findByIdentifier(getRackScan().get(position.toString()));
                if(vessel != null) {
                    setLabVessel(vessel);
                    getLabVessel().removeAbandonedVessel(getLabVessel().getAbandonVessels());
                }
            }
        }
        else {

            doSearch();
            for (LabVessel vessel : getFoundVessels()) {
                if(vessel != null) {
                    vessel.removeAbandonedVessel(vessel.getAbandonVessels());
                }
            }
        }

        labVesselDao.flush();
        messageCollection.addInfo("Vessel Position(s) Successfully Unabandoned. " );
        addMessages(messageCollection);
        return new RedirectResolution(SESSION_LIST_REDIRECT_PAGE);
    }


    /**
     *
     * Determine if a specific vessel  has been abandoned
     *
     */
    public boolean isVesselAbandoned() {

        if(getRackScan() != null) {
            setRackScanGeometry();
            for (VesselPosition position : getVesselGeometry().getVesselPositions()) {
                setLabVessel(labVesselDao.findByIdentifier(getRackScan().get(position.toString())));
                if(labVessel != null) {
                    if (labVessel.getAbandonVessels().size() > 0) {
                        return true;
                    }
                }
            }
            return false;
        }

        if (labVessel.getAbandonVessels().size() == 0) {
            return false;
        }
        return true;
    }

    /**
     *
     * Determine if a specific vessel position has been abandoned
     *
     */
    public boolean isPositionAbandoned(String vesselPosition)
    {

        isRackScan(vesselPosition);

        for (LabVessel vessel : getFoundVessels()) {
            if(vessel == null) {
                return false;
            }
            for (AbandonVessel abandonVessel : vessel.getAbandonVessels()) {
                for (AbandonVesselPosition abandonVesselPosition : abandonVessel.getAbandonedVesselPosition()) {
                    if(abandonVesselPosition.getPosition().equals(vesselPosition)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     *
     * Retrieve the reason why a specific position was abandoned.
     *
     */
    public String getAbandonReason(String vesselPosition)
    {
        for (LabVessel vessel : getFoundVessels()) {
            if(vessel == null) {
                return reasonSelect;
            }
            for (AbandonVessel abandonVessel : vessel.getAbandonVessels()) {
                for (AbandonVesselPosition abandonVesselPosition : abandonVessel.getAbandonedVesselPosition()) {
                    if(abandonVesselPosition.getPosition().equals(vesselPosition)){
                       return abandonVesselPosition.getReason().getDisplayName();
                    }
                }
            }
        }
        return reasonSelect;
    }

    /**
     * This method orders the results based on the order of strings passed in.
     *
     */
    private void orderResults() {

        Map<String, LabVessel> labelToVessel = new HashMap<>();
        String barcodes = "";
        for (LabVessel vessel : getFoundVessels()) {
            setLabVessel(vessel);
            labelToVessel.put(vessel.getLabel(), vessel);
            barcodes += (vessel.getLabel() + " ");
            this.vesselGeometry = vessel.getVesselGeometry();
            getMultiplePositions(vessel);
        }

        setBarcode(barcodes);

    }

    /**
     * This method creates a list of found vessels for the abandon_vessel.jsp page.
     *
     */
    protected void doSearch()
    {

        //If this is a rack scan there is no need to search.
        if(getRackScan() != null) {
            resultsAvailable = true;
            isSearchDone = true;
            return;
        }

        if(searchKey == null) {
            resultsAvailable = false;
            return;
        }

        List<String> searchList = Arrays.asList(searchKey.split(" "));

        List<LabVessel> labVessels = labVesselDao.findByListIdentifiers(searchList);

        if(labVessels.size() == 0) {
            resultsAvailable = false;
            return;
        }


        if(labVessels.size() > 1 ) {
            for(LabVessel labVessel : labVessels) {
                if(!isMatrixTube(labVessel)) {
                    resultsAvailable = false;
                    messageCollection.addError("You can only bulk abandon matrix tubes.");
                    addMessages(messageCollection);
                    return;
                }
            }
        }

        if(isRackOfTubes(labVessels.get(0)) && getSearchKey() != null){
            resultsAvailable = false;
            messageCollection.addError("You must perform a rack scan to abandon tubes in a rack.");
            addMessages(messageCollection);
            return;
          }

         setFoundVessels(new ArraySet<LabVessel>(labVessels));
         resultsAvailable = true;
         isSearchDone = true;
    }

    /**
     * Serialize and set the rackScan object from the .jsp
     *
     */
    public void setRackMap(String rackMap) throws Exception{
        if(rackMap != null) {
            this.rackScan = new ObjectMapper().readValue(rackMap.replaceAll("~", "\""), LinkedHashMap.class);
        }
    }

    /**
     * Return a serialized rackScan object to the .jsp
     *
     */
    public String getRackMap() throws Exception {
        if(rackScan != null) {
            setResultsAvailable(true);
            return  new JSONObject(rackScan).toString().replace("\"", "~");
        }
        return  null;
    }

    /**
     *
     * Determine if this is a vessel with multiple positions.
     *
     */
    public boolean getMultiplePositions(LabVessel vessel)
    {
        if(getRackScan() != null){
            isMultiplePositions = true;
            setRackScanGeometry();
            return true;
        }

        if(vessel.isMultiplePositions()) {
            isMultiplePositions = true;
            return true;
        }
        else {
            isMultiplePositions = false;
            return false;
        }
    }

    public AbandonVessel getVesselToAbandon(LabVessel vessel)
    {
        if(vessel.getAbandonVessels().size() == 0) {
            return new AbandonVessel();
        }
        return vessel.getParentAbandonVessel();
    }

    /**
     *
     *  If the vessel has > 24 positions, change the CSS set to shrink
     *  the elements so they fit on the screen.
     *
     */
    public String shrinkCss(String cssType)
    {
        if(getVesselGeometry().getCapacity() > 24) {
            return cssType;
        }
        return null;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public Set<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(Set<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public void setSearchDone(boolean searchDone) {
        isSearchDone = searchDone;
    }

    public boolean isSearchDone() {
        return isSearchDone;
    }

    public String getResultSummaryString() {
        return resultSummaryString;
    }

    public void setResultSummaryString(String resultSummaryString) {
        this.resultSummaryString = resultSummaryString;
    }

    public  String getSearchKey() {
        return searchKey;
    }

    public  boolean getResultsAvailable() {
        return resultsAvailable;
    }

    public  void setLabVessel(LabVessel vessel)
    {
        this.labVessel = vessel;
    }

    public LabVessel getLabVessel()
    {
        return this.labVessel;
    }

    public VesselGeometry getVesselGeometry() {
        return this.vesselGeometry;
    }

    public void setVesselGeometry(VesselGeometry vesselGeometry) { this.vesselGeometry = vesselGeometry; }

    public boolean getIsMultiplePositions() {return isMultiplePositions; }

    public String getVesselPositionReason() { return vesselPositionReason;}

    public void setVesselPositionReason(String vesselPositionReason)  { this.vesselPositionReason = vesselPositionReason;  }

    public String getVesselPosition() {
        return vesselPosition;
    }

    public void setVesselPosition(String vesselPosition) {
        this.vesselPosition = vesselPosition;
    }

    public AbandonVessel.Reason[] getReasonCodes() { return new AbandonVessel().getReasonList(); }

    public String getAbandonComment() { return abandonComment; }

    public void setAbandonComment(String abandonComment) {
        this.abandonComment = abandonComment;
    }

    public String getUnAbandonComment() {
        return unAbandonComment;
    }

    public void setResultsAvailable(boolean resultsAvailable) {
        this.resultsAvailable = resultsAvailable;
    }

    public void setUnAbandonComment(String unAbandonComment) {
        this.unAbandonComment = unAbandonComment;
    }

    public String getVesselBarcode() {
        return vesselBarcode;
    }

    public void setVesselBarcode(String vesselBarcode) {
        this.vesselBarcode = vesselBarcode;
    }

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
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

    @Override
    public String getRackScanPageUrl() { return ACTIONBEAN_URL_BINDING; }

    @Override
    public String getPageTitle() { return PAGE_TITLE; }

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
}
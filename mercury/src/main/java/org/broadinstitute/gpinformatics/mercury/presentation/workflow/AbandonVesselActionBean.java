package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;

/**
 * Abandon / Un-Abandon vessel / plastic logic.
 *
 */

@UrlBinding(value = "/workflow/AbandonVessel.action")
public class AbandonVesselActionBean  extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/vessel.action";
    public static final String VESSEL_SEARCH = "vesselSearch";
    public static final String ABANDON_POSITION = "abandonPosition";
    public static final String UN_ABANDON_POSITION = "unAbandonPosition";
    public static final String UN_ABANDON_VESSEL = "unAbandonVessel";
    public static final String ABANDON_VESSEL = "abandonVessel";
    public static final String ABANDON_ALL_POSITIONS = "abandonAllPositions";
    private Set<LabVessel> foundVessels = new HashSet<>();
    private static final String SESSION_LIST_PAGE = "/workflow/abandon_vessel.jsp";
    private static String chipReason = "Vessel Position(s)";
    private String resultSummaryString;
    private boolean resultsAvailable = false;
    private boolean isSearchDone = false;
    private int numSearchTerms;
    private String abandonComment;
    private String unAbandonComment;
    private String vesselBarcode;
    private String vesselLabel;
    public String barcode;
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
    public Resolution vesselSearch() throws Exception {
        doSearch();
        orderResults();
        if(!isResultsAvailable()) {
            setSearchDone(false);
            messageCollection.addError("No results found for: " + getSearchKey());
            addMessages(messageCollection);
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
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

        if(reason.equals(reasonSelect)) {
            messageCollection.addError("Please select a reason for abandoning the well.");
            addMessages(messageCollection);
            return vesselSearch();
        }

        for (LabVessel vessel : getFoundVessels()) {
             AbandonVessel abandonVessel = getVesselToAbandon(vessel);
             abandonVessel.setReason(chipReason);
             abandonVessel.setAbandonedOn(true);
             AbandonVesselPosition abandonVesselPosition = new AbandonVesselPosition();
             abandonVesselPosition.setAbandonedOn(true);
             abandonVesselPosition.setReason(reason);
             abandonVesselPosition.setPosition(vesselPosition);
             abandonVessel.addAbandonVesselPosition(abandonVesselPosition);
             vessel.addAbandonedVessel(abandonVessel);
             labVesselDao.flush();
       }
        messageCollection.addInfo("Position: " + vesselPosition +  " For Vessel: " + vesselLabel + " Successfully Abandoned. " );
        addMessages(messageCollection);
        return vesselSearch();
    }

    /**
     *
     * Abandon ALL positions in a vessel
     *
     */
    @HandlesEvent(ABANDON_ALL_POSITIONS)
    public Resolution abandonAllPositions() throws Exception {
        setSearchKey(vesselLabel);
        doSearch();
        String reason = getVesselPositionReason();

        if(reason.equals(reasonSelect)) {
            messageCollection.addError("Please select a reason for abandoning all wells.");
            addMessages(messageCollection);
            return vesselSearch();
        }

        for (LabVessel vessel : getFoundVessels()) {
            AbandonVessel abandonVessel = getVesselToAbandon(vessel);
            abandonVessel.setReason(chipReason);
            abandonVessel.setAbandonedOn(true);

            for (VesselPosition position : vessel.getVesselGeometry().getVesselPositions())
                {
                    AbandonVesselPosition abandonVesselPosition = new AbandonVesselPosition();
                    abandonVesselPosition.setAbandonedOn(true);
                    abandonVesselPosition.setReason(reason);
                    abandonVesselPosition.setPosition(position.toString());
                    abandonVessel.addAbandonVesselPosition(abandonVesselPosition);
                }
                vessel.addAbandonedVessel(abandonVessel);
                labVesselDao.flush();
            }

        messageCollection.addInfo("All Positions Successfully Abandoned.. " );
        addMessages(messageCollection);
        return vesselSearch();
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

        for (LabVessel vessel : getFoundVessels()) {
            for (AbandonVessel abaondendVessel : vessel.getAbandonVessels()) {
                for (AbandonVesselPosition abandonVesselPosition : abaondendVessel.getAbandonedVesselPosition()) {
                    if (abandonVesselPosition.getPosition().equals(vesselPosition)) {
                        abaondendVessel.removeAbandonedWells(abandonVesselPosition);
                        if (vessel.getParentAbandonVessel().getAbandonedVesselPosition().size() == 0) {
                            vessel.removeAbandonedVessel(vessel.getAbandonVessels());
                        }
                        labVesselDao.flush();
                        messageCollection.addInfo("Position Successfully Unabandoned. " );
                        addMessages(messageCollection);
                        return vesselSearch();
                    }
                }
            }
        }
        return vesselSearch();
    }

    /**
     *
     * Abandon a specific vessel.
     *
     */
    @HandlesEvent(ABANDON_VESSEL)
    public Resolution abandonVessel() throws Exception {

        setSearchKey(vesselLabel);
        doSearch();

        if(abandonComment.equals(reasonSelect)) {
            messageCollection.addError("Please select a reason for abandoning the vessel.");
            addMessages(messageCollection);
            return vesselSearch();
        }

        for (LabVessel vessel : getFoundVessels()) {
            AbandonVessel abandonVessel = getVesselToAbandon(vessel);
            abandonVessel.setReason(abandonComment);
            abandonVessel.setAbandonedOn(true);
            vessel.addAbandonedVessel(abandonVessel);
            labVesselDao.flush();
        }
        messageCollection.addInfo("Vessel: " + vesselBarcode + " Successfully Abandoned. " );
        addMessages(messageCollection);
        return vesselSearch();

    }

    /**
     *
     * Un-Abandon a specific vessel and remove any positions associated with it.
     *
     */
    @HandlesEvent(UN_ABANDON_VESSEL)
    public Resolution unAbandonVessel() throws Exception {

        setSearchKey(vesselLabel);
        doSearch();
        for (LabVessel vessel : getFoundVessels()) {
            vessel.removeAbandonedVessel(vessel.getAbandonVessels());
        }

        labVesselDao.flush();
        messageCollection.addInfo("Vessel: " + vesselLabel + " Successfully Unabandoned. " );
        addMessages(messageCollection);
        return vesselSearch();
    }


    /**
     *
     * Determine if a specific vessel  has been abandoned
     *
     */
    public boolean isVesselAbandoned() {

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
        for (LabVessel vessel : getFoundVessels()) {
            for (AbandonVessel abaondendVessel : vessel.getAbandonVessels()) {
                for (AbandonVesselPosition abandonVesselPosition : abaondendVessel.getAbandonedVesselPosition()) {
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
            for (AbandonVessel abaondendVessel : vessel.getAbandonVessels()) {
                for (AbandonVesselPosition abandonVesselPosition : abaondendVessel.getAbandonedVesselPosition()) {
                    if(abandonVesselPosition.getPosition().equals(vesselPosition)){
                       return abandonVesselPosition.getReason();
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
        for (LabVessel vessel : getFoundVessels()) {
            setLabVessel(vessel);
            labelToVessel.put(vessel.getLabel(), vessel);
            setBarcode(vessel.getLabel());
            this.vesselGeometry = vessel.getVesselGeometry();
            getMultiplePositions(vessel);
        }
        setFoundVessels(new LinkedHashSet<LabVessel>());
        List<String> searchOrder = new ArrayList<String>();
        searchOrder.add(searchKey);
        for (String key : searchOrder) {
            LabVessel labVessel = labelToVessel.get(key);
            if (labVessel != null) {
                getFoundVessels().add(labVessel);
            }
        }
    }

    /**
     * This method creates a list of found vessels for the abandon_vessel.jsp page.
     *
     */
    protected void doSearch()
    {
         List<String> searchList = new ArrayList<String>();
         searchList.add(searchKey);
         LabVessel vessel = labVesselDao.findByIdentifier(searchList.get(0));
         if (vessel != null) {
             foundVessels.add(vessel);
             resultsAvailable = true;
         }
         else {
             resultsAvailable = false;
         }
         isSearchDone = true;
    }

    /**
     *
     * Determine if this is a vessel with multiple positions.
     *
     */
    public boolean getMultiplePositions(LabVessel vessel)
    {
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
        if(labVessel.getGeometrySize() > 24) {
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

    public  boolean isResultsAvailable() {
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
}
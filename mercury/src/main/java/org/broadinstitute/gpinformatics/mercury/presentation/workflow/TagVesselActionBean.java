package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jettison.json.JSONObject;
import java.io.IOException;
import javax.inject.Inject;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Collection;

@UrlBinding(TagVesselActionBean.ACTIONBEAN_URL_BINDING)
public class TagVesselActionBean extends RackScanActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/workflow/TagVessel.action";
    public static final String PAGE_TITLE = "Tag Vessel";
    public static final String TAG_VESSEL = "tagVessel";
    public static final String UNTAG_VESSEL = "unTagVessel";
    public static final String TICKET_SEARCH = "ticketSearch";
    public static final String RACK_SCAN_EVENT = "rackScan";
    /** POSTed from the form, for rack scan. */
    private Integer scanIndex;
    /** POSTed from the form, for rack scan. */
    private Set<TubeFormation> tubeFormations;
    private Set<LabVessel> foundVessels = new HashSet<>();
    private static final String SESSION_LIST_PAGE = "/workflow/tag_vessel.jsp";
    private String reasonSelect = "--Select--";
    private boolean isVesselSearchDone = true;
    private boolean isTicketSearchDone = false;
    private boolean showResults = false;
    public String barcode;
    private String ticketSummary;
    private VesselGeometry vesselGeometry;
    private String vesselPosition;
    private String vesselLabel;
    private String vesselPositionReason;
    private boolean isMultiplePositions = false;
    private LabVessel labVessel;
    private String searchKey;
    private String devTicketKey;
    private MessageCollection messageCollection = new MessageCollection();
    private JiraIssue jiraIssue = null;
    private static final Map<VesselPosition, BarcodedTube> positionToTubeMap = new HashMap<>();

    @Inject
    private JiraService jiraService;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private JiraTicketDao jiraTicketDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Rack scan handler.
     *
     */
    @HandlesEvent(RACK_SCAN_EVENT)
    public Resolution rackScan() throws Exception {

        scan();

        if(!isResultsAvailable()) {
            setVesselSearchDone(false);
            messageCollection.addError("No results found for: " + getSearchKey());
            addMessages(messageCollection);
        }
        else {
            getSubTasks();
            return displayResults();
        }
        return new ForwardResolution(SESSION_LIST_PAGE);

    }

    /**
     * Search for the Jira ticket with the dev conditions.
     *
     */
    @HandlesEvent(TICKET_SEARCH)
    public Resolution ticketSearch() throws Exception {

        getSubTasks();

        if(!isJiraTicketAvailable()) {
            setVesselSearchDone(true);
            setTicketSearchDone(false);
            messageCollection.addError("No results found for Jira Ticket: " + getDevTicketKey());
            addMessages(messageCollection);
        }
        else {
            setVesselSearchDone(true);
            setTicketSearchDone(true);
            setShowResults(false);
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Handles the tag event.
     *
     */
    @HandlesEvent(TAG_VESSEL)
    public Resolution tagVessel() throws Exception {

        if(getVesselPositionReason().equals(reasonSelect)) {
            messageCollection.addError("Please select a dev condition.");
            addMessages(messageCollection);
            return displayResults();
        }

        addJiraTicket();
        return displayResults();
    }

    /**
     * Delete association between vessel and Jira ticket
     *
     */
    @HandlesEvent(UNTAG_VESSEL)
    public Resolution unTagVessel() throws Exception {
        JiraTicket jiraTicket = getVesselJiraTicket(getVesselPosition());
        LabVessel vessel = labVesselDao.findByIdentifier(getVesselLabelByPosition(getVesselPosition()));
        vessel.removeJiraTicket(jiraTicket);
        labVesselDao.persist(vessel);
        jiraTicketDao.remove(jiraTicket);
        jiraTicketDao.flush();
        return displayResults();
    }

    /**
     * Return point for the .jsp page on success.
     *
     */
    private  Resolution displayResults() throws Exception {
        getSubTasks();
        setVesselSearchDone(true);
        setTicketSearchDone(true);
        setShowResults(true);
        setTicketSummary(jiraIssue.getSummary());
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * This method creates a link between the vessel and the jir ticket associated with the dev condition.
     *
     */
    public void addJiraTicket()
    {

        setShowResults(true);
        JiraTicket jiraTicket = new JiraTicket(jiraService,getVesselPositionReason());
        LabVessel vessel = labVesselDao.findByIdentifier(getVesselLabelByPosition(getVesselPosition()));
        vessel.getJiraTickets().add(jiraTicket);
        labVesselDao.persist(vessel);
    }

    /**
     * Return the the Jira ticket associated with vessel position
     *
     */
    public JiraTicket getVesselJiraTicket(String position)
    {
        Collection<JiraTicket> tickets = labVesselDao.findByIdentifier((String) getRackScan().get(position)).getJiraTickets();
        for(JiraTicket ticket : tickets)
        {
            return ticket;
        }
        return null;
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
            return  new JSONObject(rackScan).toString().replace("\"", "~");
        }
        return  null;
    }
    /**
     * Determine if the vessel has a condition tag associated with it.
     *
     */
    public boolean isVesselTagged(String vesselPosition) throws IOException {
        String label = (String) getRackScan().get(vesselPosition);
        LabVessel vessel = labVesselDao.findByIdentifier(label);
        Collection<JiraTicket> jiraTickets = vessel.getJiraTickets();
        if(jiraTickets.size() > 0) {
            return true;
        }
        else{
            return false;
        }
    }

    /**
     * Returns the Jira issue summary of the dev condition by position
     *
     */
    public String getSelected(String vesselPosition) throws IOException {
        String label = (String) getRackScan().get(vesselPosition);
        LabVessel vessel = labVesselDao.findByIdentifier(label);
        Collection<JiraTicket> jiraTickets = vessel.getJiraTickets();
        for(JiraTicket ticket: jiraTickets) {
              JiraIssue jiraIssue =  jiraService.getIssue(ticket.getTicketId());
              return jiraIssue.getSummary();
        }
        return reasonSelect;
    }

    /**
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

    /**
     *  If the vessel has > 24 positions, change the CSS set to shrink
     *  the elements so they fit on the screen.
     *
     */
    public String shrinkCss(String cssType)
    {
        if((getVesselGeometry().getRowCount() * getVesselGeometry().getRowCount()) > 24) {
            return cssType;
        }
        return null;
    }

    /**
     * Used by the jsp page to retrieve list of issue descriptions
     *
     */
    public String[] getJiraIssues()
    {
        List<String> issueList = new ArrayList<String>();

        if(jiraIssue != null) {
            issueList = jiraIssue.getSubTaskSummaries();
            issueList.add(0,"--Select--");
            return issueList.toArray(new String[issueList.size()]);
        }
        else {
            return null;
        }
    }

    /**
     * Used by the jsp page to retrieve list of issue IDs
     *
     */
    public String[] getJiraIssuesIds()
    {
        List<String> keyList = new ArrayList<String>();
        if(jiraIssue != null) {
            keyList = jiraIssue.getSubTaskKeys();
            keyList.add(0,reasonSelect);
            return keyList.toArray(new String[keyList.size()]);
        }
        else {
            return null;
        }
    }

    /**
     *
     * Check to see if the rack scan had results.
     *
     */
    public  boolean isResultsAvailable() {
        if(getRackScan().size() > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Check for an available Jira ticket and set the summary for the .jsp page.
     *
     */
    public  boolean isJiraTicketAvailable() throws Exception {

        if(jiraIssue == null) {
            return false;
        }
        else {
            if(jiraIssue.getSubTaskSummaries().size() > 0) {
                setTicketSummary(jiraIssue.getSummary());
                return true;
            }
            else {
                return false;
            }
        }
    }

    public VesselGeometry getVesselGeometry() {
        RackOfTubes rackOfTubes = new RackOfTubes("rackBarcode", RackOfTubes.RackType.Matrix96);
        this.vesselGeometry = rackOfTubes.getVesselGeometry();
        return this.vesselGeometry;
    }

    public String getBarcode() {
        return barcode;
    }

    public void getSubTasks()  throws Exception  { jiraIssue = jiraService.getIssueInfo(getDevTicketKey(), null);  }

    public void setBarcode(String barcode) { this.barcode = barcode; }

    public void setFoundVessels(Set<LabVessel> foundVessels) { this.foundVessels = foundVessels; }

    public LabVessel getLabVessel()
    {
        return this.labVessel;
    }

    public boolean isVesselSearchDone() {
        return isVesselSearchDone;
    }

    public boolean isTicketSearchDone() {
        return isTicketSearchDone;
    }

    public String getVesselPositionReason() { return vesselPositionReason;}

    public void setVesselPositionReason(String vesselPositionReason)  { this.vesselPositionReason = vesselPositionReason;  }

    public void setVesselPosition(String vesselPosition) { this.vesselPosition = vesselPosition; }

    public String getVesselLabel()  { return this.vesselLabel; }

    public String getVesselPosition() { return vesselPosition; }

    public void setDevTicketKey(String devTicketKey)  { this.devTicketKey = devTicketKey;    }

    public String getDevTicketKey() {  return this.devTicketKey;  }

    public String getTicketSummary() {  return this.ticketSummary; }

    public void setTicketSummary(String ticketSummary) { this.ticketSummary = ticketSummary; }

    public void setVesselSearchDone(boolean vesselSearchDone) { isVesselSearchDone = vesselSearchDone;  }

    public void setTicketSearchDone(boolean ticketSearchDone) { isTicketSearchDone = ticketSearchDone;  }

    public void setSearchKey(String searchKey) { this.searchKey = searchKey;   }

    public boolean getShowResults() {return this.showResults; }

    public void setShowResults(boolean showResults) { this.showResults = showResults; }

    public  void setLabVessel(LabVessel vessel) { this.labVessel = vessel; }

    public  String getSearchKey() { return searchKey; }

    public String getVesselLabelByPosition(String position) { return (String) getRackScan().get(position); }

    @Override
    public String getRackScanPageUrl() { return ACTIONBEAN_URL_BINDING; }

    @Override
    public String getPageTitle() { return PAGE_TITLE; }
}

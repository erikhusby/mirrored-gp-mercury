package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraServiceProducer;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TubeFormationDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import java.io.IOException;
import javax.inject.Inject;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Collection;


@UrlBinding(value = "/workflow/TagVessel.action")
public class TagVesselActionBean   extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/vessel.action";
    public static final String TAG_VESSEL = "tagVessel";
    public static final String UNTAG_VESSEL = "unTagVessel";
    public static final String VESSEL_SEARCH = "vesselSearch";
    public static final String TICKET_SEARCH = "ticketSearch";

    private Set<TubeFormation> tubeFormations;
    private Set<LabVessel> foundVessels = new HashSet<>();
    private static final String SESSION_LIST_PAGE = "/workflow/tag_vessel.jsp";
    private String reasonSelect = "--Select--";
    private boolean resultsAvailable = false;
    private boolean isVesselSearchDone = false;
    private boolean isTicketSearchDone = true;
    private boolean showResults = false;
    public String barcode;
    private VesselGeometry vesselGeometry;
    private String vesselPosition;
    private String vesselPositionReason;
    private boolean isMultiplePositions = false;
    private LabVessel labVessel;
    private String searchKey;
    private String devTicketKey;
    private MessageCollection messageCollection = new MessageCollection();
    private JiraIssue jiraIssue = null;

    @Inject
    private JiraService jiraService;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private TubeFormationDao rackDao;

    @Inject
    private JiraTicketDao jiraTicketDao;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * Search for the lab vessel by barcode.
     *
     */
    @HandlesEvent(VESSEL_SEARCH)
    public Resolution vesselSearch() throws Exception {

        if(getSearchKey() == null)
        {
            setVesselSearchDone(true);
            setTicketSearchDone(false);
            messageCollection.addError("No barcode provided.");
            addMessages(messageCollection);
            return new ForwardResolution(SESSION_LIST_PAGE);
        }

        doSearch();
        if(!isResultsAvailable()) {
            setVesselSearchDone(false);
            messageCollection.addError("No results found for: " + getSearchKey());
            addMessages(messageCollection);
        }
        else {
            setVesselSearchDone(true);
            setTicketSearchDone(false);

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
        doSearch();

        if(!isJiraTicketAvailable()) {
            setVesselSearchDone(true);
            setTicketSearchDone(false);
            messageCollection.addError("No results found for Jira Ticket: " + getDevTicketKey());
            addMessages(messageCollection);
        }
        else {
            setVesselSearchDone(true);
            setTicketSearchDone(true);
            setShowResults(true);
        }
        return new ForwardResolution(SESSION_LIST_PAGE);
    }


    @HandlesEvent(TAG_VESSEL)
    public Resolution tagVessel() throws Exception {

        if(getVesselPositionReason().equals(reasonSelect)) {
            messageCollection.addError("Please select a dev condition.");
            addMessages(messageCollection);
            return ticketSearch();
        }

        doSearch();
        addJiraTicket();
        return ticketSearch();

    }

    /**
     * This method creates a link between the vessel and the jir ticket associated with the dev condition.
     *
     */
    public void addJiraTicket()
    {
        setShowResults(true);
        doSearch();
        String label = getVesselLabel(getVesselPosition());
        String reason = getVesselPositionReason();
        JiraTicket jiraTicket = new JiraTicket(jiraService,reason);
        LabVessel vessel = labVesselDao.findByIdentifier(label);
        vessel.getJiraTickets().add(jiraTicket);
        labVesselDao.persist(vessel);
        jiraTicketDao.persist(jiraTicket);

    }

    /**
     * Delete association between vessel and Jira ticket
     *
     */
    @HandlesEvent(UNTAG_VESSEL)
    public Resolution unTagVessel() throws Exception {

        doSearch();
        String label = getVesselLabel(getVesselPosition());
        JiraTicket jiraTicket = getVesselJiraTicket(getVesselPosition());
        LabVessel vessel = labVesselDao.findByIdentifier(label);
        vessel.removeJiraTicket(jiraTicket);
        labVesselDao.persist(vessel);
        jiraTicketDao.remove(jiraTicket);
        jiraTicketDao.flush();
        setShowResults(true);
        return ticketSearch();
    }

    /**
    * This method creates a list of found vessels for the tags_vessel.jsp page.
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
        isVesselSearchDone = true;
        orderResults();
    }

    /**
     * This method orders the results based on the order of strings passed in.
     *
     */
    private void orderResults() {

        Map<String, LabVessel> labelToVessel = new HashMap<>();
        for (LabVessel vessel : getFoundVessels()) {
            setLabVessel(vessel);
            setTubeFormations(vessel);
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
     * Return barcode by container position.
     *
     */
    public String getVesselLabel(String position)
    {

        for(TubeFormation tube : getTubeFormations()){
            Set<Map.Entry<VesselPosition, BarcodedTube>> map =  tube.getContainerRole().getMapPositionToVessel().entrySet();
            for(Map.Entry<VesselPosition, BarcodedTube> item : map) {
                VesselPosition vesselPosition = item.getKey();
                if(position.equals(vesselPosition.name())) {
                    String label = item.getValue().getLabel();
                    return label;
                }
            }
        }
        return null;
    }

    /**
     * Return the the Jira ticket associated with vessel position
     *
     */
    public JiraTicket getVesselJiraTicket(String position)
    {
        for(TubeFormation tube : getTubeFormations()){
            Set<Map.Entry<VesselPosition, BarcodedTube>> map =  tube.getContainerRole().getMapPositionToVessel().entrySet();
            for(Map.Entry<VesselPosition, BarcodedTube> item : map) {
                VesselPosition vesselPosition = item.getKey();
                if(position.equals(vesselPosition.name())) {
                    Collection<JiraTicket> tickets = item.getValue().getJiraTickets();
                    for(JiraTicket ticket : tickets)
                    {
                      return ticket;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Determine if the vessel has a condition tag associated with it.
     *
     */
    public boolean isVesselTagged(String vesselPosition) throws IOException {
        String label = getVesselLabel(vesselPosition);
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
        String label = getVesselLabel(vesselPosition);
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
        if(labVessel.getGeometrySize() > 24) {
            return cssType;
        }
        return null;
    }

    /**
     *
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
     *
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

    private void setTubeFormations(LabVessel vessel) { tubeFormations =  rackDao.findById(RackOfTubes.class, vessel.getLabVesselId()).getTubeFormations(); }

    private Set<TubeFormation> getTubeFormations() { return  tubeFormations;  }

    public void setVesselSearchDone(boolean vesselSearchDone) { isVesselSearchDone = vesselSearchDone;  }

    public void setTicketSearchDone(boolean ticketSearchDone) { isTicketSearchDone = ticketSearchDone;  }

    public void setSearchKey(String searchKey) { this.searchKey = searchKey;   }

    public boolean getShowResults()
    {
      return this.showResults;
    }

    public void setShowResults(boolean showResults) {
        this.showResults = showResults;
    }

    public Set<LabVessel> getFoundVessels() {return foundVessels; }

    public  void setLabVessel(LabVessel vessel) { this.labVessel = vessel; }

    public  String getSearchKey() { return searchKey; }

    public  boolean isResultsAvailable() { return resultsAvailable; }

    public  boolean isJiraTicketAvailable() {

        if(jiraIssue == null) {
            return false;
        }
        else {
            if(jiraIssue.getSubTaskSummaries().size() > 0) {
                return true;
            }
            else {
                return false;
            }
        }
    }

    public String getBarcode() {
        return barcode;
    }

    public void getSubTasks() throws Exception { jiraIssue = jiraService.getIssueInfo(getDevTicketKey(), null);  }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void setFoundVessels(Set<LabVessel> foundVessels) { this.foundVessels = foundVessels; }

    public LabVessel getLabVessel()
    {
        return this.labVessel;
    }

    public VesselGeometry getVesselGeometry() { return this.vesselGeometry;  }

    public boolean isVesselSearchDone() {
        return isVesselSearchDone;
    }

    public boolean isTicketSearchDone() {
        return isTicketSearchDone;
    }

    public String getVesselPositionReason() { return vesselPositionReason;}

    public void setVesselPositionReason(String vesselPositionReason)  { this.vesselPositionReason = vesselPositionReason;  }

    public void setVesselPosition(String vesselPosition)  { this.vesselPosition = vesselPosition;  }

    public String getVesselPosition() { return vesselPosition; }

    public void setDevTicketKey(String devTicketKey)
    {
        this.devTicketKey = devTicketKey;
    }

    public String getDevTicketKey() {  return this.devTicketKey;  }

    public boolean getIsMultiplePositions() {return isMultiplePositions; }

}

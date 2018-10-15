package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import clover.org.apache.commons.lang.StringUtils;
import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jira.JiraService;
import org.broadinstitute.gpinformatics.infrastructure.jira.issue.JiraIssue;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.mercury.control.dao.project.JiraTicketDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.project.JiraTicket;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.vessel.RackScanActionBean;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jettison.json.JSONObject;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


@UrlBinding(TagVesselActionBean.ACTIONBEAN_URL_BINDING)
public class TagVesselActionBean extends RackScanActionBean {
    private static final Log log = LogFactory.getLog(TagVesselActionBean.class);
    public static final String ACTIONBEAN_URL_BINDING = "/workflow/TagVessel.action";
    public static final String PAGE_TITLE = "Tag Vessel";
    public static final String TAG_VESSEL = "tagVessel";
    public static final String TICKET_SEARCH = "ticketSearch";
    public static final String CREATE_SPREADSHEET = "createSpreadsheet";
    public static final String RACK_SCAN_EVENT = "rackScan";
    private String TagVesselJsonData;
    private static final String SESSION_LIST_PAGE = "/workflow/tag_vessel.jsp";
    private String reasonSelect = "(no condition)";
    private boolean isVesselSearchDone = true;
    private boolean isTicketSearchDone = false;
    private boolean showResults = false;
    private String ticketSummary;
    private VesselGeometry vesselGeometry;
    private String vesselPosition;
    private String jsonData;
    private LabVessel labVessel;
    private String searchKey;
    private String devTicketKey;
    private MessageCollection messageCollection = new MessageCollection();
    private JiraIssue jiraIssue = null;
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M-d-yy");
    private Workbook workbook;
    private String displayCoditions;
    private Map<String, LabVessel> labVessels = new LinkedHashMap<>();
    private Map<JiraTicket, JiraIssue> jiraDetails = new LinkedHashMap<>();

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
        displayExistingConditions();

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

        String json = getJsonData();
        setTagVesselJsonData(json);

        removeTags();

        for(TagVesselJsonData tagJsonVesselData : getTagVesselData()) {
            if(!tagJsonVesselData.getSelection().equals(reasonSelect)) {
                if(tagJsonVesselData.getDevCondition().length() > 1) {
                    addJiraTicket(tagJsonVesselData.getDevCondition(), tagJsonVesselData.getPosition());
                }
            }
            else {
                messageCollection.addError("Error Adding at position: " + tagJsonVesselData.getPosition() + " No condition selected");
            }
        }


        addMessages(messageCollection);
        return displayResults();
    }


    /**
     * Handles the create spreadsheet event
     *
     */
    @HandlesEvent(CREATE_SPREADSHEET)
    public Resolution downloadSpreadSheet()
    {
        try{
            // Makes the spreadsheet.
            workbook = makeSpreadsheet(getTagVesselData());

            // Sets the default filename.
            String filename = DATE_FORMAT.format(new Date()) + "_EXPERIMENT_" + getDevTicketKey()  + ".xls";

            // Streams the spreadsheet back to user.
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                    new ByteArrayInputStream(out.toByteArray()));
            stream.setFilename(filename);

            return stream;

        } catch (IOException e) {
            log.error("Failed to create spreadsheet", e);
        }

        return null;
    }

    /**
     * Create spreadsheet of all dev conditions
     *
     */
    private Workbook makeSpreadsheet( List<TagVesselJsonData> tagJsonData) throws IOException {

        Map<String, Object[][]> sheets = new HashMap<>();

        String[][] devCells = new String[tagJsonData.size() + 1][];

        int rowIndex = 0;

        // Fills in the headers.
        devCells[rowIndex] = new String[] {"Receptacle 2D Barcode", "Position",
                "Experiment", "Condition"};
        ++rowIndex;

        // Fills in the data.
        for (TagVesselJsonData positions : tagJsonData) {
            String barcode = getVesselLabelByPosition(positions.getPosition());
            LabVessel labVessel = labVesselDao.findByIdentifier(barcode);
            if(labVessel != null) {
                List<String> conditions = new ArrayList<>(Arrays.asList(positions.getDevCondition().split(",")));
                List<String> conditionDetials = new ArrayList<>(Arrays.asList(positions.getSelection().trim().split("<br>")));
                conditionDetials.removeAll(Collections.singleton(""));
                String displayConditions = "";
                int conditionIndex = 0;
                for (String devCondition : conditions) {
                    displayConditions += "[" + devCondition + ", " + conditionDetials.get(conditionIndex) + "], ";
                    conditionIndex++;
                }
                devCells[rowIndex] = new String[]{labVessel.getLabel(), positions.getPosition(), getDevTicketKey(), StringUtils.removeEnd(displayConditions, ", ")};
            }
            else {
                devCells[rowIndex] = new String[]{"Vessel " + barcode + " Does Not Exist in Mercury.", positions.getPosition(), "", ""};

            }
            ++rowIndex;
        }

        sheets.put("DevTags", devCells);

        return SpreadsheetCreator.createSpreadsheet(sheets);
    }

    /**
     * Find and return a list of ancestor lab vessels that have associated Jira tickets.
     *
     */
    private List<LabVessel> findAncestors(LabVessel labVessel)
    {
        if(labVessel != null) {
            List<LabVessel> labVessels = new ArrayList<>();
            Collection<LabVessel> ancestorVessels = labVessel.getAncestorVessels();
            for (LabVessel item : ancestorVessels) {
                if (item.getJiraTickets() != null) {
                    labVessels.add(item);
                }
            }
            return labVessels;
        }
        return null;
    }

    /**
     * Called by the .jsp page to get dev conditions based on position of vessel in the rack.
     *
     */
    public String displayDevConditions(String position)
    {
        List<LabVessel> labVessels = findAncestors(findAvailableVesselsByPosition(position));
        return displayBuilder(labVessels);
    }

    /**
     * Build the output for the Jira ticket dev conditions to be inserted as an HTML table for the .jsp page.
     *
     */
    private String displayBuilder(List<LabVessel> labVessels)
    {
        if(labVessels != null) {
            boolean found = false;
            StringBuilder displayBuilder = new StringBuilder();
            String experimentText = "";
            displayBuilder.append("<div><table border=1><tbody><tr>");
            for (LabVessel labVessel : labVessels) {
                if (labVessel != null) {
                    StringBuilder idsBuilder = new StringBuilder();
                    experimentText = null;
                    List<JiraTicket> jiraTickets =  getSortedJirTickets(labVessel);
                    for (JiraTicket ticket : jiraTickets) {
                        String jiraTicket = "";
                        if (ticket != null) {
                            found = true;
                            try {
                                JiraIssue issue = getJiraDetails(ticket);
                                jiraTicket = issue.getSummary();
                                if(jiraTicket == null) {
                                    jiraTicket = "Jira ticket  " + issue.getKey() + " does not exist.";
                                }
                            } catch (IOException e) {
                            }
                            experimentText = idsBuilder.append(" " + "<a target =_blank href=" + ticket.getBrowserUrl() + ">" + ticket.getTicketId() + "</a>").append(" ").append(jiraTicket).toString();
                        }
                    }
                    String label = labVessel.getLabel();
                    if(experimentText != null)
                        displayBuilder.append("<td><label>Experiments for:" + label + "</label><label>" + experimentText + "</label></td>");
                }
            }

            displayBuilder.append("<tr></tbody></table></div>");
            if(!found) {
                return "";
            }
            else {
                return displayBuilder.toString();
            }
        }
        return "";
    }
    /**
     * This returns the Jira tickets in the correct sorted order for display.
     *
     */
    public List<JiraTicket> getSortedJirTickets(LabVessel labVessel) {
        List<JiraTicket> jiraTickets = new ArrayList<>(labVessel.getJiraTickets());
        Collections.sort(jiraTickets, new Comparator<JiraTicket>() {
            @Override
            public int compare(JiraTicket o1, JiraTicket o2) {
                return ObjectUtils.compare(o1.getTicketId(), o2.getTicketId());
            }
        });
        return jiraTickets;
    }

    /**
     * buffer Jira ticket details to increase performance.
     *
     */
    private JiraIssue getJiraDetails(JiraTicket ticket)
    {
        try {
            JiraIssue issue = jiraDetails.get(ticket);
            if( issue == null) {
                issue = ticket.getJiraDetails();
                jiraDetails.put(ticket, issue);
            }
            return issue;
        } catch (IOException e) {
            messageCollection.addError("Error locating Jira Ticket: " + ticket.getTicketId());
        }
        return null;
    }

    /**
     * Delete all association between vessels and Jira tickets
     *
     */
    public void removeTags() throws Exception {
        for(VesselPosition position : getVesselGeometry().getVesselPositions()) {
            JiraTicket jiraTicket = getVesselJiraTicket(position.toString());
            if(jiraTicket != null) {
                LabVessel vessel = findAvailableVesselsByPosition(position.toString());
                vessel.removeJiraTickets();
            }
        }
        labVesselDao.flush();
    }

    /**
     * Return point for the .jsp page on success.
     *
     */
    private  Resolution displayResults() throws Exception {
        getSubTasks();
        displayExistingConditions();
        setVesselSearchDone(true);
        setTicketSearchDone(true);
        setShowResults(true);
        setTicketSummary(jiraIssue.getSummary());

        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    /**
     * This method creates a link between the vessel and the jira ticket associated with the dev condition.
     *
     */
    private void addJiraTicket(String devCondition, String position)
    {

        List<String> devConditionList = new ArrayList<>();
        if(devCondition.contains(",")) {
            devConditionList = Arrays.asList(devCondition.split(","));
        }
        else {
            devConditionList.add(devCondition);
        }
        setShowResults(true);
        LabVessel vessel = findAvailableVesselsByPosition(position);
        for(String devItem : devConditionList) {
            JiraTicket existingTicket = jiraTicketDao.fetchByName(devItem);
            if(vessel == null) {
                messageCollection.addError("Lab Vessel:  " +  getRackScan().get(position) + " does not exist at position: " + position);
                return;
            }
            if(existingTicket == null)
                vessel.addJiraTicket(new JiraTicket(jiraService,devItem));
            else
                vessel.addJiraTicket(existingTicket);
        }
        labVesselDao.persist(vessel);
        messageCollection.addInfo("Successfully Added: " + getRackScan().get(position) + " to position: " + position);

    }

    /**
     * Return the the Jira ticket associated with vessel position
     *
     */
    private JiraTicket getVesselJiraTicket(String position)
    {
        LabVessel labVessel = findAvailableVesselsByPosition(position);
        if(labVessel != null) {
            Collection<JiraTicket> tickets = labVessel.getJiraTickets();
            for(JiraTicket ticket : tickets) {
                return ticket;
            }
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
    public String isVesselTagged(String vesselPosition) throws IOException {
        LabVessel vessel = findAvailableVesselsByPosition(vesselPosition);
        if (vessel != null) {
            Collection<JiraTicket> jiraTickets = vessel.getJiraTickets();
            if (jiraTickets.size() > 0) {
                return "checked";
            } else {
                return null;
            }
        }
    return null;
    }

    /**
     * Returns the Jira issue summary of the dev condition by position
     *
     */
    public String getSelected(String vesselPosition) throws IOException {
        if(vesselPosition != "") {
            LabVessel vessel = findAvailableVesselsByPosition(vesselPosition);
            if (vessel != null) {
                List<JiraTicket> jiraTickets = getSortedJirTickets(vessel);
                String summary = "<br>";
                for (JiraTicket ticket : jiraTickets) {
                    JiraIssue jiraIssue =  getJiraDetails(ticket);
                    summary += jiraIssue.getSummary() + "<br>";
                }
                if (summary.length() > 0)
                    return summary.replace("\"","");
                else {
                    return reasonSelect;
                }
            }
            return reasonSelect;
        }
        return null;
    }

    /**
     * Returns the Jira issue id for a given position.
     *
     */
    public String getSelectedId(String vesselPosition) throws IOException {
        LabVessel vessel = findAvailableVesselsByPosition(vesselPosition);
        if(vessel != null) {
            List<JiraTicket> jiraTickets = getSortedJirTickets(vessel);
            String ticketId = "";
            int count = 0;
            for (JiraTicket ticket : jiraTickets) {
                if (count > 0) {
                    ticketId += ",";
                }
                ticketId += ticket.getTicketId();
                ++count;
            }
            if (ticketId.length() > 0)
                return ticketId;
            else {
                return "cells_" + vesselPosition;
            }
        }
        return "cells_" + vesselPosition;
    }

     /**
     * Used by the jsp page to retrieve list of issue descriptions
     *
     */
    public String[] getJiraIssues()
    {
        List<String> issueList;

        if(jiraIssue != null) {
            issueList = jiraIssue.getSubTaskSummaries();
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
        List<String> keyList;
        if(jiraIssue != null) {
            keyList = jiraIssue.getSubTaskKeys();
            return keyList.toArray(new String[keyList.size()]);
        }
        else {
            return null;
        }
    }

    /**
     * Check to see if the rack scan had results.
     *
     */
    private  boolean isResultsAvailable() {
        if(getRackScan().size() > 0) {
            return true;
        }
        else {
            return false;
        }
    }

    /**
     * Returns the existing Jira ticket subtasks / dev conditions for a given ticket.
     *
     */
    public void displayExistingConditions()
    {
       LinkedHashSet<String> displayElements = new LinkedHashSet<>();
        StringBuilder stringBuilder = new StringBuilder();
        if(jiraIssue != null) {
            for(String devTasks: jiraIssue.getSubTaskKeys()) {
                JiraTicket jiraTicket = jiraTicketDao.fetchByName(devTasks);
                if(jiraTicket != null) {
                  List<LabVessel> labVessels = labVesselDao.findByTickets(jiraTicket.getTicketId());
                    for(LabVessel labVessel : labVessels) {
                        List<LabVessel> ancestorVessels = findAncestors(labVessel);
                        if (ancestorVessels.size() > 0) {
                            displayElements.add(displayBuilder(ancestorVessels));
                        }
                    }
                }
            }
        }
        for(String displayItem : displayElements){
           stringBuilder.append(displayItem);
        }
        setDisplayConditions(stringBuilder.toString());
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

    /**
     * Returns the vessel geometry to render the rack on the .jsp page.
     *
     */
    public VesselGeometry getVesselGeometry() {
        RackOfTubes rackOfTubes = new RackOfTubes("rackBarcode", RackOfTubes.RackType.Matrix96);
        this.vesselGeometry = rackOfTubes.getVesselGeometry();
        return this.vesselGeometry;
    }

    /**
     *  Retrieves and persists a copy of available lab Vessels if it does not already exist.
     *
     */
    private LabVessel findAvailableVesselsByPosition(String vesselPosition) {
        String barcode = getRackScan().get(vesselPosition);
        if(barcode == null) {
            return null;
        }
        if(labVessels.size() > 0) {
            LabVessel labVessel = labVessels.get(barcode);
            return labVessel;
        }
        else {
            List<String> barcodes = new ArrayList<>();
            List<String> positions = new ArrayList<>();
            for (Map.Entry<String, String> e : getRackScan().entrySet()) {
                positions.add(e.getKey());
                barcodes.add(e.getValue());
            }

            labVessels = labVesselDao.findByBarcodes(barcodes);
            LabVessel labVessel = labVessels.get(barcode);
            return labVessel;
        }
    }


    private void getSubTasks()  throws Exception  { jiraIssue = jiraService.getIssueInfo(getDevTicketKey(), null);  }

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

    public String getJsonData() { return jsonData; }


    public  List<TagVesselJsonData> getTagVesselData()
    {
        String json = getJsonData();
        ObjectMapper mapper = new ObjectMapper();
        try{
            List<TagVesselJsonData> mappedJSON = mapper.readValue(json, new TypeReference<List<TagVesselJsonData>>(){});
            if(mappedJSON.size() == 0) {
                messageCollection.addError("No selections found");
            }
            return mappedJSON;

        } catch (IOException e) {
            log.error("Failed to parse JSON data from page: " + json, e);
        }
        return null;
    }

    public void setJsonData(String jsonData) { this.jsonData = jsonData; }

    public void setVesselPosition(String vesselPosition) { this.vesselPosition = vesselPosition; }

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

    public String getSearchKey() { return searchKey; }

    public String getVesselLabelByPosition(String position) { return getRackScan().get(position);  }

    public void setDisplayConditions(String displayConditions) { this.displayCoditions = displayConditions; }

    public String getDisplayConditions() { return displayCoditions;}

    @Override
    public String getRackScanPageUrl() { return ACTIONBEAN_URL_BINDING; }

    @Override
    public String getPageTitle() { return PAGE_TITLE; }

    public void setTagVesselJsonData(String tagJsonVesselData) { TagVesselJsonData = tagJsonVesselData; }

    public String getTagVesselJsonData() {
        if(TagVesselJsonData == null) {
            return null;
        }
        return TagVesselJsonData.replace("\"", "*");
    }
}

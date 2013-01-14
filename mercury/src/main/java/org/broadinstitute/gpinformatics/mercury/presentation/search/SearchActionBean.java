package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.SimpleError;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationErrors;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.presentation.links.JiraLink;
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.*;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding("/search/all.action")
public class SearchActionBean extends CoreActionBean {

    private static final String SESSION_LIST_PAGE = "/search/search.jsp";
    public static final String ACTIONBEAN_URL_BINDING = "/search/all.action";

    private static final String BATCH_CREATE_PAGE = "/search/create_batch.jsp";

    private static final String BATCH_CONFIRM_PAGE = "/search/batch_confirm.jsp";

    public static final String CREATE_BATCH_ACTION = "createBatch";
    public static final String VIEW_ACTION = "startBatch";
    public static final String CONFIRM_ACTION = "confirm";
    public static final String SEARCH_ACTION = "search";
    public static final String SEARCH_BATCH_CANDIDATES_ACTION = "searchBatchCandidates";

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";

    @Inject
    private LabBatchEjb labBatchEjb;
    @Inject
    private UserBean userBean;
    @Inject
    private JiraLink jiraLink;

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private LabBatchDAO labBatchDAO;

    @Validate(required = true, on = {SEARCH_ACTION,SEARCH_BATCH_CANDIDATES_ACTION})
    private String searchKey;

    private String batchLabel;
    private LabBatch batch;

    private List<LabVessel> foundVessels = null;
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private List<LabBatch> foundBatches;

    private boolean resultsAvailable = false;
    private boolean multipleResultTypes = false;
    private Map<String, String> getPDOKeyMap = null;
    private Map<String, String> getIndexesMap = null;

    private List<String> selectedVesselLabels;
    private List<LabVessel> selectedBatchVessels;

    @Validate(required = true, on = {CREATE_BATCH_ACTION})
    private String jiraInputType = EXISTING_TICKET;

    private String jiraTicketId;

    private String important;
    private String description;
    private String summary;
    private Date dueDate;

    /**
     * Initialize the product with the passed in key for display in the form
     */
    @Before(stages = LifecycleStage.BindingAndValidation, on = {CONFIRM_ACTION})
    public void init() {
        batchLabel = getContext().getRequest().getParameter("batchLabel");
        if (StringUtils.isNotBlank(batchLabel)) {
            batch = labBatchDAO.findByBusinessKey(batchLabel);
        }
    }


    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() throws Exception {
        List<String> searchList = cleanInputString(searchKey);

        int count = 0;
        long totalResults = 0l;

        foundVessels = labVesselDao.findByListIdentifiers(searchList);
        if (foundVessels.size() > 0) {
            count++;
            totalResults += foundVessels.size();
        }

        foundSamples = mercurySampleDao.findBySampleKeys(searchList);
        if (foundSamples.size() > 0) {
            count++;
            totalResults += foundSamples.size();
        }

        foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
        if (foundPDOs.size() > 0) {
            count++;
            totalResults += foundPDOs.size();
        }

        foundBatches = labBatchDAO.findByListIdentifier(searchList);
        if (foundBatches.size() > 0) {
            count++;
            totalResults += foundBatches.size();
        }

        // If there is only one result, jump to the item's page, if it has a view page
        if (totalResults == 1) {
            RedirectResolution resolution = getRedirectResolution();
            if (resolution != null) {
                return resolution;
            }
        }

        multipleResultTypes = count > 1;
        resultsAvailable = totalResults > 0;

        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation(ValidationErrors errors) {

        if(selectedVesselLabels == null || selectedVesselLabels.isEmpty()) {
            doBatchSearch();
            errors.add("selectedVesselLabels", new SimpleError("At least one vessel must be selected to create a batch"));
        }

        if(jiraInputType.equals(EXISTING_TICKET)) {
            if(StringUtils.isBlank(jiraTicketId)) {
                doBatchSearch();
                errors.add("jiraTicketId",new SimpleError("An existing Jira ticket key is required"));
            }
        } else {
            if(StringUtils.isBlank(summary)) {
                doBatchSearch();
                errors.add("summary", new SimpleError("You must provide at least a summary to create a Jira Ticket"));
            }
        }
    }

    @HandlesEvent(SEARCH_BATCH_CANDIDATES_ACTION)
    public Resolution searchForBatchCandidates() throws Exception {
        doBatchSearch();
        return new ForwardResolution(BATCH_CREATE_PAGE);
    }

    private void doBatchSearch() {
        List<String> searchList = SearchActionBean.cleanInputString(searchKey);

        foundVessels = labVesselDao.findByListIdentifiers(searchList);

        foundSamples = null;
        foundPDOs = null;
        foundBatches = null;

        long totalResults = foundVessels.size();

        resultsAvailable = totalResults > 0;
    }


    @HandlesEvent(CONFIRM_ACTION)
    public Resolution confirm() {
        return new ForwardResolution(BATCH_CONFIRM_PAGE);
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() throws Exception {
        LabBatch batchObject;

        Set<LabVessel> vesselSet =
                new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

        if (isUseExistingTicket()) {
            /*
               If the user is associating the batch with an existing ticket, just the ticket ID and the set of vessels
               are needed to create the batch
            */

            batchObject =
                    labBatchEjb.createLabBatch(vesselSet, userBean.getBspUser().getUsername(),
                            jiraTicketId.trim());
        } else {

            /*
               If a new ticket is to be created, pass the description, summary, due date and important info in a batch
               object acting as a DTO
            */
            batchObject = new LabBatch(summary.trim(), vesselSet, description, dueDate, important);

            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername());
        }

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been ''{1}''.",
                batchObject.getJiraTicket().getTicketName(), isUseExistingTicket() ? "assigned" : "created"));

        //Forward
        return new RedirectResolution(CreateBatchActionBean.class, CONFIRM_ACTION)
                .addParameter("batchLabel", batchObject.getBatchName());
    }

    private boolean isUseExistingTicket() {
        return jiraInputType.equals(EXISTING_TICKET);
    }

    private RedirectResolution getRedirectResolution() {
        if (foundPDOs.size() > 0) {
            ProductOrder order = foundPDOs.get(0);
            return new RedirectResolution(ProductOrderActionBean.class, VIEW_ACTION).addParameter("productOrder", order.getBusinessKey());
        }

        return null;
    }

    public boolean isMultipleResultTypes() {
        return multipleResultTypes;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public List<MercurySample> getFoundSamples() {
        return foundSamples;
    }

    public void setFoundSamples(List<MercurySample> foundSamples) {
        this.foundSamples = foundSamples;
    }

    public List<ProductOrder> getFoundPDOs() {
        return foundPDOs;
    }

    public void setFoundPDOs(List<ProductOrder> foundPDOs) {
        this.foundPDOs = foundPDOs;
    }

    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public List<LabBatch> getFoundBatches() {
        return foundBatches;
    }

    public void setFoundBatches(List<LabBatch> foundBatches) {
        this.foundBatches = foundBatches;
    }

    /**
     * This method takes a list of search keys turns newlines into commas and splits the individual search keys into
     * a list.
     *
     * @return A list of all the keys from the searchKey string.
     */
    public static List<String> cleanInputString(String searchKey) {
        searchKey = searchKey.replaceAll("\\n", ",");
        String[] keys = searchKey.split(",");
        int index = 0;
        for (String key : keys) {
            keys[index++] = key.trim();
        }
        return Arrays.asList(keys);
    }

    public boolean isResultsAvailable() {
        return resultsAvailable;
    }

    public String getResultTypeStyle() {
        if (multipleResultTypes) {
            return "display:none";
        }

        return "display:block";
    }

    public Map<String, String> getGetPDOKeyMap() {
        if (getPDOKeyMap == null) {

        }

        return getPDOKeyMap;
    }

    public Map<String, String> getIndexesMap() {
        if (getIndexesMap == null) {

        }

        return getIndexesMap;
    }



    public List<String> getSelectedVesselLabels() {
        return selectedVesselLabels;
    }

    public void setSelectedVesselLabels(List<String> selectedVesselLabels) {
        this.selectedVesselLabels = selectedVesselLabels;
    }

    public List<LabVessel> getSelectedBatchVessels() {
        return selectedBatchVessels;
    }

    public void setSelectedBatchVessels(List<LabVessel> selectedBatchVessels) {
        this.selectedBatchVessels = selectedBatchVessels;
    }

    public String getExistingJiraTicketValue() {
        return EXISTING_TICKET;
    }

    public String getNewJiraTicketValue() {
        return NEW_TICKET;
    }

    public String getJiraInputType() {
        return jiraInputType;
    }

    public void setJiraInputType(String jiraInputType) {
        this.jiraInputType = jiraInputType;
    }

    public String getImportant() {
        return important;
    }

    public void setImportant(String important) {
        this.important = important;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    public Date getDueDate() {
        return dueDate;
    }

    public String getBatchLabel() {
        return batchLabel;
    }

    public void setBatchLabel(String batchLabel) {
        this.batchLabel = batchLabel;
    }

    public LabBatch getBatch() {
        return batch;
    }

    public void setBatch(LabBatch batch) {
        this.batch = batch;
    }

    public String getJiraTicketId() {
        return jiraTicketId;
    }

    public void setJiraTicketId(String jiraTicketId) {
        this.jiraTicketId = jiraTicketId;
    }

    /**
     * Get the fully qualified Jira URL.
     *
     * @return URL string
     */
    public String getJiraUrl() {
        if (jiraLink == null) {
            return "";
        }
        return jiraLink.browseUrl();
    }


}

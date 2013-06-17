package org.broadinstitute.gpinformatics.mercury.presentation.search;

import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import java.text.MessageFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author Scott Matthews
 *         Date: 1/8/13
 *         Time: 3:54 PM
 */
@UrlBinding(CreateBatchActionBean.ACTIONBEAN_URL_BINDING)
public class CreateBatchActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/batch/create.action";

    private static final String BATCH_CREATE_PAGE = "/search/create_batch.jsp";

    private static final String BATCH_CONFIRM_PAGE = "/search/batch_confirm.jsp";

    public static final String CREATE_BATCH_ACTION = "createBatch";
    public static final String VIEW_ACTION = "startBatch";
    public static final String CONFIRM_ACTION = "confirm";
    public static final String SEARCH_ACTION = "search";

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";


    @Inject
    private LabBatchEjb labBatchEjb;

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private LabBatchDAO labBatchDAO;

    @Inject
    private UserBean userBean;

    @Validate(required = true, on = {SEARCH_ACTION})
    private String searchKey;

    private String batchLabel;
    private LabBatch batch;

    private List<LabVessel> foundVessels = null;
    private boolean resultsAvailable = false;

    private List<String> selectedVesselLabels;
    private List<LabVessel> selectedBatchVessels;

    @Validate(required = true, on = {CREATE_BATCH_ACTION})
    private String jiraInputType = EXISTING_TICKET;

    @Validate(required = true, on = CREATE_BATCH_ACTION)
    private String selectedBucket;

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
        return new ForwardResolution(BATCH_CREATE_PAGE);
    }

    @HandlesEvent(CONFIRM_ACTION)
    public Resolution confirm() {
        return new ForwardResolution(BATCH_CONFIRM_PAGE);
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation() {

        if (!getUserBean().isValidJiraUser()) {
            addValidationError("jiraTicketId","You must be A valid Jira user to create an LCSet");
        }

        if(selectedVesselLabels == null || selectedVesselLabels.isEmpty()) {
            addValidationError("selectedVesselLabels", "At least one vessel must be selected to create a batch");
        }

        if(jiraInputType.equals(EXISTING_TICKET)) {
            if(StringUtils.isBlank(jiraTicketId)) {
                addValidationError("jiraTicketId","An existing Jira ticket key is required");
            }
        } else {
            if(StringUtils.isBlank(summary)) {
                addValidationError("summary", "You must provide at least a summary to create a Jira Ticket");
            }
        }
    }

    /**
     * Supports the submission for the page.  Will forward to confirmation page on success
     *
     * @return The resolution
     */
    @HandlesEvent(CREATE_BATCH_ACTION)
    public Resolution createBatch() throws Exception {
        LabBatch batchObject;

        if (isUseExistingTicket()) {
            /*
               If the user is associating the batch with an existing ticket, just the ticket ID and the set of vessels
               are needed to create the batch
            */

//            batchObject =
//                    labBatchEjb.createLabBatch(vesselSet, userBean.getBspUser().getUsername(),
//                            jiraTicketId.trim());
            batchObject = labBatchEjb.createLabBatchAndRemoveFromBucket(selectedVesselLabels,
                    userBean.getBspUser().getUsername(), jiraTicketId.trim(), selectedBucket,
                    LabEvent.UI_EVENT_LOCATION);
        } else {

            Set<LabVessel> vesselSet =
                    new HashSet<LabVessel>(labVesselDao.findByListIdentifiers(selectedVesselLabels));

            /*
               If a new ticket is to be created, pass the description, summary, due date and important info in a batch
               object acting as a DTO
            */
            batchObject = new LabBatch(summary.trim(), vesselSet, LabBatch.LabBatchType.WORKFLOW, description, dueDate,
                    important);

//            labBatchEjb.createLabBatch(batchObject, userBean.getBspUser().getUsername());
            labBatchEjb.createLabBatchAndRemoveFromBucket(batchObject, userBean.getBspUser().getUsername(),
                    selectedBucket, LabEvent.UI_EVENT_LOCATION);
        }

        addMessage(MessageFormat.format("Lab batch ''{0}'' has been ''{1}''.",
                batchObject.getJiraTicket().getTicketName(), isUseExistingTicket()?"assigned":"created"));

        //Forward
        return new RedirectResolution(CreateBatchActionBean.class, CONFIRM_ACTION)
                .addParameter("batchLabel", batchObject.getBatchName());
    }

    private boolean isUseExistingTicket() {
        return jiraInputType.equals(EXISTING_TICKET);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() throws Exception {
        List<String> searchList = SearchActionBean.cleanInputString(searchKey);

        foundVessels = labVesselDao.findByListIdentifiers(searchList);

        long totalResults = foundVessels.size();

        resultsAvailable = totalResults > 0;
        return new ForwardResolution(BATCH_CREATE_PAGE);
    }


    public String getSearchKey() {
        return searchKey;
    }

    public void setSearchKey(String searchKey) {
        this.searchKey = searchKey;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public boolean isResultsAvailable() {
        return resultsAvailable;
    }

    public void setResultsAvailable(boolean resultsAvailable) {
        this.resultsAvailable = resultsAvailable;
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

    public String getSelectedBucket() {
        return selectedBucket;
    }

    public void setSelectedBucket(String selectedBucket) {
        this.selectedBucket = selectedBucket;
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
}

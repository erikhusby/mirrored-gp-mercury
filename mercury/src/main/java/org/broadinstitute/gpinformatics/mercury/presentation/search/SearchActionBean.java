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
import java.util.regex.Pattern;

/**
 * This handles all the needed interface processing elements
 */
@UrlBinding(SearchActionBean.ACTIONBEAN_URL_BINDING)
public class SearchActionBean extends CoreActionBean {
    public static final String ACTIONBEAN_URL_BINDING = "/search/all.action";

    private enum SearchType {
        VESSELS_BY_BARCODE, VESSELS_BY_PDO, VESSELS_BY_SAMPLE_KEY, PDO_BY_KEY, SAMPLES_BY_NAME, BATCH_BY_KEY
    }

    private static final String SEPARATOR = ",";
    private static final Pattern SPLIT_PATTERN = Pattern.compile("[" + SEPARATOR + "\\s]+");

    /**
     * Automatically convert known BSP IDs (SM-, SP-) to uppercase.
     */
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[sS][mMpP]-.*");

    private static final String SESSION_LIST_PAGE = "/search/search.jsp";
    private static final String BATCH_CREATE_PAGE = "/search/create_batch.jsp";
    private static final String BATCH_CONFIRM_PAGE = "/search/batch_confirm.jsp";

    public static final String CREATE_BATCH_ACTION = "createBatch";
    public static final String VIEW_ACTION = "startBatch";
    public static final String CONFIRM_ACTION = "confirm";
    public static final String SEARCH_ACTION = "search";
    public static final String VIEW_PLASTIC_ACTION = "viewPlastic";
    public static final String SEARCH_PLASTIC_ACTION = "searchPlastic";
    public static final String SEARCH_BATCH_CANDIDATES_ACTION = "searchBatchCandidates";

    public static final String EXISTING_TICKET = "existingTicket";
    public static final String NEW_TICKET = "newTicket";

    @Inject
    private LabBatchEjb labBatchEjb;
    @Inject
    private UserBean userBean;

    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private ProductOrderDao productOrderDao;
    @Inject
    private LabBatchDAO labBatchDAO;

    @Validate(required = true, on = {SEARCH_ACTION, SEARCH_BATCH_CANDIDATES_ACTION, SEARCH_PLASTIC_ACTION})
    private String searchKey;

    private String batchLabel;
    private LabBatch batch;

    private Set<LabVessel> foundVessels = new HashSet<LabVessel>();
    private List<MercurySample> foundSamples;
    private List<ProductOrder> foundPDOs;
    private List<LabBatch> foundBatches;

    private boolean resultsAvailable = false;
    private boolean multipleResultTypes = false;
    private Map<String, String> getPDOKeyMap = null;
    private Map<String, String> getIndexesMap = null;

    private List<String> selectedVesselLabels;
    private List<LabVessel> selectedBatchVessels;
    private List<String> selectedBatchLabels;

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

    private void doSearch(SearchType... searchForItems) {
        if (searchForItems.length == 0) {
            searchForItems = SearchType.values();
        }
        List<String> searchList = cleanInputString(searchKey);

        int count = 0;
        long totalResults = 0l;
        for (SearchType searchForItem : searchForItems) {
            switch (searchForItem) {
            case VESSELS_BY_BARCODE:
                foundVessels.addAll(labVesselDao.findByListIdentifiers(searchList));
                if (foundVessels.size() > 0) {
                    count++;
                    totalResults += foundVessels.size();
                }

                break;
            case VESSELS_BY_PDO:
                foundVessels.addAll(labVesselDao.findByPDOKeyList(searchList));
                break;
            case VESSELS_BY_SAMPLE_KEY:
                foundVessels.addAll(labVesselDao.findBySampleKeyList(searchList));
                break;
            case SAMPLES_BY_NAME:
                foundSamples = mercurySampleDao.findBySampleKeys(searchList);
                if (foundSamples.size() > 0) {
                    count++;
                    totalResults += foundSamples.size();
                }
                break;
            case PDO_BY_KEY:
                foundPDOs = productOrderDao.findListByBusinessKeyList(searchList);
                if (foundPDOs.size() > 0) {
                    count++;
                    totalResults += foundPDOs.size();
                }
                break;
            case BATCH_BY_KEY:
                foundBatches = labBatchDAO.findByListIdentifier(searchList);
                if (foundBatches.size() > 0) {
                    count++;
                    totalResults += foundBatches.size();
                }
                break;
            }
        }
        multipleResultTypes = count > 1;
        resultsAvailable = totalResults > 0;
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() throws Exception {
        doSearch();
        return new ForwardResolution(SESSION_LIST_PAGE);
    }

    @ValidationMethod(on = CREATE_BATCH_ACTION)
    public void createBatchValidation(ValidationErrors errors) {

        if (selectedVesselLabels == null || selectedVesselLabels.isEmpty()) {
            doSearch(SearchType.VESSELS_BY_SAMPLE_KEY, SearchType.VESSELS_BY_PDO, SearchType.VESSELS_BY_BARCODE);
            errors.add("selectedVesselLabels",
                    new SimpleError("At least one vessel must be selected to create a batch"));
        }

        if (jiraInputType.equals(EXISTING_TICKET)) {
            if (StringUtils.isBlank(jiraTicketId)) {
                doSearch(SearchType.VESSELS_BY_SAMPLE_KEY, SearchType.VESSELS_BY_PDO, SearchType.VESSELS_BY_BARCODE);
                errors.add("jiraTicketId", new SimpleError("An existing Jira ticket key is required"));
            }
        } else {
            if (StringUtils.isBlank(summary)) {
                doSearch(SearchType.VESSELS_BY_SAMPLE_KEY, SearchType.VESSELS_BY_PDO, SearchType.VESSELS_BY_BARCODE);
                errors.add("summary", new SimpleError("You must provide at least a summary to create a Jira Ticket"));
            }
        }
    }

    @HandlesEvent(SEARCH_BATCH_CANDIDATES_ACTION)
    public Resolution searchForBatchCandidates() throws Exception {
        doSearch(SearchType.VESSELS_BY_SAMPLE_KEY, SearchType.VESSELS_BY_PDO, SearchType.VESSELS_BY_BARCODE);
        return new ForwardResolution(BATCH_CREATE_PAGE);
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
            batchObject = new LabBatch(summary.trim(), vesselSet, LabBatch.LabBatchType.WORKFLOW, description, dueDate,
                    important);

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
            return new RedirectResolution(ProductOrderActionBean.class,
                    ProductOrderActionBean.VIEW_ACTION).addParameter("productOrder", order.getBusinessKey());
        }

        return null;
    }

    public boolean isMultipleResultTypes() {
        return multipleResultTypes;
    }

    public Set<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(Set<LabVessel> foundVessels) {
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

    public static List<String> cleanInputString(String searchKey) {
        return cleanInputString(searchKey, false);
    }

    public static List<String> cleanInputStringForSamples(String searchKey) {
        return cleanInputString(searchKey, true);
    }

    /**
     * This method takes a list of search keys turns newlines into commas and splits the individual search keys into
     * a list.
     *
     * @return A list of all the keys from the searchKey string.
     */
    private static List<String> cleanInputString(String searchKey, boolean includeSampleFixup) {
        if (searchKey == null) {
            return Collections.emptyList();
        }

        String[] valueArray = SPLIT_PATTERN.split(searchKey, 0);
        if (valueArray.length == 1 && valueArray[0].isEmpty()) {
            // Handle empty string case.
            valueArray = new String[0];
        }

        List<String> sampleIds = new ArrayList<String>(valueArray.length);
        for (String value : valueArray) {
            if (!StringUtils.isBlank(value)) {
                value = value.trim();
                if (includeSampleFixup && UPPERCASE_PATTERN.matcher(value).matches()) {
                    value = value.toUpperCase();
                }
                sampleIds.add(value);
            }
        }

        return sampleIds;
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

    public List<String> getSelectedBatchLabels() {
        return selectedBatchLabels;
    }

    public void setSelectedBatchLabels(List<String> selectedBatchLabels) {
        this.selectedBatchLabels = selectedBatchLabels;
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

}

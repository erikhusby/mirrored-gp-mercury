package org.broadinstitute.gpinformatics.mercury.presentation.queue;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnEntity;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableList;
import org.broadinstitute.gpinformatics.infrastructure.columns.ConfigurableListFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.ConfigurableSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.QueueEntitySearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchDefinitionFactory;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchInstance;
import org.broadinstitute.gpinformatics.infrastructure.search.queue.DNAQuantQueueSearchTerms;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.QueueGroupingDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import javax.inject.Inject;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * ActionBean for interacting with Queues.
 */
@UrlBinding(QueueActionBean.QUEUE_QUEUE_ACTION)
public class QueueActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(QueueActionBean.class);
    private static final String SPREADSHEET_FILENAME = "_queue_data_dump.xls";

    public static final String QUEUE_QUEUE_ACTION = "/queue/Queue.action";

    private QueueType queueType;

    private GenericQueue queue;

    private Long queueGroupingId;
    private String newGroupName;
    private Integer positionToMoveTo;
    private String excludeVessels;
    private String enqueueSampleIds;

    private QueueGrouping queueGrouping;

    private Map<Long, BspUser> userIdToUsername = new HashMap<>();

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private GenericQueueDao queueDao;

    @Inject
    private QueueGroupingDao queueGroupingDao;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private BSPUserList userList;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ConfigurableListFactory configurableListFactory;

    private static final String READABLE_TEXT = "Manually added on ";
    private Map<String, SampleData> sampleIdToSampleData;
    private Map<Long, String> labVesselIdToSampleId;
    private Map<Long, MercurySample> labVesselIdToMercurySample;
    private QueueSpecialization queueSpecialization;
    private int totalInQueue;
    private int totalNeedRework;
    private Map<Long, Integer> remainingEntitiesInQueue;
    private Map<Long, Integer> totalEntitiesInQueue;
    private Map<QueuePriority, Integer> entitiesQueuedByPriority;
    private List<QueueGrouping> queueGroupings;

    // todo this is actually going to need to be changed somehow to be loaded based off the
    private String selectedSearchTermType;
    private String selectedSearchTermValues;
    private Set<String> searchValuesNotFound;

    /**
     * Search results in column set chosen by user
     */
    private ConfigurableList.ResultList labSearchResultList;

    /**
     * The search the user is creating or running
     */
    private SearchInstance searchInstance;
    private ConfigurableSearchDefinition configurableSearchDef;

    private String entityName;

    /**
     * Which set of columns to use for displaying results
     */
    private String columnSetName;

    /**
     * For multi-page results, the Hibernate property to order by
     */
    private String dbSortPath;

    /**
     * Prefix for search instance session key
     */
    public static final String SEARCH_INSTANCE_PREFIX = "searchInstance_";

    /**
     * HTTP session key for search parameters, used in re-sorting results columns
     */
    private String sessionKey;

    /**
     * Prefix for pagination session key
     */
    public static final String PAGINATION_PREFIX = "pagination_";

    public SearchInstance getSearchInstance() {
        return searchInstance;
    }

    public String getSelectedSearchTermType() {
        return selectedSearchTermType;
    }

    public void setSelectedSearchTermType(String selectedSearchTermType) {
        this.selectedSearchTermType = selectedSearchTermType;
    }

    public String getSelectedSearchTermValues() {
        return selectedSearchTermValues;
    }

    public void setSelectedSearchTermValues(String selectedSearchTermValues) {
        this.selectedSearchTermValues = selectedSearchTermValues;
    }

    public Set<String> getAllowedDisplaySearchTerms() {
        return QueueEntitySearchDefinition.QUEUE_ENTITY_SEARCH_TERMS.getAllowedDisplaySearchTerms();
    }

    public String getEntityName() {
        return entityName;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getColumnSetName() {
        return columnSetName;
    }

    public void setColumnSetName(String columnSetName) {
        this.columnSetName = columnSetName;
    }

    public String getDownloadColumnSets() {
        return columnSetName;
    }

    public String getDbSortPath() {
        return dbSortPath;
    }

    public void setDbSortPath(String dbSortPath) {
        this.dbSortPath = dbSortPath;
    }

    /**
     * Utility enum usued internally in search feature to easily track what the 'not found' terms are tracked by.
     */
    private enum FinderFlag {
        BARCODE("Barcode"),
        MERCURY_SAMPLE_ID("Mercury Sample ID"),
        CONTAINER_BARCODE("Container barcode");

        private final String text;

        FinderFlag(String text) {
            this.text = text;
        }
        String getText() { return this.text; }
    }

    /**
     * Perform a search of the corresponding queue
     * @return
     */
    @HandlesEvent("searchQueue")
    public Resolution searchQueue() {

        // Construct the search instance.
        if (sessionKey == null) {
            searchInstance = new SearchInstance();

            // Save the searchInstance, in case the user re-sorts the results
            sessionKey = Integer.toString(new Random().nextInt(10000));
            getContext().getRequest().getSession()
                    .setAttribute(SEARCH_INSTANCE_PREFIX + sessionKey, searchInstance);
        } else {
            // We're resorting results we retrieved previously
            // Get saved form parameters, so the JSP re-renders correctly
            searchInstance = (SearchInstance) getContext().getRequest().getSession()
                    .getAttribute(SEARCH_INSTANCE_PREFIX + sessionKey);
        }

        entityName = ColumnEntity.LAB_VESSEL.getEntityName();
        configurableSearchDef = SearchDefinitionFactory.getForEntity(entityName);

        if (selectedSearchTermType != null) {
            if (StringUtils.isNotBlank(selectedSearchTermValues)) {
                SearchInstance.SearchValue userSelectedTerm =
                        searchInstance.addTopLevelTerm(selectedSearchTermType, configurableSearchDef);
                userSelectedTerm.setOperator(SearchInstance.Operator.IN);
                userSelectedTerm.setValues(Collections.singletonList(selectedSearchTermValues));

                // Check for vessels specifically in DNA Quant queue.
                SearchInstance.SearchValue queue_type = searchInstance.addTopLevelTerm("Queue Type", configurableSearchDef);
                queue_type.setOperator(SearchInstance.Operator.EQUALS);
                queue_type.setValues(Collections.singletonList(queueType.toString()));
                queue_type.setIncludeInResults(false);

                searchInstance.getPredefinedViewColumns().add(DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.NEAREST_SAMPLE_ID.getTerm());
                searchInstance.getPredefinedViewColumns().add(DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.BARCODE.getTerm());
                searchInstance.getPredefinedViewColumns().add(DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.CONTAINER_INFO.getTerm());
                // Note that in order for this search to work in all queues the corresponding queue type search definition MUST have a term named
                // in the format of '[queue type] Entity Status' e.g. 'DNA Quant Entity Status'.
                searchInstance.getPredefinedViewColumns().add(queueType.getTextName() + " Entity Status"); // Used to determine if the sample is active in the queue or not.

                searchInstance.establishRelationships(configurableSearchDef);
                ConfigurableListFactory.FirstPageResults firstPageResults = configurableListFactory.getFirstResultsPage(
                        searchInstance, configurableSearchDef, null, 0, null, "ASC", entityName);
                labSearchResultList = firstPageResults.getResultList();

                List<ConfigurableList.ResultRow> resultRows = labSearchResultList.getResultRows();

                // Split up the values so that we can verify that all the searched values had returned results.
                searchValuesNotFound = new HashSet<>(Arrays.asList(selectedSearchTermValues.split("\\r?\\n")));
                FinderFlag termToFind = FinderFlag.CONTAINER_BARCODE;   // Defaulting to container barcode because that is the most common use.

                // Determine the index for the column we'll specifically be looking for to determine 'searched item found in results'
                if (selectedSearchTermType.compareToIgnoreCase(DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.MERCURY_SAMPLE_ID.getTerm()) == 0) {
                    termToFind = FinderFlag.MERCURY_SAMPLE_ID;
                } else if (selectedSearchTermType.compareToIgnoreCase(DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.BARCODE.getTerm()) == 0) {
                    termToFind = FinderFlag.BARCODE;
                }

                if (resultRows.size() > 0) {

                    List<ConfigurableList.ResultRow> rowsToRemove = new ArrayList<>();
                    int queueStatusPosition = -1;

                    // Determine which search term is being used so that we know what the 'not found' map will be based on.
                    int positionToVerifyNotFoundTerms = 0;

                    for (ConfigurableList.Header header : labSearchResultList.getHeaders()) {
                        String viewHeader = header.getViewHeader();

                        if (viewHeader.compareToIgnoreCase(queueType.getTextName() + " Entity Status") == 0) {
                            queueStatusPosition = header.getOrder();
                        }

                        // Determine the index for the column we'll specifically be looking for to determine 'searched item found in results'
                        // as container barcode is handled differently in the search results.
                        if (termToFind == FinderFlag.CONTAINER_BARCODE && viewHeader.compareToIgnoreCase(
                                DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.CONTAINER_INFO.getTerm()) == 0) {
                            positionToVerifyNotFoundTerms = header.getOrder();
                        } else  if (termToFind == FinderFlag.MERCURY_SAMPLE_ID && viewHeader.compareToIgnoreCase(
                                DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.NEAREST_SAMPLE_ID.getTerm()) == 0) {
                            positionToVerifyNotFoundTerms = header.getOrder();
                        } else if (termToFind == FinderFlag.BARCODE && viewHeader.compareToIgnoreCase(
                                DNAQuantQueueSearchTerms.DNA_QUANT_TERMS.BARCODE.getTerm()) == 0) {
                            positionToVerifyNotFoundTerms = header.getOrder();
                        }
                    }

                    for (ConfigurableList.ResultRow resultRow : resultRows) {
                        int cellCounter = 0;
                        for (String cell : resultRow.getRenderableCells()) {

                            // If we are at the position for the 'queue status' then check to see if it's 'active' or 'repeat'
                            if (cellCounter == queueStatusPosition && StringUtils.isNotBlank(cell)) {
                                if (cell.compareToIgnoreCase(QueueStatus.Active.getName()) == 0
                                    || cell.compareToIgnoreCase(QueueStatus.Repeat.getName()) == 0) {
                                    rowsToRemove.add(resultRow);
                                }
                            }

                            // We need to determine which cell position the term we need to grab is going to be found in.
                            if (termToFind == FinderFlag.CONTAINER_BARCODE) {
                                if (cellCounter == positionToVerifyNotFoundTerms && (StringUtils.isNotBlank(cell))) {
                                    String[] foundContainers = cell.split(" ");
                                    if (foundContainers.length > 0) {

                                        for (String foundContainer : foundContainers) {
                                            // The split string containing the container barcode will have a '/' in it.
                                            // e.g. (000010117154/A01:01/14/2020 12:16:22 S-114121602/H11:01/14/2020 12:17:52 000110117154/H11:01/14/2020 12:18:28)
                                            if (foundContainer.contains("/")) {
                                                int endOfContainerBarcode = foundContainer.indexOf("/");
                                                String barcode = foundContainer.substring(0, endOfContainerBarcode);
                                                searchValuesNotFound.remove(barcode);
                                            }
                                        }
                                    }
                                }
                            } else {
                                if (cellCounter == positionToVerifyNotFoundTerms) {
                                    searchValuesNotFound.remove(cell);
                                }
                            }

                            cellCounter++;
                        }
                    }
                    // Attempt to remove all the rows that we found are in the DNA Quant queue and are 'Active' or 'Repeat'.
                    if (!rowsToRemove.isEmpty()) {
                        resultRows.removeAll(rowsToRemove);
                    }
                } else {
                    // no results found!!!
                    addGlobalValidationError("No Mercury samples were found!");
                }
            } else {
                addGlobalValidationError("You must enter a search value.");
            }
        } else {
            addGlobalValidationError("You must select a search term.");
        }

        return new ForwardResolution("/queue/show_search_results.jsp");
    }

    /**
     * Shows the main Queue page.
     */
    @DefaultHandler
    @HandlesEvent("showQueuePage")
    // Suppressing this as it considers the two for loops over the queued entities to be duplicates, and the variables
    // are different in each place, so we can't extract a method to fix it.
    @SuppressWarnings("Duplicates")
    public Resolution showQueuePage() {

        if (queueType == null) {
            flashMessage(new SimpleMessage("You attempted to load the Queue page without specifying a queue."));
            return new RedirectResolution(SecurityActionBean.HOME_PAGE);
        }

        remainingEntitiesInQueue = new HashMap<>();
        totalEntitiesInQueue = new HashMap<>();
        entitiesQueuedByPriority = new HashMap<>();

        queue = queueEjb.findQueueByType(queueType);
        queueGroupings = queueGroupingDao.findActiveGroupsByQueueType(queueType);
        for (QueueGrouping grouping : queueGroupings) {

            QueuePriority queuePriority = grouping.getQueuePriority();
            Long queueGroupingId = grouping.getQueueGroupingId();

            // Fill in the initial value for both remaining entities within this grouping, and for the entities by
            // priority (if it is a new priority).
            remainingEntitiesInQueue.put(queueGroupingId, 0);
            totalEntitiesInQueue.put(queueGroupingId, 0);
            if (!entitiesQueuedByPriority.containsKey(queuePriority)) {
                entitiesQueuedByPriority.put(queuePriority, 0);
            }

            Map<QueueStatus, Long> entityStatusCounts = queueGroupingDao.getEntityStatusCounts(queueGroupingId);

            // Fill in the proper numbers.
            for (Map.Entry<QueueStatus, Long> entry : entityStatusCounts.entrySet()) {
                totalEntitiesInQueue.put(queueGroupingId, totalEntitiesInQueue.get(queueGroupingId) + entry.getValue().intValue());
                switch (entry.getKey()) {
                    case Repeat:
                        totalNeedRework += entry.getValue().intValue();
                    case Active:
                        totalInQueue += entry.getValue().intValue();
                        entitiesQueuedByPriority.put(queuePriority, entitiesQueuedByPriority.get(queuePriority) + entry.getValue().intValue());
                        remainingEntitiesInQueue.put(queueGroupingId,
                                remainingEntitiesInQueue.get(queueGroupingId) + entry.getValue().intValue());
                        break;
                    // Completed?, Excluded?
                    default:
                }
            }
        }

        return new ForwardResolution("/queue/show_queue.jsp");
    }

    /**
     * View the details of a particular QueueGrouping.
     */
    @HandlesEvent("viewGrouping")
    public Resolution viewGrouping() {
        if (queueGroupingId == null) {
            addGlobalValidationError("Queue Grouping not specified");
            return getSourcePageResolution();
        }

        queueGrouping = queueDao.findById(QueueGrouping.class, queueGroupingId);

        List<Long> userIds = new ArrayList<>(); // todo jmt queried but never updated
        List<LabVessel> labVessels = new ArrayList<>();
        labVesselIdToSampleId = new HashMap<>();
        labVesselIdToMercurySample = new HashMap<>();
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            labVessels.add(queueEntity.getLabVessel());
            // todo jmt add to userIds?
        }

        List<MercurySample> mercurySamples = new ArrayList<>();

        AbstractDataDumpGenerator.loadMercurySampleInformation(labVessels, mercurySamples, labVesselIdToSampleId, labVesselIdToMercurySample);

        sampleIdToSampleData = loadData(mercurySamples);

        for (Long userId : userIds) {
            userIdToUsername.put(userId, userList.getById(userId));
        }

        return new ForwardResolution("/queue/show_queue_grouping.jsp");
    }

    private Map<String, SampleData> loadData(List<MercurySample> mercurySamples) {
        return sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, getSearchColumns());
    }

    private BSPSampleSearchColumn[] getSearchColumns() {
        return new BSPSampleSearchColumn[]{
                BSPSampleSearchColumn.COLLECTION,
                BSPSampleSearchColumn.LOCATION
        };
    }

    /**
     * Rename a grouping - AJAX call!
     */
    @HandlesEvent("renameGroup")
    public Resolution renameGroup() throws JSONException {
        final JSONObject resultJson = new JSONObject();
        final StringBuilder errors = new StringBuilder();
        if (queueGroupingId == null) {
            errors.append("Queue Grouping not specified");
        } else if (StringUtils.isEmpty(newGroupName)) {
            errors.append("New group name not supplied.");
        } else {
            try {
                queueGroupingDao.renameGrouping(queueGroupingId, newGroupName);
            } catch (Exception e) {
                // Wrapped in an EJBException
                if (!(e.getCause() instanceof IllegalArgumentException)) {
                    errors.append("Unexpected error:  ").append(e.getMessage());
                    log.error("Failure renaming queue group", e);
                } else {
                    errors.append(e.getCause().getMessage());
                }
            }
        }

        if (errors.length() > 0) {
            resultJson.put("errors", new String[]{errors.toString()});
        } else {
            resultJson.put("newGroupName", newGroupName);
        }

        return new StreamingResolution("text/json") {
            @Override
            public void stream(HttpServletResponse response) throws Exception {
                ServletOutputStream out = response.getOutputStream();
                out.write(resultJson.toString().getBytes());
                out.close();
            }
        };

    }

    /**
     * Download for just the Grouping data for a particular grouping.
     */
    @HandlesEvent("downloadGroupingData")
    public Resolution downloadGroupingData() {
        try {
            if (queueGroupingId == null) {
                addGlobalValidationError("Queue Grouping not specified");
                return getSourcePageResolution();
            }

            Object[][] rows = queueEjb.generateDataDump(queueDao.findById(QueueGrouping.class, queueGroupingId));

            return streamSpreadsheet(rows);
        } catch (Exception e) {
            addMessage("There was an error generating the data dump. Please contact support.");
            log.error("Failed to generate data dump", e);
            return showQueuePage();
        }
    }

    @NotNull
    private Resolution streamSpreadsheet(Object[][] rows) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            SpreadsheetCreator.createSpreadsheet("Sample Info", rows, out, SpreadsheetCreator.Type.XLS);
        } catch (IOException ioEx) {
            log.error("Failed to create spreadsheet");
            throw new RuntimeException(ioEx);
        }

        StreamingResolution stream = new StreamingResolution(StreamCreatedSpreadsheetUtil.XLS_MIME_TYPE,
                new ByteArrayInputStream(out.toByteArray()));
        stream.setFilename(queueType.getTextName() + SPREADSHEET_FILENAME);
        return stream;
    }

    /**
     * Download for just the Grouping data for a full queue.
     */
    @HandlesEvent("downloadFullQueueData")
    public Resolution downloadFullQueueData() {
        queue = queueEjb.findQueueByType(queueType);
        try {
            Object[][] rows = queueEjb.generateDataDump(queue);

            return streamSpreadsheet(rows);
        } catch (Exception e) {
            addMessage("There was an error generating the data dump. Please contact support.");
            log.error("Failed to generate data dump", e);
            return showQueuePage();
        }
    }

    @HandlesEvent("moveToTop")
    public Resolution moveToTop() {

        queueEjb.moveToTop(queueType, queueGroupingId);

        return showQueuePage();
    }

    @HandlesEvent("moveToBottom")
    public Resolution moveToBottom() {

        queueEjb.moveToBottom(queueType, queueGroupingId);

        return showQueuePage();
    }

    @HandlesEvent("updatePositions")
    public Resolution updatePositions() {
        MessageCollection messageCollection = new MessageCollection();

        queueEjb.reOrderQueue(queueGroupingId, positionToMoveTo, queueType, messageCollection);

        queue = queueEjb.findQueueByType(queueType);
        addMessages(messageCollection);
        return showQueuePage();
    }

    @HandlesEvent("excludeLabVessels")
    public Resolution excludeLabVessels() {
        MessageCollection messageCollection = new MessageCollection();

        queueEjb.excludeItemsById(excludeVessels, queueType, messageCollection);

        queue = queueEjb.findQueueByType(queueType);
        addMessages(messageCollection);
        return showQueuePage();
    }

    @HandlesEvent("enqueueLabVessels")
    public Resolution enqueueLabVessels() {

        MessageCollection messageCollection = new MessageCollection();

        String readableText = READABLE_TEXT + DateUtils.convertDateTimeToString(new Date());
        queueEjb.enqueueBySampleIdList(enqueueSampleIds, queueType, readableText, messageCollection, QueueOrigin.OTHER, queueSpecialization);
        queue = queueEjb.findQueueByType(queueType);
        addMessages(messageCollection);

        return showQueuePage();
    }

    public QueueType getQueueType() {
        return queueType;
    }

    public void setQueueType(QueueType queueType) {
        this.queueType = queueType;
    }

    public GenericQueue getQueue() {
        return queue;
    }

    public void setQueue(GenericQueue queue) {
        this.queue = queue;
    }

    public Long getQueueGroupingId() {
        return queueGroupingId;
    }

    public void setQueueGroupingId(Long queueGroupingId) {
        this.queueGroupingId = queueGroupingId;
    }

    public void setNewGroupName(String newGroupName) {
        this.newGroupName = newGroupName.trim();
    }

    public QueueGrouping getQueueGrouping() {
        return queueGrouping;
    }

    public void setQueueGrouping(QueueGrouping queueGrouping) {
        this.queueGrouping = queueGrouping;
    }

    public Map<Long, BspUser> getUserIdToUsername() {
        return userIdToUsername;
    }

    public void setUserIdToUsername(Map<Long, BspUser> userIdToUsername) {
        this.userIdToUsername = userIdToUsername;
    }

    public Integer getPositionToMoveTo() {
        return positionToMoveTo;
    }

    public void setPositionToMoveTo(Integer positionToMoveTo) {
        this.positionToMoveTo = positionToMoveTo;
    }

    public String getExcludeVessels() {
        return excludeVessels;
    }

    public void setExcludeVessels(String excludeVessels) {
        this.excludeVessels = excludeVessels;
    }

    public String getEnqueueSampleIds() {
        return enqueueSampleIds;
    }

    public void setEnqueueSampleIds(String enqueueSampleIds) {
        this.enqueueSampleIds = enqueueSampleIds;
    }

    public Map<String, SampleData> getSampleIdToSampleData() {
        return sampleIdToSampleData;
    }

    public void setSampleIdToSampleData(Map<String, SampleData> sampleIdToSampleData) {
        this.sampleIdToSampleData = sampleIdToSampleData;
    }

    public Map<Long, String> getLabVesselIdToSampleId() {
        return labVesselIdToSampleId;
    }

    public void setLabVesselIdToSampleId(Map<Long, String> labVesselIdToSampleId) {
        this.labVesselIdToSampleId = labVesselIdToSampleId;
    }

    public QueueSpecialization getQueueSpecialization() {
        return queueSpecialization;
    }

    public void setQueueSpecialization(QueueSpecialization queueSpecialization) {
        this.queueSpecialization = queueSpecialization;
    }

    public Map<Long, MercurySample> getLabVesselIdToMercurySample() {
        return labVesselIdToMercurySample;
    }

    public void setLabVesselIdToMercurySample(Map<Long, MercurySample> labVesselIdToMercurySample) {
        this.labVesselIdToMercurySample = labVesselIdToMercurySample;
    }

    public List<QueueSpecialization> getAllowedQueueSpecializations() {
        return QueueSpecialization.getQueueSpecializationsByQueueType(queueType);
    }

    public int getTotalInQueue() {
        return totalInQueue;
    }

    public int getTotalNeedRework() {
        return totalNeedRework;
    }

    public Map<Long, Integer> getRemainingEntitiesInQueue() {
        return remainingEntitiesInQueue;
    }

    public QueuePriority[] getQueuePriorities() {
        return QueuePriority.values();
    }

    public Map<QueuePriority, Integer> getEntitiesQueuedByPriority() {
        return entitiesQueuedByPriority;
    }

    public Map<Long, Integer> getTotalEntitiesInQueue() {
        return totalEntitiesInQueue;
    }

    public List<QueueGrouping> getQueueGroupings() {
        return queueGroupings;
    }

    public ConfigurableList.ResultList getLabSearchResultList() {
        return labSearchResultList;
    }

    public Set<String> getSearchValuesNotFound() {
        return searchValuesNotFound;
    }

    public void setSearchValuesNotFound(Set<String> searchValuesNotFound) {
        this.searchValuesNotFound = searchValuesNotFound;
    }
}

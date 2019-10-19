package org.broadinstitute.gpinformatics.mercury.presentation.queue;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.SimpleMessage;
import net.sourceforge.stripes.action.StreamingResolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.QueueGroupingDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;
import org.broadinstitute.gpinformatics.mercury.presentation.security.SecurityActionBean;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ActionBean for interacting with Queues.
 */
@UrlBinding("/queue/Queue.action")
public class QueueActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(QueueActionBean.class);
    private static final String SPREADSHEET_FILENAME = "_queue_data_dump.xls";

    private QueueType queueType;

    private GenericQueue queue;

    private Long queueGroupingId;
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
    private BSPUserList userList;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    private static final String READABLE_TEXT = "Manually added on ";
    private Map<String, SampleData> sampleIdToSampleData;
    private Map<Long, String> labVesselIdToSampleId;
    private Map<Long, MercurySample> labVesselIdToMercurySample;
    private QueueSpecialization queueSpecialization;
    private int totalInQueue;
    private int totalNeedRework;
    private Map<Long, Long> remainingEntities = new HashMap<>();
    private Map<QueuePriority, Long> entitiesInQueueByPriority = new HashMap<>();
    private List<QueueGrouping> queueGroupings;

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

        queue = queueEjb.findQueueByType(queueType);
        queueGroupings = queueGroupingDao.findActiveGroupsByQueueType(queueType);
        List<MercurySample> mercurySamples = new ArrayList<>();
        for (QueueGrouping grouping : queueGroupings) {

            QueuePriority queuePriority = grouping.getQueuePriority();

            // Fill in the initial value for both remaining entities within this grouping, and for the entities by
            // priority (if it is a new priority).
            remainingEntities.put(grouping.getQueueGroupingId(), 0L);
            if (!entitiesInQueueByPriority.containsKey(queuePriority)) {
                entitiesInQueueByPriority.put(queuePriority, 0L);
            }
            // Fill in the proper numbers.
            for (QueueEntity queueEntity : grouping.getQueuedEntities()) {
                switch (queueEntity.getQueueStatus()) {
                    case Repeat:
                        totalNeedRework++;
                    case Active:

                        totalInQueue++;
                        entitiesInQueueByPriority.put(queuePriority, entitiesInQueueByPriority.get(queuePriority) + 1);
                        remainingEntities.put(grouping.getQueueGroupingId(),
                                remainingEntities.get(grouping.getQueueGroupingId()) + 1);
                        break;
                    default:
                }
                mercurySamples.addAll(queueEntity.getLabVessel().getMercurySamples());
            }
        }

        Map<String, SampleData> mapIdToData = loadData(mercurySamples);
        for (MercurySample mercurySample : mercurySamples) {
            SampleData sampleData = mapIdToData.get(mercurySample.getSampleKey());
            if (sampleData != null) {
                mercurySample.setSampleData(sampleData);
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
        return new BSPSampleSearchColumn[] {
                BSPSampleSearchColumn.COLLECTION,
                BSPSampleSearchColumn.LOCATION
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
            SpreadsheetCreator.createSpreadsheet("Sample Info", rows, out);
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

    public Map<Long, Long> getRemainingEntities() {
        return remainingEntities;
    }

    public QueuePriority[] getQueuePriorities() {
        return QueuePriority.values();
    }

    public Map<QueuePriority, Long> getEntitiesInQueueByPriority() {
        return entitiesInQueueByPriority;
    }

    public List<QueueGrouping> getQueueGroupings() {
        return queueGroupings;
    }
}

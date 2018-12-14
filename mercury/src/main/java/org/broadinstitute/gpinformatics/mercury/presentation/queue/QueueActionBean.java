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
import org.broadinstitute.gpinformatics.athena.presentation.orders.ProductOrderActionBean;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.StreamCreatedSpreadsheetUtil;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.QueueEjb;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
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

@UrlBinding("/queue/Queue.action")
public class QueueActionBean extends CoreActionBean {
    private static final Log log = LogFactory.getLog(CoreActionBean.class);
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
    private BSPUserList userList;
    private static final String READABLE_TEXT = "Manually added on ";
    private Map<String, SampleData> sampleIdToSampleData;
    private Map<Long, String> labVesselIdToSampleId;
    private Map<Long, MercurySample> labVesselIdToMercurySample;
    private QueueSpecialization queueSpecialization;
    private int totalNeedPico;
    private int totalExomeExpress;
    private int totalClinical;
    private int totalNeedRework;


    @DefaultHandler
    @HandlesEvent("showQueuePage")
    // Suppressing this as it considers the two for loops over the queued entities to be duplicates, and the variables
    // are different in each place, so we can't extract a method to fix it.
    @SuppressWarnings("Duplicates")
    public Resolution showQueuePage() {

        if (queueType == null) {
            flashMessage(new SimpleMessage("You attempted to load the Queue page without specifying a queue."));
            return new RedirectResolution(ProductOrderActionBean.class).addParameter("list", "");
        }

        queue = queueEjb.findQueueByType(queueType);
        for (QueueGrouping grouping : queue.getQueueGroupings()) {

            if (grouping.getQueuePriority() == QueuePriority.EXOME_EXPRESS) {
                for (QueueEntity queueEntity : grouping.getQueuedEntities()) {
                    if (queueEntity.getQueueStatus() == QueueStatus.Active) {
                        totalExomeExpress++;
                        totalNeedPico++;
                    } else if (queueEntity.getQueueStatus() == QueueStatus.Repeat) {
                        totalNeedRework++;
                        totalNeedPico++;
                    }
                }
            }
            if (grouping.getQueuePriority() == QueuePriority.CLIA) {
                for (QueueEntity queueEntity : grouping.getQueuedEntities()) {
                    if (queueEntity.getQueueStatus() == QueueStatus.Active) {
                        totalClinical++;
                        totalNeedPico++;
                    } else if (queueEntity.getQueueStatus() == QueueStatus.Repeat) {
                        totalNeedRework++;
                        totalNeedPico++;
                    }
                }
            }
        }

        return new ForwardResolution("/queue/show_queue.jsp");
    }

    @HandlesEvent("viewGrouping")
    public Resolution viewGrouping() {

        queueGrouping = queueDao.findById(QueueGrouping.class, queueGroupingId);

        List<Long> userIds = new ArrayList<>();
        List<LabVessel> labVessels = new ArrayList<>();
        labVesselIdToSampleId = new HashMap<>();
        labVesselIdToMercurySample = new HashMap<>();
        for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
            if (queueEntity.getCompletedBy() != null) {
                userIds.add(queueEntity.getCompletedBy());
            }
            labVessels.add(queueEntity.getLabVessel());
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
        SampleDataFetcher sampleDataFetcher = ServiceAccessUtility.getBean(SampleDataFetcher.class);
        return sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, getSearchColumns());
    }

    private BSPSampleSearchColumn[] getSearchColumns() {
        return new BSPSampleSearchColumn[] {
                BSPSampleSearchColumn.COLLECTION,
                BSPSampleSearchColumn.LOCATION
        };
    }

    @HandlesEvent("downloadGroupingData")
    public Resolution downloadGroupingData() {
        try {
            Object[][] rows = queueEjb.generateDataDump(queueType, queueDao.findById(QueueGrouping.class, queueGroupingId));

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

    @HandlesEvent("downloadFullQueueData")
    public Resolution downloadFullQueueData() {
        queue = queueEjb.findQueueByType(queueType);
        try {
            Object[][] rows = queueEjb.generateDataDump(queueType, queue);

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

    public int getTotalNeedPico() {
        return totalNeedPico;
    }

    public int getTotalExomeExpress() {
        return totalExomeExpress;
    }

    public int getTotalClinical() {
        return totalClinical;
    }

    public int getTotalNeedRework() {
        return totalNeedRework;
    }
}

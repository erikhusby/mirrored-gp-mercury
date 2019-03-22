package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.QueueValidationHandler;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueContainerRule;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueSpecialization;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.search.SearchActionBean;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * EJB for handling any Queue Related items.
 */
@Stateful
@RequestScoped
public class QueueEjb {

    public QueueEjb() {
    }

    public QueueEjb(GenericQueueDao genericQueueDao, QueueValidationHandler queueValidationHandler) {
        this.genericQueueDao = genericQueueDao;
        this.queueValidationHandler = queueValidationHandler;
    }

    @Inject
    private GenericQueueDao genericQueueDao;

    @Inject
    private LabVesselDao labVesselDao;

    private QueueValidationHandler queueValidationHandler = new QueueValidationHandler();

    /**
     * Adds a list of lab vessel of any type to a queue as a
     * single queue group.  The general intent is for all lab vessels added together to stay together.
     *
     * @param sampleIds             String containing the sample Ids
     * @param queueType             Type of Queue to add the lab vessels to.
     * @param readableText          Text displayed on the queue row for this item.  If none is there, default text will be
     *                              provided.  Recommended that if a single container is utilized, you use the barcode of
     *                              the container lab vessel.
     * @param messageCollection     Messages back to the user.
     * @param queueSpecialization   What, if any QueueSpecialization is known about these lab vessels for the full list
     */
    public void enqueueBySampleIdList(String sampleIds, QueueType queueType, @Nullable String readableText,
                                      @Nonnull MessageCollection messageCollection, QueueOrigin queueOrigin, QueueSpecialization queueSpecialization) {
        final List<String> sampleNames = SearchActionBean.cleanInputStringForSamples(sampleIds.trim().toUpperCase());
        enqueueBySampleIdList(sampleNames, queueType, readableText, messageCollection, queueOrigin, queueSpecialization);
    }

    /**
     * Adds a list of lab vessel of any type to a queue as a
     * single queue group.  The general intent is for all lab vessels added together to stay together.
     *
     * @param sampleIds             List of sample ids to be added to the Queue.
     * @param queueType             Type of Queue to add the lab vessels to.
     * @param readableText          Text displayed on the queue row for this item.  If none is there, default text will be
     *                              provided.  Recommended that if a single container is utilized, you use the barcode of
     *                              the container lab vessel.
     * @param messageCollection     Messages back to the user.
     * @param queueSpecialization   What, if any QueueSpecialization is known about these lab vessels for the full list
     */
    private void enqueueBySampleIdList(List<String> sampleIds, QueueType queueType, @Nullable String readableText,
                                       @Nonnull MessageCollection messageCollection, QueueOrigin queueOrigin, QueueSpecialization queueSpecialization) {
        List<LabVessel> labVessels = labVesselDao.findByUnknownBarcodeTypeList(sampleIds);
        enqueueLabVessels(labVessels, queueType, readableText, messageCollection, queueOrigin, queueSpecialization);
    }

    /**
     * Adds a list of lab vessel of any type to a queue as a
     * single queue group.  The general intent is for all lab vessels added together to stay together.
     *
     * @param vesselList            List of vessels to queue up as a single group.
     * @param readableText          Text displayed on the queue row for this item.  If none is there, default text will be
     *                              provided.  Recommended that if a single container is utilized, you use the barcode of
     *                              the container lab vessel.
     * @param queueType             Type of Queue to add the lab vessels to.
     * @param messageCollection     Messages back to the user.
     * @param queueOrigin           What subsystem of the application did the enqueue originate from?
     * @param queueSpecialization   What, if any QueueSpecialization is known about these lab vessels for the full list
     * @return                      the Database ID of the newly created QueueGrouping.
     */
    public Long enqueueLabVessels(@Nonnull Collection<LabVessel> vesselList,
                                  @Nonnull QueueType queueType, @Nullable String readableText,
                                  @Nonnull MessageCollection messageCollection, QueueOrigin queueOrigin, QueueSpecialization queueSpecialization) {

        GenericQueue genericQueue = findQueueByType(queueType);

        if (genericQueue.getQueueGroupings() == null) {
            genericQueue.setQueueGroupings(new TreeSet<>(QueueGrouping.BY_SORT_ORDER));
        }
        List<Long> vesselIds = getApplicableLabVesselIds(queueType, vesselList);

        boolean isUniqueSetOfActiveVessels = true;

        for (QueueGrouping queueGrouping : genericQueue.getQueueGroupings()) {
            if (queueGrouping.getQueuedEntities().size() == vesselList.size()) {
                // Make a copy of the vessel ids list so we can try to add all the vessel ids and see if there are any differences
                Set<Long> verifyingVesselIds = new HashSet<>(vesselIds);
                for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                    // If the status isn't active, already there is a difference so cut out.
                    if (queueEntity.getQueueStatus() != QueueStatus.Active) {
                        verifyingVesselIds.clear();
                        break;
                    }
                    // try to add the vessel id - if successful it is different list so we can cut out.
                    if (verifyingVesselIds.add(queueEntity.getLabVessel().getLabVesselId())) {
                        break;
                    }
                }
                // Since the # of the entities in the group is the same as the vessels, and the Ids are identical in both
                // then we know it is a duplicate request and can stop trying to enqueue.
                if (verifyingVesselIds.size() == vesselList.size()) {
                    isUniqueSetOfActiveVessels = false;
                }
            }
        }

        // Verify that multiple people haven't submitted EXACTLY duplicate queue groupings.  If 2 people put SM-1234 in
        // queue groupings that don't otherwise match it is fine and SM-1234 will be in the queue 2x.  This is handled,
        // but if 2 people submit SM-1234 & SM-3456  as a group  only one will get in.
        if (isUniqueSetOfActiveVessels) {
            QueueGrouping queueGrouping = createGroupingAndSetInitialOrder(readableText, genericQueue, vesselList, queueSpecialization);
            queueGrouping.setQueueOrigin(queueOrigin);

            genericQueue.getQueueGroupings().add(queueGrouping);
            try {
                queueValidationHandler.validate(vesselList, queueType, messageCollection);
            } catch (Exception e) {
                messageCollection.addWarning("Internal error trying to validate: " + e.getMessage());
            }
            return queueGrouping.getQueueGroupingId();
        }

        return null;
    }

    @NotNull
    private List<Long> getApplicableLabVesselIds(@Nonnull QueueType queueType, @Nonnull Collection<LabVessel> vesselList) {
        List<Long> vesselIds = new ArrayList<>();
        for (LabVessel labVessel : vesselList) {

            if (labVessel.getContainerRole() != null && queueType.getQueueContainerRule() == QueueContainerRule.TUBES_ONLY) {
                for (LabVessel vessel : labVessel.getContainerRole().getMapPositionToVessel().values()) {
                    vesselIds.add(vessel.getLabVesselId());
                }
            } else {
                vesselIds.add(labVessel.getLabVesselId());
            }
        }
        return vesselIds;
    }

    /**
     * Removes Lab Vessels from a queue.  It is expected only to utilize tubes and not full containers in the dequeue
     * process.
     *
     * @param labVessels            Vessels to Dequeue.
     * @param queueType             Queue Type to remove from.
     * @param messageCollection     Messages back to the user.
     * @param dequeueingOptions     Whether you want to follow the default rules, or update the status regardless.
     */
    @SuppressWarnings("WeakerAccess")
    public void dequeueLabVessels(Collection<LabVessel> labVessels, QueueType queueType,
                                  MessageCollection messageCollection, DequeueingOptions dequeueingOptions) {

        List<Long> labVesselIds = getApplicableLabVesselIds(queueType, labVessels);

        // Finds all the Active entities by the vessel Ids
        List<QueueEntity> entitiesByVesselIds = genericQueueDao.findActiveEntitiesByVesselIds(queueType, labVesselIds);

        // Check for completeness, then if somplete update status.
        for (QueueEntity queueEntity : entitiesByVesselIds) {
            if (!queueValidationHandler.isComplete(queueEntity.getLabVessel(), queueType, messageCollection)
                            && dequeueingOptions == DequeueingOptions.DEFAULT_DEQUEUE_RULES) {
                messageCollection.addWarning(queueEntity.getLabVessel().getLabel()
                        + " has been denoted as not yet completed"
                        + " from the " + queueType.getTextName() + " queue.");
            } else {
                updateQueueEntityStatus(messageCollection, queueEntity, QueueStatus.Completed);
            }
        }
    }

    /**
     * Dequeues the Lab Vessels based on a completed LabMetricRun
     *
     * @param labMetricRun          Run to use in dequeueing.
     * @param queueType             Queue Type to remove from.
     * @param messageCollection     Messages back to the user.
     */
    public void dequeueLabVessels(LabMetricRun labMetricRun, QueueType queueType, MessageCollection messageCollection) {
        List<LabVessel> completed = new ArrayList<>();
        List<LabVessel> repeats = new ArrayList<>();

        for (LabMetric labMetric : labMetricRun.getLabMetrics()) {
            switch (labMetric.getLabMetricDecision().getDecision()) {
                case FAIL:
                    // Note:  There is no functional differnce between failling due to low ng, and passing as both are meant to fall out of the queue.
                case PASS:
                case RISK:
                    completed.add(labMetric.getLabVessel());
                    break;
                case BAD_TRIP:
                case OVER_THE_CURVE:
                case REPEAT:
                case RUN_FAILED:
                case TEN_PERCENT_DIFF_REPEAT:
                case NORM:
                    repeats.add(labMetric.getLabVessel());
                    break;

                default:
                    throw new RuntimeException("Unknown Metric Decision.");
            }
        }

        dequeueLabVessels(completed, queueType, messageCollection, DequeueingOptions.OVERRIDE);
        updateLabVesselsToRepeat(repeats, queueType);
    }

    private void updateLabVesselsToRepeat(List<LabVessel> repeats, QueueType queueType) {
        List<Long> labVesselIds = getApplicableLabVesselIds(queueType, repeats);

        // Finds all the Active entities by the vessel Ids
        List<QueueEntity> queueEntities = genericQueueDao.findActiveEntitiesByVesselIds(queueType, labVesselIds);

        for (QueueEntity queueEntity : queueEntities) {
            queueEntity.setQueueStatus(QueueStatus.Repeat);
        }
    }

    /**
     * Changes the ordering of the queue to whatever is passed in by the user.
     *
     * @param queueGroupingId       QueueGroupingID  to reorder.
     * @param positionToMoveTo      Position to move the grouping to.
     * @param queueType             Queue to re-order within.
     * @param messageCollection     Messages back to the user.
     */
    public void reOrderQueue(Long queueGroupingId, Integer positionToMoveTo, QueueType queueType, MessageCollection messageCollection) {

        if (positionToMoveTo == null) {
            messageCollection.addInfo("Failed to make the changes as no position to move to was provided.");
            return;
        }
        GenericQueue genericQueue = findQueueByType(queueType);

        long currentIndex = 1;

        QueueGrouping queueGroupingBeingMoved = null;

        List<QueueGrouping> queueGroupings = new ArrayList<>(genericQueue.getQueueGroupings());

        for (QueueGrouping queueGrouping : queueGroupings) {
            if (queueGrouping.getQueueGroupingId().equals(queueGroupingId)) {
                queueGroupingBeingMoved = queueGrouping;
            }
        }

        if (queueGroupingBeingMoved == null) {
            messageCollection.addError("Error finding the Queue'd item you wish to move within the queue.");
        } else {

            for (QueueGrouping queueGrouping : queueGroupings) {
                if (positionToMoveTo.longValue() == currentIndex) {
                    queueGroupingBeingMoved.setSortOrder(currentIndex++);
                    queueGroupingBeingMoved.setQueuePriority(QueuePriority.ALTERED);
                }

                if (!queueGrouping.getQueueGroupingId().equals(queueGroupingId)) {
                    queueGrouping.setSortOrder(currentIndex++);
                }
            }
        }
    }

    /**
     * Items to remove manually from the Queue.
     *
     * @param labVesselsToExclude       List of Lab vessels to remove
     * @param queueType                 Queue type to remove them from
     * @param messageCollection         Messages back to the user.
     */
    void excludeItems(Collection<? extends LabVessel> labVesselsToExclude, QueueType queueType, MessageCollection messageCollection) {

        List<Long> labVesselIds = new ArrayList<>();
        for (LabVessel labVessel : labVesselsToExclude) {
            labVesselIds.add(labVessel.getLabVesselId());
        }

        List<QueueEntity> queueEntities = genericQueueDao.findEntitiesByVesselIds(queueType, labVesselIds);

        for (QueueEntity queueEntity : queueEntities) {
            updateQueueEntityStatus(messageCollection, queueEntity, QueueStatus.Excluded);
        }

    }

    private void updateQueueEntityStatus(MessageCollection messageCollection, QueueEntity queueEntity, QueueStatus queueStatus) {

        switch (queueStatus) {
            case Completed:
            case Excluded:
                if (queueEntity.getQueueStatus() == QueueStatus.Active) {
                    queueEntity.setQueueStatus(queueStatus);
                } else {
                    messageCollection.addInfo(queueEntity.getLabVessel().getLabel() + " was attempted to be "
                            + queueStatus.name() + " but was not active, it currently is: " + queueEntity.getQueueStatus().name());
                }
                break;
            default:
                throw new RuntimeException("Unexpected update status.");
        }
    }

    /**
     * Creates a queueGrouping and adds it to the queue in the proper placement.
     *
     * @param readableText          Readable text for the Queue Grouping
     * @param genericQueue          Queue to add the new grouping to
     * @param vesselList            List of LabVessels to add the to the queue
     * @param queueSpecialization   Queue Specialization to be set.
     * @return                      Newly created Queue Grouping.
     */
    private QueueGrouping createGroupingAndSetInitialOrder(@Nullable String readableText, GenericQueue genericQueue,
                                                           Collection<LabVessel> vesselList,
                                                           QueueSpecialization queueSpecialization) {

        QueueGrouping queueGrouping = new QueueGrouping(readableText, genericQueue, queueSpecialization);

        queueGrouping.setAssociatedQueue(genericQueue);
        persist(queueGrouping);
        genericQueueDao.flush();

        for (LabVessel labVessel : vesselList) {

            QueueEntity queueEntity = new QueueEntity(queueGrouping, labVessel);
            queueGrouping.getQueuedEntities().add(queueEntity);
            persist(queueEntity);
        }
        setInitialOrder(queueGrouping);
        return queueGrouping;
    }

    private void setInitialOrder(QueueGrouping queueGrouping) {
        try {
            AbstractEnqueueOverride enqueueOverride = queueGrouping.getAssociatedQueue().getQueueType().getEnqueueOverrideClass().newInstance();

            // Find the vessel ids which already have been in the queue.  These would get standard priority.
            List<Long> vesselIds = new ArrayList<>();
            // Grab the vessel is from the queue entity
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                vesselIds.add(queueEntity.getLabVessel().getLabVesselId());

                // grab the vessel id from the root mercury samples
                for (SampleInstanceV2 sampleInstanceV2 : queueEntity.getLabVessel().getSampleInstancesV2()) {
                    for (MercurySample mercurySample : sampleInstanceV2.getRootMercurySamples()) {
                        for (LabVessel labVessel : mercurySample.getLabVessel()) {
                            vesselIds.add(labVessel.getLabVesselId());
                        }
                    }
                }
            }

            // Find the existing entities
            List<QueueEntity> entitiesByVesselIds = genericQueueDao.findEntitiesByVesselIds(queueGrouping.getAssociatedQueue().getQueueType(), vesselIds);

            Set<Long> uniqueVesselIdsAlreadyInQueue = new HashSet<>();
            for (QueueEntity entity : entitiesByVesselIds) {
                uniqueVesselIdsAlreadyInQueue.add(entity.getLabVessel().getLabVesselId());
            }

            enqueueOverride.setInitialOrder(queueGrouping, uniqueVesselIdsAlreadyInQueue);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Moves a particular Queue Grouping to the top of the queue.
     *
     * @param queueType         Queue to move to the top of.
     * @param queueGroupingId   Queue Grouping to move up.
     */
    public void moveToTop(QueueType queueType, Long queueGroupingId) {

        GenericQueue queue = findQueueByType(queueType);

        List<QueueGrouping> groupingsInCurrentOrder = new ArrayList<>(queue.getQueueGroupings());

        long i = 2;
        for (QueueGrouping grouping : groupingsInCurrentOrder) {
            if (grouping.getQueueGroupingId().equals(queueGroupingId)) {
                grouping.setSortOrder(1L);
                break;
            } else {
                grouping.setSortOrder(i++);
            }
        }
    }

    /**
     * Moves a particular Queue Grouping to the bottom of the queue.
     *
     * @param queueType         Queue to move to the bottom of.
     * @param queueGroupingId   Queue Grouping to move down.
     */
    public void moveToBottom(QueueType queueType, Long queueGroupingId) {

        GenericQueue queue = findQueueByType(queueType);

        List<QueueGrouping> groupingsInCurrentOrder = new ArrayList<>(queue.getQueueGroupings());

        long i = 1;

        QueueGrouping groupingToMakeLast = null;
        for (QueueGrouping grouping : groupingsInCurrentOrder) {
            if (grouping.getQueueGroupingId().equals(queueGroupingId)) {
                groupingToMakeLast = grouping;
            } else {
                grouping.setSortOrder(i++);
            }
        }

        if (groupingToMakeLast != null) {
            groupingToMakeLast.setSortOrder(i);
        }
    }

    /**
     * Excludes lab vessels by their sample ids.
     *
     * @param excludeVessels        List of Sample Ids to exclude.
     * @param queueType             Queue To exclude them from.
     * @param messageCollection     Messages back to the user.
     */
    private void excludeItemsById(List<String> excludeVessels, QueueType queueType, MessageCollection messageCollection) {
        List<LabVessel> vessels = labVesselDao.findByUnknownBarcodeTypeList(excludeVessels);

        vessels.addAll(labVesselDao.findByBarcodes(excludeVessels).values());
        vessels.removeAll(Collections.singletonList(null));

        excludeItems(vessels, queueType, messageCollection);
    }

    /**
     * Excludes Lab vessels by a list of some sort of barcode.  We utilize other methods for figuring out what lab
     * vessels and by what type of barcode.
     *
     * @param excludeVessels        Lab vessels seperated in whichever manner SearchActionBean.cleanInputStringForSamples
     *                              uses.
     * @param queueType             Queue the vessels are being excluded from.
     * @param messageCollection     Messages back to the user.
     */
    public void excludeItemsById(String excludeVessels, QueueType queueType, MessageCollection messageCollection) {
        List<String> barcodes = SearchActionBean.cleanInputStringForSamples(excludeVessels.trim().toUpperCase());
        excludeItemsById(barcodes, queueType, messageCollection);
    }

    private void persist(QueueEntity queueEntity) {
        genericQueueDao.persist(queueEntity);
    }

    private void persist(QueueGrouping queueGrouping) {
        genericQueueDao.persist(queueGrouping);
    }

    /**
     * Loads the requested queue by its queue type
     * @param queueType     The Type of the queue you wish to load
     * @return              The Loaded Queue which will include all QueueGroupings which have ACTIVE QueueEntities.
     */
    public GenericQueue findQueueByType(QueueType queueType) {
        return genericQueueDao.findQueueByType(queueType);
    }

    /**
     * Used to generate a 2D array which can be turned into an Excel file.  This is meant to generate a data dump for a
     * single Queue Grouping
     *
     * @param queueGrouping     QueueGrouping to generate a Data Dump for.
     * @return                  Object array containing data meant for an excel file data dump.
     */
    public Object[][] generateDataDump(QueueGrouping queueGrouping) throws Exception {
        try {
            AbstractDataDumpGenerator dataDumpGenerator = queueGrouping.getAssociatedQueue().getQueueType()
                    .getDataDumpGenerator().newInstance();
            return dataDumpGenerator.generateSpreadsheet(queueGrouping);
        } catch (InstantiationException | IllegalAccessException e) {
            throw new Exception(e);
        }
    }

    /**
     * Used to generate a 2D array which can be turned into an excel file.  This is meant to generate a data dump for an
     * entire queue.
     *
     * @param genericQueue      Queue to generate a Data Dump for.
     * @return                  Object array containing data meant for an excel file data dump.
     */
    public Object[][] generateDataDump(GenericQueue genericQueue) throws Exception {
        try {
            AbstractDataDumpGenerator dataDumpGenerator = genericQueue.getQueueType().getDataDumpGenerator().newInstance();
            return dataDumpGenerator.generateSpreadsheet(genericQueue.getQueueGroupings());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new Exception(e);
        }
    }
}

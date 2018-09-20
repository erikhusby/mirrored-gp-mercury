package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.TreeSet;

public class QueueTestFactory {

    private static long id = 1;

    private static GenericQueue getEmptyPicoQueue() {
        GenericQueue genericQueue = new GenericQueue();
        genericQueue.setQueueType(QueueType.PICO);
        genericQueue.setQueueName("Pico Queue");
        genericQueue.setQueueDescription("Pico Queue");
        return genericQueue;
    }

    static GenericQueueDao getEnqueueTestQueue() {

        GenericQueue emptyPicoQueue = getEmptyPicoQueue();
        GenericQueueDao genericQueueDao = Mockito.mock(GenericQueueDao.class);
        Mockito.when(genericQueueDao.findQueueByType(QueueType.PICO)).thenReturn(emptyPicoQueue);

        addAnswerToPersist(genericQueueDao);

        return genericQueueDao;
    }

    private static void addAnswerToPersist(GenericQueueDao genericQueueDao) {
        Mockito.doAnswer(invocation -> {
            if (invocation.getArguments()[0] instanceof  QueueGrouping) {
                QueueGrouping grouping = (QueueGrouping) invocation.getArguments()[0];
                grouping.setQueueGroupingId(id++);
                grouping.setSortOrder(grouping.getQueueGroupingId());
            }
            return null;
        }).when(genericQueueDao).persist(Mockito.any(QueueGrouping.class));
    }

    static GenericQueueDao getDequeueTestQueue(Long labVesselId) {
        GenericQueueDao genericQueueDao = Mockito.mock(GenericQueueDao.class);
        GenericQueue genericQueue = getEmptyPicoQueue();

        genericQueue.setQueueGroupings(new TreeSet<>(QueueGrouping.BY_SORT_ORDER));

        QueueGrouping queueGrouping = new QueueGrouping();
        queueGrouping.setQueueGroupingId(id++);
        queueGrouping.setSortOrder(queueGrouping.getQueueGroupingId());
        queueGrouping.setAssociatedQueue(genericQueue);
        queueGrouping.setQueuedEntities(new ArrayList<>());

        genericQueue.getQueueGroupings().add(queueGrouping);

        LabVessel labVessel = generateLabVessel(labVesselId);

        QueueEntity queueEntity = new QueueEntity(queueGrouping, labVessel);
        queueEntity.setQueueEntityId(id++);
        queueGrouping.getQueuedEntities().add(queueEntity);

        addAnswerToPersist(genericQueueDao);

        Mockito.when(genericQueueDao.findQueueByType(QueueType.PICO)).thenReturn(genericQueue);
        return genericQueueDao;
    }

    @NotNull
    static LabVessel generateLabVessel(Long labVesselId) {
        LabVessel labVessel = Mockito.mock(LabVessel.class);
        Mockito.when(labVessel.getLabVesselId()).thenReturn(labVesselId);
        return labVessel;
    }
}

package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.broadinstitute.gpinformatics.infrastructure.test.TestGroups.DATABASE_FREE;


@Test(singleThreaded = true, groups = DATABASE_FREE)
public class QueueEjbDbFreeTest {

    private static final String BARCODE_1 = "12345";
    private static final String BARCODE_2 = "67890";
    private static final long LAB_VESSEL_ID = 12345;

    @Test(groups = DATABASE_FREE)
    public void testEnqueueLabVessels() {
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getEnqueueTestQueue(), QueueTestFactory.getPicoValidationNoErrors());
        LabVessel labVessel = new BarcodedTube(BARCODE_1);
        LabVessel labVessel2 = new BarcodedTube(BARCODE_2);

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(null, Arrays.asList(labVessel, labVessel2), QueueType.PICO,
                        null, messageCollection);
        GenericQueue picoQueue = queueEjb.findQueueByType(QueueType.PICO);
        int foundItems = 0;
        for (QueueGrouping queueGrouping : picoQueue.getQueueGroupings()) {
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                if (queueEntity.getLabVessel().getLabel().equals(BARCODE_1)) {
                    foundItems++;
                } else if (queueEntity.getLabVessel().getLabel().equals(BARCODE_2)) {
                    foundItems++;
                }
            }
        }

        Assert.assertEquals(foundItems, 2);
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsDefaultRulesWithErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationWithErrors());

        MessageCollection messageCollection = new MessageCollection();

        QueueType queueType = QueueType.PICO;

        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                queueType, messageCollection, DequeueingOptions.DEFAULT_DEQUEUE_RULES);

        Assert.assertTrue(messageCollection.hasWarnings());
        Assert.assertFalse(queueEjb.findQueueByType(queueType).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsDefaultRulesNoErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationNoErrors());

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                QueueType.PICO, messageCollection, DequeueingOptions.DEFAULT_DEQUEUE_RULES);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.PICO).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsOverrideWithErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationWithErrors());

        QueueType queueType = QueueType.PICO;

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                queueType, messageCollection, DequeueingOptions.OVERRIDE);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.PICO).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsOverrideNoErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationNoErrors());

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                QueueType.PICO, messageCollection, DequeueingOptions.OVERRIDE);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.PICO).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testReOrderQueue() {
        MessageCollection messageCollection = new MessageCollection();
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getResortQueue(), QueueTestFactory.getPicoValidationNoErrors());

        long[] newSortOrder = new long[] { 4, 1, 3, 2};

        handleReOrderTesting(queueEjb, newSortOrder, false, messageCollection);

        Assert.assertFalse(messageCollection.hasErrors());
    }

    @Test(groups = DATABASE_FREE)
    public void testReOrderQueueNonUniqueValues() {
        MessageCollection messageCollection = new MessageCollection();
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getResortQueue(), QueueTestFactory.getPicoValidationNoErrors());

        long[] newSortOrder = new long[] { 4, 4, 3, 2};

        handleReOrderTesting(queueEjb, newSortOrder, true, messageCollection);

        Assert.assertTrue(messageCollection.hasErrors());
    }

    @Test(groups = DATABASE_FREE)
    public void testReOrderQueueMissingValues() {

        MessageCollection messageCollection = new MessageCollection();
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getResortQueue(), QueueTestFactory.getPicoValidationNoErrors());

        long[] newSortOrder = new long[] { 1, 3, 2};

        handleReOrderTesting(queueEjb, newSortOrder, true, messageCollection);

        Assert.assertTrue(messageCollection.hasErrors());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testExclude() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationNoErrors());
        LabVessel labVessel = QueueTestFactory.generateLabVessel(LAB_VESSEL_ID);

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(null, Collections.singletonList(labVessel), QueueType.PICO,
                null, messageCollection);

        queueEjb.excludeItems(Collections.singletonList(labVessel), QueueType.PICO, messageCollection);

        GenericQueue picoQueue = queueEjb.findQueueByType(QueueType.PICO);
        int foundItems = 0;
        for (QueueGrouping queueGrouping : picoQueue.getQueueGroupings()) {
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                if (queueEntity.getLabVessel().getLabel().equals(BARCODE_1)
                            && queueEntity.getQueueStatus() == QueueStatus.Excluded) {
                    foundItems++;
                } else if (queueEntity.getLabVessel().getLabel().equals(BARCODE_2)
                            && queueEntity.getQueueStatus() == QueueStatus.Excluded) {
                    foundItems++;
                }
            }
        }

        Assert.assertEquals(foundItems, 2);
    }

    private void handleReOrderTesting(QueueEjb queueEjb, long[] newSortOrder, boolean failExpected,
                                      MessageCollection messageCollection) {

        Map<Long, Long> newOrder = new HashMap<>();
        int currentOrder = 0;
        GenericQueue queueByType = queueEjb.findQueueByType(QueueType.PICO);
        for (QueueGrouping queueGrouping : queueByType.getQueueGroupings()) {
            if (newSortOrder.length > currentOrder) {
                newOrder.put(queueGrouping.getQueueGroupingId(), newSortOrder[currentOrder++]);
            }
        }

        try {
            queueEjb.reOrderQueue(newOrder, QueueType.PICO, messageCollection);
            if (failExpected) {
                Assert.fail("Expected exception");
            }
            for (QueueGrouping queueGrouping : queueByType.getQueueGroupings()) {
                Assert.assertEquals(queueGrouping.getSortOrder(), newOrder.get(queueGrouping.getQueueGroupingId()));
            }
        } catch (Exception e) {
            if (!failExpected) {
                Assert.fail("Unexpected exception");
            }
        }
    }
}

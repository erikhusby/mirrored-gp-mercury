package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueStatus;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;

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
        queueEjb.enqueueLabVessels(Arrays.asList(labVessel, labVessel2), QueueType.DNA_QUANT,
                        null, messageCollection, QueueOrigin.EXTRACTION, null);
        GenericQueue dnaQuantQueue = queueEjb.findQueueByType(QueueType.DNA_QUANT);
        int foundItems = 0;
        for (QueueGrouping queueGrouping : dnaQuantQueue.getQueueGroupings()) {
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

        QueueType queueType = QueueType.DNA_QUANT;

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
                QueueType.DNA_QUANT, messageCollection, DequeueingOptions.DEFAULT_DEQUEUE_RULES);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.DNA_QUANT).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsOverrideWithErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationWithErrors());

        QueueType queueType = QueueType.DNA_QUANT;

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                queueType, messageCollection, DequeueingOptions.OVERRIDE);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.DNA_QUANT).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testDequeueLabVesselsOverrideNoErrors() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationNoErrors());

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)),
                QueueType.DNA_QUANT, messageCollection, DequeueingOptions.OVERRIDE);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.DNA_QUANT).isQueueEmpty());
    }

    @Test(groups = DATABASE_FREE)
    public void testReOrderQueueNoError() {
        MessageCollection messageCollection = new MessageCollection();
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getResortQueue(), QueueTestFactory.getPicoValidationNoErrors());

        Long validQueueGroupingId = queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings().first().getQueueGroupingId();
        queueEjb.reOrderQueue(validQueueGroupingId, 2, QueueType.DNA_QUANT, messageCollection);

        // NOTE:  Because we are mocking, the only thing we can do is verify no error occurs, we can't verify the change
        //        itself because the mocked dao will ALWAYS return the same queue groupings order.   The funcitonality
        //        of the items moving is actually tested within the DB test in QueueEjbTest.

        Assert.assertFalse(messageCollection.hasErrors());
    }

    @Test(groups = DATABASE_FREE)
    public void testReOrderQueueBadQueueGroupingId() {
        MessageCollection messageCollection = new MessageCollection();
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getResortQueue(), QueueTestFactory.getPicoValidationNoErrors());

        queueEjb.reOrderQueue(Long.MAX_VALUE, 2, QueueType.DNA_QUANT, messageCollection);

        Assert.assertTrue(messageCollection.hasErrors());
    }

    @Test(groups = TestGroups.DATABASE_FREE)
    public void testExclude() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID), QueueTestFactory.getPicoValidationNoErrors());
        LabVessel labVessel = QueueTestFactory.generateLabVessel(LAB_VESSEL_ID);

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(Collections.singletonList(labVessel), QueueType.DNA_QUANT,
                null, messageCollection, QueueOrigin.RECEIVING, null);

        queueEjb.excludeItems(Collections.singletonList(labVessel), QueueType.DNA_QUANT, messageCollection);

        GenericQueue dnaQuantQueue = queueEjb.findQueueByType(QueueType.DNA_QUANT);
        int foundItems = 0;
        for (QueueGrouping queueGrouping : dnaQuantQueue.getQueueGroupings()) {
            for (QueueEntity queueEntity : queueGrouping.getQueuedEntities()) {
                if (queueEntity.getLabVessel().getLabel().equals(BARCODE_1)
                            && queueEntity.getQueueStatus() == QueueStatus.EXCLUDED) {
                    foundItems++;
                } else if (queueEntity.getLabVessel().getLabel().equals(BARCODE_2)
                            && queueEntity.getQueueStatus() == QueueStatus.EXCLUDED) {
                    foundItems++;
                }
            }
        }

        Assert.assertEquals(foundItems, 2);
    }
}

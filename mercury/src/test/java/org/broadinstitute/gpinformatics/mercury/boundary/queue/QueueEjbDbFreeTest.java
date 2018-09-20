package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import java.util.Arrays;
import java.util.Collections;
import java.util.SortedSet;

import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
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
        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getEnqueueTestQueue());
        LabVessel labVessel = new BarcodedTube(BARCODE_1);
        LabVessel labVessel2 = new BarcodedTube(BARCODE_2);

        queueEjb.enqueueLabVessels(null, Arrays.asList(labVessel, labVessel2), QueueType.PICO, null);
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
    public void testDequeueLabVessels() {

        QueueEjb queueEjb = new QueueEjb(QueueTestFactory.getDequeueTestQueue(LAB_VESSEL_ID));

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.dequeueLabVessels(Collections.singletonList(QueueTestFactory.generateLabVessel(LAB_VESSEL_ID)), QueueType.PICO, messageCollection);
        Assert.assertFalse(messageCollection.hasWarnings());

        Assert.assertTrue(queueEjb.findQueueByType(QueueType.PICO).isQueueEmpty());
    }
}

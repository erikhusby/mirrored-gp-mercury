package org.broadinstitute.gpinformatics.mercury.boundary.queue;


import org.apache.commons.collections.CollectionUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchTestUtils;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.BarcodedTube;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Inject;
import javax.transaction.UserTransaction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class QueueEjbTest extends Arquillian {


    @Inject
    private UserTransaction utx;

    @Inject
    private QueueEjb queueEjb;

    @Inject
    LabBatchTestUtils labBatchTestUtils;

    private LinkedHashMap<String, BarcodedTube> mapBarcodeToTube = new LinkedHashMap<>();


    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }

        utx.begin();

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        mapBarcodeToTube = labBatchTestUtils.initializeTubes(vesselSampleList);
    }

    @AfterMethod(groups = TestGroups.STUBBY)
    public void tearDown() throws Exception {
        if (utx == null) {
            return;
        }

        utx.rollback();
    }

    @Deployment
    public static WebArchive buildMercuryWar() {
        return DeploymentBuilder.buildMercuryWar(DEV);
    }

    @Test(groups = TestGroups.STANDARD)
    public void enqueueToPicoQueueTest() {
        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(null, generateLabVesselsForTest(), QueueType.PICO, "Whatever", messageCollection);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    @Test(groups = TestGroups.STANDARD)
    public void dequeueFromPicoQueueTest() {
        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(null, generateLabVesselsForTest(), QueueType.PICO, "Whatever", messageCollection);
        queueEjb.dequeueLabVessels(generateLabVesselsForTest(), QueueType.PICO, messageCollection,
                DequeueingOptions.OVERRIDE);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    @Test(groups = TestGroups.STANDARD)
    public void reorderTest() throws Exception {
        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(null, generateLabVesselsForTest(), QueueType.PICO, "Whatever", messageCollection);


        SortedSet<QueueGrouping> queueGroupings = queueEjb.findQueueByType(QueueType.PICO).getQueueGroupings();
        Assert.assertNotNull(queueGroupings);

        List<Long> originalOrder = new ArrayList<>();
        for (QueueGrouping queueGrouping : queueGroupings) {
            originalOrder.add(queueGrouping.getQueueGroupingId());
        }

        List<Long> oppositeOrder = new ArrayList<>(originalOrder);

        Collections.reverse(oppositeOrder);

        long currentNumber = 1;
        Map<Long, Long> reorder = new HashMap<>();

        for (Long orderId : oppositeOrder) {
            reorder.put(orderId, currentNumber++);
        }

        queueEjb.reOrderQueue(reorder, QueueType.PICO);

        GenericQueue queueByType = queueEjb.findQueueByType(QueueType.PICO);
        int i = 0;
        for (QueueGrouping queueGrouping : queueByType.getQueueGroupings()) {
            Assert.assertEquals(queueGrouping.getQueueGroupingId(), oppositeOrder.get(0));
            Assert.assertNotEquals(queueGrouping.getQueueGroupingId(), oppositeOrder.get(0));
        }

        queueEjb.dequeueLabVessels(generateLabVesselsForTest(), QueueType.PICO, messageCollection,
                DequeueingOptions.OVERRIDE);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    private Collection<? extends LabVessel> generateLabVesselsForTest() {
        return mapBarcodeToTube.values();
    }
}

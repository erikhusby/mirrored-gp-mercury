package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.broadinstitute.bsp.client.queue.DequeueingOptions;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.boundary.vessel.LabBatchTestUtils;
import org.broadinstitute.gpinformatics.mercury.control.dao.queue.GenericQueueDao;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueGrouping;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueOrigin;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
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
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.SortedSet;

import static org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment.DEV;

@Test(groups = TestGroups.STANDARD)
public class QueueEjbTest extends Arquillian {

    public static final int POSITION_TO_MOVE_TO_4 = 4;
    public static final int POSITION_TO_MOVE_TO_2 = 2;
    @Inject
    private UserTransaction utx;

    @Inject
    private QueueEjb queueEjb;

    @Inject
    private GenericQueueDao queueDao;

    @Inject
    LabBatchTestUtils labBatchTestUtils;

    private LinkedHashMap<String, LabVessel> mapBarcodeToTube = new LinkedHashMap<>();

    @BeforeMethod(groups = TestGroups.STUBBY)
    public void setUp() throws Exception {
        if (utx == null) {
            return;
        }

        utx.begin();

        List<String> vesselSampleList = new ArrayList<>(6);

        Collections.addAll(vesselSampleList, "SM-423", "SM-243", "SM-765", "SM-143", "SM-9243", "SM-118");

        mapBarcodeToTube.putAll(labBatchTestUtils.initializeTubes(vesselSampleList, MaterialType.DNA_DNA_GENOMIC));
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
    public void enqueueToDnaQuantQueueTest() {
        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    @Test(groups = TestGroups.STANDARD)
    public void dequeueFromDnaQuantQueueTest() {
        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);
        queueEjb.dequeueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, messageCollection,
                DequeueingOptions.OVERRIDE);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    @Test(groups = TestGroups.STANDARD)
    public void reorderTest() {
        MessageCollection messageCollection = new MessageCollection();
        Collection<? extends LabVessel> labVessels = generateLabVesselsForTest();
        for (LabVessel labVessel : labVessels) {
            queueEjb.enqueueLabVessels(Collections.singletonList(labVessel), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);
        }

        SortedSet<QueueGrouping> queueGroupings = queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings();
        Assert.assertNotNull(queueGroupings);

        Long firstGroupindId = queueGroupings.first().getQueueGroupingId();

        Long lastGroupingId = queueGroupings.last().getQueueGroupingId();

        queueEjb.reOrderQueue(firstGroupindId, POSITION_TO_MOVE_TO_4, QueueType.DNA_QUANT, messageCollection);
        queueDao.flush();
        queueDao.clear();

        verifyReorderSuccess(firstGroupindId, POSITION_TO_MOVE_TO_4);

        queueEjb.reOrderQueue(lastGroupingId, POSITION_TO_MOVE_TO_2, QueueType.DNA_QUANT, messageCollection);
        queueDao.flush();
        queueDao.clear();

        verifyReorderSuccess(lastGroupingId, POSITION_TO_MOVE_TO_2);

        Assert.assertFalse(messageCollection.hasErrors());
    }

    private void verifyReorderSuccess(Long positionToCheck, int positionToMoveTo) {
        SortedSet<QueueGrouping> updatedQueueGroupings = queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings();

        int i = 0;
        for (QueueGrouping queueGrouping : updatedQueueGroupings) {
            i++;
            if (i == positionToMoveTo) {
                Assert.assertEquals(queueGrouping.getQueueGroupingId(), positionToCheck);
            }
        }
    }

    @Test(groups = TestGroups.STANDARD)
    public void excludeTest() {

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);
        queueEjb.excludeItems(generateLabVesselsForTest(), QueueType.DNA_QUANT, messageCollection);

        Assert.assertFalse(messageCollection.hasErrors());
        Assert.assertFalse(messageCollection.hasWarnings());
    }

    private Collection<LabVessel> generateLabVesselsForTest() {
        return mapBarcodeToTube.values();
    }

     @Test
    public void moveToBottomTest() {

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);

        SortedSet<QueueGrouping> queueGroupings = queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings();
        Long idOfItemBeingMoved = queueGroupings.first().getQueueGroupingId();
        queueEjb.moveToBottom(QueueType.DNA_QUANT, idOfItemBeingMoved);

        queueDao.flush();
        queueDao.clear();

        Assert.assertEquals(queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings().last().getQueueGroupingId(), idOfItemBeingMoved);
    }

    @Test
    public void moveToTopTest() {

        MessageCollection messageCollection = new MessageCollection();
        queueEjb.enqueueLabVessels(generateLabVesselsForTest(), QueueType.DNA_QUANT, "Whatever", messageCollection, QueueOrigin.EXTRACTION, null);

        SortedSet<QueueGrouping> queueGroupings = queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings();
        Long idOfItemBeingMoved = queueGroupings.last().getQueueGroupingId();
        queueEjb.moveToTop(QueueType.DNA_QUANT, idOfItemBeingMoved);

        queueDao.flush();
        queueDao.clear();

        Assert.assertEquals(queueEjb.findQueueByType(QueueType.DNA_QUANT).getQueueGroupings().first().getQueueGroupingId(), idOfItemBeingMoved);
    }
}

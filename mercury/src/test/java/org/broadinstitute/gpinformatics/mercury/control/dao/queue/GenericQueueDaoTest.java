package org.broadinstitute.gpinformatics.mercury.control.dao.queue;

import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.entity.queue.GenericQueue;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueEntity;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.testng.Assert;
import org.testng.annotations.Test;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Test(groups = TestGroups.STANDARD)
public class GenericQueueDaoTest {

    public static final long RNA_LAB_VESSEL_ID = 5541841;
    public static final long LAB_VESSEL_ID_IN_QUEUE_IN_PAST = 0;

    @Inject
    public GenericQueueDao genericQueueDao;

    @Test(groups = TestGroups.STANDARD)
    public void findQueueByTypeTest() {
        // Verify that we have a database row for every queue type, and that we can successfully find it
        for (QueueType queueType : QueueType.values()) {
            GenericQueue queue = genericQueueDao.findQueueByType(queueType);
            Assert.assertNotNull(queue);
        }
    }

    // TODO:  After first refresh that has production pico queue data, put DB ID into variable and enable this test.
    @Test(groups = TestGroups.STANDARD, enabled = false)
    public void findEntitiesByVesselIdsTest() {
        List<QueueEntity> queueEntities =
                genericQueueDao.findEntitiesByVesselIds(QueueType.PICO, Collections.singletonList(LAB_VESSEL_ID_IN_QUEUE_IN_PAST));

        Assert.assertFalse(queueEntities.isEmpty());
    }

    @Test(groups = TestGroups.STANDARD)
    public void findEntitiesByVesselIdsTestNoneFoundTest() {

        // Verify that we don't get an NPE on either a blank list or a null variable.
        List<QueueEntity> queueEntities = genericQueueDao.findEntitiesByVesselIds(QueueType.PICO,null);
        Assert.assertTrue(queueEntities.isEmpty());
        queueEntities = genericQueueDao.findEntitiesByVesselIds(QueueType.PICO, new ArrayList<>());
        Assert.assertTrue(queueEntities.isEmpty());

        // Pass in a vessel id which has never been in and will never be in the pico queue.
        queueEntities = genericQueueDao.findEntitiesByVesselIds(QueueType.PICO, Collections.singletonList(RNA_LAB_VESSEL_ID));
        Assert.assertTrue(queueEntities.isEmpty());
    }
}

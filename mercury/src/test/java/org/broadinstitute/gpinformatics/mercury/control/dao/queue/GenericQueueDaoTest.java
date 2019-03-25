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
}

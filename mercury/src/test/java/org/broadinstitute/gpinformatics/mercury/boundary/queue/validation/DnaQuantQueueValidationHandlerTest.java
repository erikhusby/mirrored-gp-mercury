package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.testng.Assert;
import org.testng.annotations.Test;

public class DnaQuantQueueValidationHandlerTest {

    @Test
    public void checkQueuePriorityOrderContentsTest() {
        QueuePriority[] queuePriorityOrder = new DnaQuantEnqueueOverride().getQueuePriorityOrder();
        // Makes sure something in place
        Assert.assertTrue(ArrayUtils.isNotEmpty(queuePriorityOrder));

        // Makes sure the two items not included that the software is SPECIFICALLY expecting to not be included.
        for (QueuePriority queuePriority : queuePriorityOrder) {
            Assert.assertNotEquals(queuePriority, QueuePriority.STANDARD);
            Assert.assertNotEquals(queuePriority, QueuePriority.ALTERED);
        }
    }
}

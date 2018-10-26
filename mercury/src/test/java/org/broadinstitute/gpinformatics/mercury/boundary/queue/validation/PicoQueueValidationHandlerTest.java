package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.apache.commons.lang3.ArrayUtils;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.PicoEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueuePriority;
import org.testng.Assert;
import org.testng.annotations.Test;

public class PicoQueueValidationHandlerTest {

    @Test
    public void checkQueuePriorityOrderContentsTest() {
        QueuePriority[] queuePriorityOrder = new PicoEnqueueOverride().getQueuePriorityOrder();
        // Makes sure something in place
        Assert.assertTrue(ArrayUtils.isNotEmpty(queuePriorityOrder));

        // Makes sure the two items not included that the software is SPECIFICALLY expecting to not be included.
        for (QueuePriority queuePriority : queuePriorityOrder) {
            Assert.assertNotEquals(queuePriority, QueuePriority.STANDARD);
            Assert.assertNotEquals(queuePriority, QueuePriority.ALTERED);
        }
    }
}

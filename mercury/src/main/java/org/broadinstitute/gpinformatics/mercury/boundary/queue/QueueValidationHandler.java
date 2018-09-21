package org.broadinstitute.gpinformatics.mercury.boundary.queue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

public class QueueValidationHandler {
    private static final Log logger = LogFactory.getLog(QueueValidationHandler.class);

    public void validate(LabVessel labVessel, QueueType queueType, MessageCollection messageCollection) throws Exception {
        AbstractQueueValidator queueValidator = queueType.getValidatorClass().newInstance();
        queueValidator.validatePreEnqueue(labVessel, messageCollection);
    }

    public boolean isComplete(LabVessel labVessel, QueueType queueType, MessageCollection messageCollection) {
        AbstractQueueValidator queueValidator;
        try {
            queueValidator = queueType.getValidatorClass().newInstance();
            return queueValidator.isComplete(labVessel);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.debug(e);
            return false;
        }
    }
}

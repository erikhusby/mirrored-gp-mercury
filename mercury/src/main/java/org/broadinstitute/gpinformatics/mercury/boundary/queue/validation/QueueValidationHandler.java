package org.broadinstitute.gpinformatics.mercury.boundary.queue.validation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.entity.queue.QueueType;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Handles the validation of all the queues.  This is the class which is utilized by the QueueEJB for getting to each
 * queue's specific enqueue and dequeue validation.
 */
public class QueueValidationHandler {
    private static final Log logger = LogFactory.getLog(QueueValidationHandler.class);

    /**
     * Entry point for validation.  Handles adding warnings  for failed and unknown validation results on enqueue.
     *
     * @param labVessels            List of LabVessels to validate
     * @param queueType             Queue the lab vessels are being validated for.
     * @param messageCollection     Messages back to the user.
     */
    public void validate(Collection<LabVessel> labVessels, QueueType queueType, MessageCollection messageCollection) throws Exception {
        AbstractQueueValidator queueValidator = queueType.getValidatorClass().newInstance();
        Map<Long, ValidationResult> validationResultsByVesselId = queueValidator.validatePreEnqueue(labVessels, messageCollection);

        for (LabVessel labVessel : labVessels) {
            switch (validationResultsByVesselId.get(labVessel.getLabVesselId())) {
                case FAIL:
                    messageCollection.addWarning("Lab Vessel: " + labVessel.getLabel()
                            + " failed validation but was still entered into the " + queueType.getTextName()
                            + " queue.");
                    break;
                case UNKNOWN:
                    messageCollection.addWarning("Lab Vessel: " + labVessel.getLabel()
                            + " could not be validated but was still entered into the " + queueType.getTextName()
                            + " queue.");
                    break;
            }
        }
    }

    /**
     * Checks the samples to make sure they are properly completed.
     *
     * @param labVessel             Lab Vessel to check to see if it is completed.
     * @param queueType             Type of Queue to check for completion against.
     * @param messageCollection     Messages back to the user
     * @return                      True if complete, false otherwise.
     */
    public boolean isComplete(LabVessel labVessel, QueueType queueType, MessageCollection messageCollection) {
        AbstractQueueValidator queueValidator;
        try {
            queueValidator = queueType.getValidatorClass().newInstance();
            return queueValidator.isComplete(labVessel, messageCollection);
        } catch (InstantiationException | IllegalAccessException e) {
            logger.debug(e);
            return false;
        }
    }
}

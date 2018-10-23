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

public class QueueValidationHandler {
    private static final Log logger = LogFactory.getLog(QueueValidationHandler.class);

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

package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.AbstractQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.PicoQueueValidator;

public enum QueueType {
    PICO("Pico", PicoQueueValidator.class);

    private final String textName;
    private final Class<? extends AbstractQueueValidator> validatorClass;

    QueueType(String textName, Class<? extends AbstractQueueValidator> validatorClass) {
        this.textName = textName;
        this.validatorClass = validatorClass;
    }

    public String getTextName() {
        return textName;
    }

    public Class<? extends AbstractQueueValidator> getValidatorClass() {
        return validatorClass;
    }
}

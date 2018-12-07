package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.PicoDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.PicoEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.AbstractQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.PicoQueueValidator;

public enum QueueType {
    PICO("Pico", PicoQueueValidator.class, PicoEnqueueOverride.class, PicoDataDumpGenerator.class);

    private final String textName;
    private final Class<? extends AbstractQueueValidator> validatorClass;
    private final Class<? extends AbstractEnqueueOverride> enqueueOverrideClass;
    private final Class<? extends AbstractDataDumpGenerator> dataDumpGenerator;

    QueueType(String textName, Class<? extends AbstractQueueValidator> validatorClass,
              Class<? extends AbstractEnqueueOverride> enqueueOverrideClass,
              Class<? extends AbstractDataDumpGenerator> dataDumpGenerator) {
        this.textName = textName;
        this.validatorClass = validatorClass;
        this.enqueueOverrideClass = enqueueOverrideClass;
        this.dataDumpGenerator = dataDumpGenerator;
    }

    public String getTextName() {
        return textName;
    }

    public Class<? extends AbstractQueueValidator> getValidatorClass() {
        return validatorClass;
    }

    public Class<? extends AbstractEnqueueOverride> getEnqueueOverrideClass() {
        return enqueueOverrideClass;
    }

    public Class<? extends AbstractDataDumpGenerator> getDataDumpGenerator() {
        return dataDumpGenerator;
    }
}

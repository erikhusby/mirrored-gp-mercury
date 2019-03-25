package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.PicoDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.PicoEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.AbstractQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.PicoQueueValidator;

/**
 * Enum which defines a queue and its implementation.
 */
public enum QueueType {
    PICO("Pico", PicoQueueValidator.class, PicoEnqueueOverride.class, PicoDataDumpGenerator.class,
            QueueContainerRule.TUBES_ONLY);

    // Name displayed in the queue page
    private final String textName;
    // Used to validate on entry / exit of the queue.
    private final Class<? extends AbstractQueueValidator> validatorClass;
    // Used to define an override during the Enqueue process utilizing the QueueGrouping.Priority value
    private final Class<? extends AbstractEnqueueOverride> enqueueOverrideClass;
    // Used by the queue pages to generate a datadump.
    private final Class<? extends AbstractDataDumpGenerator> dataDumpGenerator;
    // Used throughout to do some verification based upon whether we want to allow only tubes or any vessel.
    private final QueueContainerRule queueContainerRule;

    QueueType(String textName, Class<? extends AbstractQueueValidator> validatorClass,
              Class<? extends AbstractEnqueueOverride> enqueueOverrideClass,
              Class<? extends AbstractDataDumpGenerator> dataDumpGenerator, QueueContainerRule queueContainerRule) {
        this.textName = textName;
        this.validatorClass = validatorClass;
        this.enqueueOverrideClass = enqueueOverrideClass;
        this.dataDumpGenerator = dataDumpGenerator;
        this.queueContainerRule = queueContainerRule;
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

    public QueueContainerRule getQueueContainerRule() {
        return queueContainerRule;
    }
}

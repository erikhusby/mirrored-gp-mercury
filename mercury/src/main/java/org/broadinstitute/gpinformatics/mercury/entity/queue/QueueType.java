package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.AbstractDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.datadump.DnaQuantDataDumpGenerator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules.AbstractPostDequeueHandler;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules.DnaQuantPostDequeueHandler;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules.FingerprintingPostDequeueHandler;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.dequeueRules.VolumeCheckPostDequeueHandler;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.AbstractEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.DnaQuantEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.FingerprintingEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.enqueuerules.VolumeCheckEnqueueOverride;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.AbstractQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.DnaQuantQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.FingerprintingQueueValidator;
import org.broadinstitute.gpinformatics.mercury.boundary.queue.validation.VolumeCheckQueueValidator;

/**
 * Enum which defines a queue and its implementation.
 */
public enum QueueType {
    VOLUME_CHECK("Volume Check", VolumeCheckQueueValidator.class, VolumeCheckEnqueueOverride.class,
            VolumeCheckPostDequeueHandler.class, DnaQuantDataDumpGenerator.class, QueueContainerRule.TUBES_ONLY),
    DNA_QUANT("DNA Quant", DnaQuantQueueValidator.class, DnaQuantEnqueueOverride.class,
            DnaQuantPostDequeueHandler.class, DnaQuantDataDumpGenerator.class, QueueContainerRule.TUBES_ONLY),
    FINGERPRINTING("Fingerprinting", FingerprintingQueueValidator.class, FingerprintingEnqueueOverride.class,
            FingerprintingPostDequeueHandler.class, DnaQuantDataDumpGenerator.class, QueueContainerRule.TUBES_ONLY),
//    ARRAY_PLATING("Array Plating", DnaQuantQueueValidator.class, DnaQuantEnqueueOverride.class, DnaQuantPostDequeueHandler.class,
//            DnaQuantDataDumpGenerator.class, QueueContainerRule.TUBES_ONLY),
//    SEQ_PLATING("Seq Plating", DnaQuantQueueValidator.class, DnaQuantEnqueueOverride.class, DnaQuantPostDequeueHandler.class,
//            DnaQuantDataDumpGenerator.class, QueueContainerRule.TUBES_ONLY),
    ;

    // Name displayed in the queue page
    private final String textName;
    // Used to validate on entry / exit of the queue.
    private final Class<? extends AbstractQueueValidator> validatorClass;
    // Used to define an override during the Enqueue process utilizing the QueueGrouping.Priority value
    private final Class<? extends AbstractEnqueueOverride> enqueueOverrideClass;
    // Used to define an override during the Dequeue process.
    private final Class<? extends AbstractPostDequeueHandler> postDequeueHandlerClass;
    // Used by the queue pages to generate a datadump.
    private final Class<? extends AbstractDataDumpGenerator> dataDumpGenerator;
    // Used throughout to do some verification based upon whether we want to allow only tubes or any vessel.
    private final QueueContainerRule queueContainerRule;

    QueueType(String textName, Class<? extends AbstractQueueValidator> validatorClass,
              Class<? extends AbstractEnqueueOverride> enqueueOverrideClass,
              Class<? extends AbstractPostDequeueHandler> postDequeueHandlerClass,
              Class<? extends AbstractDataDumpGenerator> dataDumpGenerator, QueueContainerRule queueContainerRule) {
        this.textName = textName;
        this.validatorClass = validatorClass;
        this.enqueueOverrideClass = enqueueOverrideClass;
        this.postDequeueHandlerClass = postDequeueHandlerClass;
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

    public Class<? extends AbstractPostDequeueHandler> getPostDequeueHandlerClass() {
        return postDequeueHandlerClass;
    }

    public Class<? extends AbstractDataDumpGenerator> getDataDumpGenerator() {
        return dataDumpGenerator;
    }

    public QueueContainerRule getQueueContainerRule() {
        return queueContainerRule;
    }
}

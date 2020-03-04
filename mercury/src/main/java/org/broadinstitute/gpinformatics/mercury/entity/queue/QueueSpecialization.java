package org.broadinstitute.gpinformatics.mercury.entity.queue;

import java.util.ArrayList;
import java.util.List;

/**
 * This is used to track what modifications are needed for the standard processes.
 * Example:  Pico gets run with different dilution rules for DNA derived from FFPE than DNA derived from other substances.
 *
 * This is meant to be used as informational only.
 */
public enum QueueSpecialization {
    MALARIA("Malaria", QueueType.DNA_QUANT),
    FFPE("FFPE", QueueType.DNA_QUANT);

    private final String displayName;
    private final QueueType queueType;

    QueueSpecialization(String displayName, QueueType queueType) {
        this.displayName = displayName;
        this.queueType = queueType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public QueueType getQueueType() {
        return queueType;
    }

    /**
     * Finds the Specializations allowed for a particular QueueType.
     *
     * @param queueType     QueueType to find specializations for.
     * @return              List of possible specializations for the selected queue.
     */
    public static List<QueueSpecialization> getQueueSpecializationsByQueueType(QueueType queueType) {
        List<QueueSpecialization> queueSpecializations = new ArrayList<>();

        for (QueueSpecialization queueSpecialization : values()) {
            if (queueSpecialization.queueType == queueType) {
                queueSpecializations.add(queueSpecialization);
            }
        }
        return queueSpecializations;
    }

    public String getName() {
        return name();
    }
}

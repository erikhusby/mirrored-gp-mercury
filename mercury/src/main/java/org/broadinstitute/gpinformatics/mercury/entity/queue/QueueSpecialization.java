package org.broadinstitute.gpinformatics.mercury.entity.queue;

import java.util.ArrayList;
import java.util.List;

public enum QueueSpecialization {
    MALARIA("Malaria", QueueType.PICO),
    FFPE("FFPE", QueueType.PICO);

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

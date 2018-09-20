package org.broadinstitute.gpinformatics.mercury.entity.queue;

import org.broadinstitute.bsp.client.util.MessageCollection;

public enum QueueType {
    PICO("Pico");

    private final String textName;

    QueueType(String textName) {
        this.textName = textName;
    }

    public String getTextName() {
        return textName;
    }
}

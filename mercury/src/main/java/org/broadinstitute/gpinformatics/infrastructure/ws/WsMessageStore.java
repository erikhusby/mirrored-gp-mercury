package org.broadinstitute.gpinformatics.infrastructure.ws;

import java.io.Serializable;
import java.util.Date;

/**
 * A facility for storing messages received through web services, so the can be resubmitted in case of error.
 */
public interface WsMessageStore extends Serializable {
    void store(String message, Date receivedDate);

    void recordError(String message, Date receivedDate, Exception exception);
}

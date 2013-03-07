package org.broadinstitute.gpinformatics.infrastructure.ws;

import java.io.Serializable;
import java.util.Date;

/**
 * A facility for storing messages received through web services, so they can be resubmitted in case of error.
 */
public interface WsMessageStore extends Serializable {

    String BETTALIMS_RESOURCE_TYPE = "bettalims";
    String SAMPLE_RECEIPT_RESOURCE_TYPE = "samplereceipt";

    void store(String resourceType, String message, Date receivedDate);

    void recordError(String resourceType, String message, Date receivedDate, Exception exception);
}

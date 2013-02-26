package org.broadinstitute.gpinformatics.infrastructure.ws;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.Date;

/**
 * Stub implementation for message store
 */
@Stub
@Alternative
public class WsMessageStoreStub implements WsMessageStore{

    @Override
    public void store(String resourceType, String message, Date receivedDate) {
        // do nothing
    }

    @Override
    public void recordError(String resourceType, String message, Date receivedDate, Exception exception) {
        // do nothing
    }
}

package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.mercury.boundary.squid.GSSRSampleKitRequest;
import org.broadinstitute.gpinformatics.mercury.boundary.squid.GSSRSampleKitResponse;

/**
 * @author breilly
 */
public interface SampleKitSOAPService {

    public GSSRSampleKitResponse createGSSRSampleKit(GSSRSampleKitRequest request);
}

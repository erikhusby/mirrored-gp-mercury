package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import java.util.Set;

/**
 */
@Stub
public class BSPSampleReceiptServiceStub implements BSPSampleReceiptService {
    @Override
    public SampleKitReceiptResponse receiveSamples(Set<String> barcodes, String username) {
        return null;
    }
}

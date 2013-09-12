package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;

import java.io.Serializable;
import java.util.Set;

/**
 *
 */
public interface BSPSampleReceiptService extends Serializable {

    public SampleKitReceiptResponse receiveSamples(Set<String> barcodes, String username);
}

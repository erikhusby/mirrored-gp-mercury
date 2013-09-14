package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;

import java.io.Serializable;
import java.util.List;

/**
 *
 */
public interface BSPSampleReceiptService extends Serializable {

    public SampleKitReceiptResponse receiveSamples(List<String> barcodes, String username);
}

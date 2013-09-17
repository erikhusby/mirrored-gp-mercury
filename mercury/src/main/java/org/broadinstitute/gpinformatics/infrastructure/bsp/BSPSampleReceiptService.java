package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.List;

/**
 * Service for receiving BSP samples.
 */
public interface BSPSampleReceiptService extends Serializable {

    /**
     * Receives samples within BSP, validate that they can be received and receives the samples within Mercury.
     *
     * @param barcodes BSP Sample IDs which are to be received.
     * @param username Username of the operator
     * @return SampleKitReceiptResponse returned from BSP.
     */
    public SampleKitReceiptResponse receiveSamples(List<String> barcodes, String username)
            throws UnsupportedEncodingException;
}

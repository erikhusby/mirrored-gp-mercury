package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.context.Dependent;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Stub of the bsp sample receipt service specifically set up to mark all of the samples as received.
 */
@Stub
@Dependent
public class BSPSampleReceiptServiceStub implements BSPSampleReceiptService {

    public static final String SAMPLE_KIT_ID = "SK-1234";

    /**
     * Receives all of the samples passed in under the sample kit id SK-1234
     *
     * @param barcodes Sample Ids
     * @param username Username of the current user, ignored for this stub.
     * @return A successful SampleKitReceiptResponse with the kit id SK-1234, each barcode as the sample id,
     *         and sample id + sample id as its associated external (matrix 2d) barcode.
     */
    @Override
    public SampleKitReceiptResponse receiveSamples(List<String> barcodes, String username) {
        SampleKitReceiptResponse sampleKitReceiptResponse = new SampleKitReceiptResponse();
        sampleKitReceiptResponse.setSuccess(true);
        sampleKitReceiptResponse.setResult(barcodes);

        // Show all samples as received, have none missing.
        sampleKitReceiptResponse.setMissingSamplesPerKit(Collections.<String,Set<SampleKitReceiptResponse.Barcodes>>emptyMap());

        // generate all the received barcodes with a sample kit id of SK-1234.
        HashMap<String, Set<SampleKitReceiptResponse.Barcodes>> receivedSamplesPerKit = new HashMap<>();
        receivedSamplesPerKit.put(SAMPLE_KIT_ID, generateBarcodes(barcodes));
        sampleKitReceiptResponse.setReceivedSamplesPerKit(receivedSamplesPerKit);

        return sampleKitReceiptResponse;
    }

    /**
     * Generates a Barcodes object for each sample id passed in.
     *
     * @param sampleIds List of sample ids to generate barcodes for.
     * @return HashSet of generated barcodes. We're using a single sampleId as the sample Id, and SampleId+sampleId as
     *         the external id (2d matrix barcode).
     */
    private Set<SampleKitReceiptResponse.Barcodes> generateBarcodes(Collection<String> sampleIds) {

        Set<SampleKitReceiptResponse.Barcodes> barcodes = new HashSet<>();

        for (String sampleId : sampleIds) {
            // Instead of generating a real 2d matrix barcode, we're using sampleId + sampleId as a string.
            barcodes.add(new SampleKitReceiptResponse.Barcodes(sampleId, "STUB-" +sampleId));
        }

        return barcodes;
    }
}

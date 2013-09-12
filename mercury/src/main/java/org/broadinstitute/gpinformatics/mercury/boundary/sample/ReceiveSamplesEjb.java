package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleReceiptService;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
@Stateful
@RequestScoped
public class ReceiveSamplesEjb {

    @Inject
    private BSPSampleDataFetcher sampleDataFetcherService;

    @Inject
    private BSPSampleReceiptService receiptService;

    public MessageCollection receiveSamples(List<String> barcodes, String username) {

        MessageCollection messageCollection = new MessageCollection();

        Map<String,SampleInfo> sampleInfoMap = sampleDataFetcherService.fetchSampleDetailsByMatrixBarcodes(barcodes);

        validateForReceipt(sampleInfoMap.values(), messageCollection);

        if (!messageCollection.hasErrors()) {

            SampleKitReceiptResponse receiptResponse = receiptService.receiveSamples(sampleInfoMap.keySet(), username);
        }

        return messageCollection;
    }

    private void validateForReceipt(Collection<SampleInfo> sampleInfos, MessageCollection messageCollection) {



    }
}

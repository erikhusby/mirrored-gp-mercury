package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.getsampledetails.SampleInfo;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 */
@Stateful
@RequestScoped
public class ReceiveSamplesEjb {

    @Inject
    private BSPSampleDataFetcher service;

    public List<String> receiveSamples(List<String> barcodes) {

        List<String> errorMessages = new ArrayList<>();

        Map<String,SampleInfo> sampleInfoMap = service.fetchSampleDetailsByMatrixBarcodes(barcodes);

        validateForReceipt(sampleInfoMap.values(), errorMessages);

        if (errorMessages.isEmpty()) {

        }

        return errorMessages;
    }

    private void validateForReceipt(Collection<SampleInfo> sampleInfos, List<String> errorMessages) {



    }
}

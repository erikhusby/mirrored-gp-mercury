package org.broadinstitute.gpinformatics.athena.presentation.links;

import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrderSample;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;

import javax.inject.Inject;

/**
 * This class is used to generate BSP links for the UI.
 */
public class SampleSearchLink {
    private static final String SEARCH_SAMPLE = "samplesearch/SampleSummary.action?sampleId=";

    @Inject
    private BSPConfig bspConfig;

    public String getUrl(ProductOrderSample sample) {
        if (sample.isInBspFormat()) {
            return bspConfig.getUrl(SEARCH_SAMPLE + sample.getBspSampleName());
        } else {
            return sample.getSampleName();
        }
    }
}

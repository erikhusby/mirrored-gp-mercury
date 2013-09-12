package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Impl
public class BSPSampleReceiptServiceImpl extends BSPJerseyClient implements BSPSampleReceiptService {

    private static final String WEB_SERVICE_URL = "sample/receivesample";

    /**
     * Required for @Impl class.
     */
    @SuppressWarnings("unused")
    public BSPSampleReceiptServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPSampleReceiptServiceImpl(BSPConfig bspConfig) {
        super(bspConfig);
    }

    @Override
    public void receiveSamples(Set<String> barcodes, String username) {

        List<String> parameters = new ArrayList<>();
        parameters.add("username=" + username);
        parameters.add("barcodes=");
    }
}

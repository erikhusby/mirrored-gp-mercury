package org.broadinstitute.gpinformatics.mercury;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.inject.Inject;

/**
 * This contains common code used by all clients of BSP web services.
 */
public abstract class BSPJerseyClient extends AbstractJerseyClientService {

    private static final long serialVersionUID = 5472586820069306030L;
    private static Log logger = LogFactory.getLog(BSPJerseyClient.class);

    @Inject
    private BSPConfig bspConfig;

    public BSPJerseyClient() {
    }

    public BSPJerseyClient(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    protected String getUrl(String urlSuffix) {
        String urlString = bspConfig.getWSUrl(urlSuffix);
        logger.debug(String.format("URL string is '%s'", urlString));
        return urlString;
    }

    public BSPConfig getBspConfig() {
        return bspConfig;
    }
}

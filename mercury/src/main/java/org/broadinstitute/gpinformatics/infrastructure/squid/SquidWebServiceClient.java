package org.broadinstitute.gpinformatics.infrastructure.squid;

import org.broadinstitute.gpinformatics.infrastructure.AbstractWebServiceClient;

/**
 * Convenience class for calling into Squid Web Services.
 *
 * @param <T> service port type
 */
public abstract class SquidWebServiceClient<T> extends AbstractWebServiceClient<T> {
    protected abstract SquidConfig getSquidConfig();

    /**
     * A tiny bit of syntactic sugar to make it clear we're invoking a webservice on Squid.
     *
     * @return service port
     */
    protected T squidCall() {
        return wsCall();
    }

    @Override
    protected String getBaseUrl() {
        SquidConfig squidConfig = getSquidConfig();
        if (squidConfig == null) {
            return null;
        }

        return squidConfig.getUrl();
    }
}

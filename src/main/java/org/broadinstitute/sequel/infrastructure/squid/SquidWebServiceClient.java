package org.broadinstitute.sequel.infrastructure.squid;

import org.broadinstitute.sequel.infrastructure.AbstractWebServiceClient;


/**
 * Convenience class for calling into Squid Web Services
 *
 * @param <T> service port type
 */
public abstract class SquidWebServiceClient<T> extends AbstractWebServiceClient<T> {

    /**
     * a tiny bit of syntactic sugar to make it clear we're invoking a webservice on Squid
     *
     * @return service port
     */
    protected T squidCall() {
        return wsCall();
    }


    protected abstract SquidConfig getSquidConfig();


    @Override
    protected String getBaseUrl() {
        return getSquidConfig().getBaseUrl();
    }
}

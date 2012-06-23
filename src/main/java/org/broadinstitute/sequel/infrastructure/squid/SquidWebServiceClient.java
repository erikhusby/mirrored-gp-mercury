package org.broadinstitute.sequel.infrastructure.squid;

import org.broadinstitute.sequel.infrastructure.AbstractWebServiceClient;


public abstract class SquidWebServiceClient<T> extends AbstractWebServiceClient<T> {

    /**
     * a tiny bit of syntactic sugar to make it clear we're invoking a webservice on Squid
     *
     * @return
     */
    protected T squidCall() {
        return wsCall();
    }


    protected abstract SquidConnectionParameters getSquidConnectionParameters();


    @Override
    protected String getBaseUrl() {
        return getSquidConnectionParameters().getBaseUrl();
    }
}

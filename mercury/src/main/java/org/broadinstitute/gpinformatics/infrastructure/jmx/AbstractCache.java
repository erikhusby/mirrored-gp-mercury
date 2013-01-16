package org.broadinstitute.gpinformatics.infrastructure.jmx;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * Objects that want to register with the external data cache control must implement this API.
 */
public abstract class AbstractCache {

    /**
     * This method is called when an object should refresh its cache.
     */
    public abstract void refreshCache();

    @Inject
    private ExternalDataCacheControl externalDataCacheControl;

    @PostConstruct
    private void postConstruct() {
        externalDataCacheControl.registerCache(this);
    }

    @PreDestroy
    private void preDestroy() {
        externalDataCacheControl.unRegisterCache(this);
    }

}

package org.broadinstitute.gpinformatics.infrastructure.jmx;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

/**
 * Objects that want to register with the external data cache control must implement this API.
 */
public abstract class AbstractCache {

    private boolean  needsRefresh = true;

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

    protected boolean shouldReFresh(Deployment deployment) {
        return needsRefresh && (deployment != Deployment.DEV);
    }

    protected void setNeedsRefresh(boolean needsRefresh) {
        this.needsRefresh = needsRefresh;
    }
}

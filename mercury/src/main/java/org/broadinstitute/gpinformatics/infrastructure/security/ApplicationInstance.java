package org.broadinstitute.gpinformatics.infrastructure.security;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

/**
 * Use this to specify an instance of mercury.  Currently there are two mercury instances, this list
 * may grow in the future.
 */
public enum ApplicationInstance {
    /** The non-CLIA instance of Mercury */
    RESEARCH;


    /**
     * @return true if this is the currently running instance of mercury
     */
    public boolean isCurrent() {
        return (this == ApplicationInstance.RESEARCH);
    }
}

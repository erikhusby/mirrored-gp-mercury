package org.broadinstitute.gpinformatics.infrastructure.security;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

/**
 * Application Context helps define a targeted instance of mercury.  Currently there are 2 mercury deployments, it is
 * conceivable that this list can grow in the future.
 */
public enum ApplicationInstance {
    CRSP, RESEARCH;


    public boolean isContextSupported() {
        return ((ApplicationInstance.RESEARCH == this) && !Deployment.isCRSP) ||
               ((ApplicationInstance.CRSP == this) && Deployment.isCRSP);
    }
}

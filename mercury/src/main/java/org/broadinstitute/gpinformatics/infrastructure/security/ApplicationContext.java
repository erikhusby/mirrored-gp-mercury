package org.broadinstitute.gpinformatics.infrastructure.security;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;

/**
 * Application Context helps define a targeted instance of mercury.  Currently there are 2 mercury deployments, it is
 * conceivable that this list can grow in the future.
 */
public enum ApplicationContext {
    CRSP, RESEARCH;


    public static boolean isContextSupported(String context) {
        return ((ApplicationContext.RESEARCH == ApplicationContext.valueOf(context)) && !Deployment.isCRSP) ||
               ((ApplicationContext.CRSP == ApplicationContext.valueOf(context)) && Deployment.isCRSP);
    }
}

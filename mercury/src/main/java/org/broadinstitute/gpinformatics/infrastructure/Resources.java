package org.broadinstitute.gpinformatics.infrastructure;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

public class Resources {

    @Produces
    public Log produceLog(InjectionPoint injectionPoint) {
        return LogFactory.getLog(injectionPoint.getMember().getDeclaringClass());
    }
}

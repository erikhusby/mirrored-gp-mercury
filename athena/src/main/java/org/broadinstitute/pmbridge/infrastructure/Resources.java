package org.broadinstitute.pmbridge.infrastructure;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 6/28/12
 * Time: 1:16 PM
 * @author sequeL
 */

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
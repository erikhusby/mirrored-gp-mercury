package org.broadinstitute.sequel.presentation.zims;

import org.apache.commons.collections15.map.LRUMap;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.HashMap;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author breilly
 */
@Qualifier
@Retention(RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
public @interface IlluminaRunQueryCache {
}

class Cache {
    @Produces @ApplicationScoped @IlluminaRunQueryCache
    LRUMap<String, ZimsIlluminaRun> runCache = new LRUMap<String, ZimsIlluminaRun>();
}
package org.broadinstitute.sequel.infrastructure.deployment;


import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Qualifier
@Retention(RetentionPolicy.RUNTIME)
@Target({METHOD, FIELD, PARAMETER, TYPE})
/**
 * Qualifier used to annotate the real implementation of a service or to specify the injection of
 * that real version.  See {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl} for an example of
 * a real service implementation annotated with {@link Impl}, and
 * {@link org.broadinstitute.sequel.boundary.pass.PassServiceProducer#impl} for an injection point that specifies the
 * {@link Impl}-annotated version of the service.
 */
public @interface Impl {
}

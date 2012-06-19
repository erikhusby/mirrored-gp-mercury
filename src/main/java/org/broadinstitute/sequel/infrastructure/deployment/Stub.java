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
 * Qualifier used to annotate the stub version of a service or to specify the injection of
 * that stub version.  See {@link org.broadinstitute.sequel.boundary.pass.PassServiceStub} for an example of
 * a stub service implementation annotated with {@link Stub}, and
 * {@link org.broadinstitute.sequel.boundary.pass.PassServiceProducer#stub} for an injection point that specifies the
 * {@link Stub}-annotated version of the service.
 */

public @interface Stub {
}

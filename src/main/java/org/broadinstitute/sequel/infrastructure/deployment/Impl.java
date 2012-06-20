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
 *
 * Qualifier used to annotate the real implementation of a service or to specify the injection of
 * that real version.  See {@link org.broadinstitute.sequel.boundary.pass.PassSOAPServiceImpl} for an example of
 * a real service implementation annotated with {@link Impl}, and
 * {@link org.broadinstitute.sequel.boundary.pass.PassServiceProducer#impl} for an injection point that specifies the
 * {@link Impl}-annotated version of the service.
 *
 *
 * Note that if the current {@link Deployment} is {@link Deployment#STUBBY}, the configuration parameters for an
 * {@link Impl} are probably nonsensical and will result in an unusable version of the service being injected to an
 * {@link Impl}-annotated injection point.  If a
 * usable real implementation of a service is needed in a {@link Deployment#STUBBY} environment, the specific
 * configuration of that service can be specified by the use of one of the {@link Qualifier}s: {@link DevInstance},
 * {@link TestInstance}, {@link QAInstance}, or {@link ProdInstance}.  See {@link SquidSOAPTest} for an example of an
 * Arquillian-deployed integration test that specifically targets the {@link Deployment#TEST} instance of Squid.
 *
 */
public @interface Impl {
}

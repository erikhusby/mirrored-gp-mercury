package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;


@Alternative
@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
@Deprecated
/**
 * The @Alternative attribute clashes with the @Default attribute required for CDI to choose the
 *   default implementation at deploy - the deploy fails
 * WELD-001408: Unsatisfied dependencies for type SquidConnector with qualifiers @Default
 */
public @interface Impl {}
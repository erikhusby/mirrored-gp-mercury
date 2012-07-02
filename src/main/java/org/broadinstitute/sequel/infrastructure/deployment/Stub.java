package org.broadinstitute.sequel.infrastructure.deployment;

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
public @interface Stub {}
package org.broadinstitute.gpinformatics.infrastructure.deployment;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Tags a CDI managed bean as an @Alternative so it's instantiated in deployments
 * using a beans.xml file containing an &lt;alternates&gt; section.
 **/
@Alternative
@Stereotype
@Target(TYPE)
@Retention(RUNTIME)
public @interface Stub {}

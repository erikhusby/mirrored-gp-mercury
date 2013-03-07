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
/**
 * TODO:  There seems to be a Weld or JBoss bug with detecting that @Stub is stereotyped as @Alternative.  To have
 * classes that are annotated as stub recognized as Alternates, pushed the @Stub to be on those specific classes.
 * This should be investigated later if seen as a hinderance.
 */
public @interface Stub {}

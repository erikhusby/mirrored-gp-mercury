package org.broadinstitute.gpinformatics.infrastructure.test;

import javax.inject.Qualifier;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
/**
 * Qualifier for test data that might be injected into a test class.
 */
public @interface TestData {
}

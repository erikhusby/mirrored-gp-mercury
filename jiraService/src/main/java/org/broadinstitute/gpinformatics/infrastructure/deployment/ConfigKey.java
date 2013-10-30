package org.broadinstitute.gpinformatics.infrastructure.deployment;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
/**
 * Used to mark an {@link AbstractConfig}-derived class with its corresponding configuration key as seen in
 * mercury-config.yaml or mercury-config-local.yaml
 */
public @interface ConfigKey {
    String value();
}

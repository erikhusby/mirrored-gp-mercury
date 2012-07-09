package org.broadinstitute.sequel.infrastructure.deployment;

import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.Stereotype;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

@Alternative
@Stereotype
@Retention(RetentionPolicy.RUNTIME)
@Target({TYPE})
public @interface ConfigKey {

    String value();
}

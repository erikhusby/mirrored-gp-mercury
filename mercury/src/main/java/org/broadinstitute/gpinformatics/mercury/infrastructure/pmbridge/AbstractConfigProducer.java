package org.broadinstitute.gpinformatics.mercury.infrastructure.pmbridge;

import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.infrastructure.deployment.SequelConfiguration;

import java.lang.reflect.ParameterizedType;


/**
 * Base class of concrete configuration producers, encapsulates reflection on the generic type variable to produce
 * the right {@link AbstractConfig}-derived type.
 *
 * @param <C> the concrete type of config to produce
 */
public abstract class AbstractConfigProducer<C extends AbstractConfig> {


    private Class<C> getTypeArgument() {

        ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
        Class<C> typeArgument = (Class<C>) parameterizedType.getActualTypeArguments()[0];

        return typeArgument;
    }

    public C produce( Deployment deployment ) {

        return (C) SequelConfiguration.getInstance().getConfig(getTypeArgument(), deployment);
    }
}

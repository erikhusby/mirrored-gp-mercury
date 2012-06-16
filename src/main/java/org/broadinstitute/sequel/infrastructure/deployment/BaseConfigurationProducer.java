package org.broadinstitute.sequel.infrastructure.deployment;


public interface BaseConfigurationProducer<T extends BaseConfiguration> {

    T devInstance();

    T testInstance();

    T qaInstance();

    T prodInstance();

    T stubInstance();

    T produce();

}


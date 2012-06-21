package org.broadinstitute.sequel.infrastructure.deployment;


/**
 * Interface for configuration or service producers, allowing for instances specified with the
 * {@link javax.inject.Qualifier}s
 * {@link DevInstance}, {@link TestInstance}, {@link QAInstance}, {@link ProdInstance}, or {@link StubInstance}, as well
 * as a non-Qualified {@link javax.enterprise.inject.Default} {@link #produce()} method.  Most injection points will
 * use the default {@link #produce()} method which will inject the implementation appropriate to the current
 * deployment as specified by <pre>SEQUEL_DEPLOYMENT</pre>.
 *
 * @param <T> The type of the configuration to be produced.
 *
 */
public interface InstanceSpecificProducer<T> extends BasicProducer<T> {

    T devInstance();

    T testInstance();

    T qaInstance();

    T prodInstance();

    T stubInstance();

}


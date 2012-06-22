package org.broadinstitute.sequel.infrastructure.common;

import org.broadinstitute.sequel.infrastructure.squid.AbstractSquidWSConnector;

import java.lang.reflect.ParameterizedType;

/**
 * 
 * The purpose of AbstractGenericsClass is only to define in a handy place for the helper method getParameterClass
 * 
 * @author Scott Matthews
 *         Date: 6/22/12
 *         Time: 3:12 PM
 */
public class AbstractGenericsClass<T> {
    /**
     * getParameterClass is a helper method to a user to access the Class type of the parameterized type {@code T}.
     * The Class type is not accessible from a generic type which makes this implementation necessary
     *
     * @return a {@link Class} object the generic parameterized type used in the definition of the concrete
     * implementation of AbstractGenericsClass
     */
    protected Class<T> getParameterClass() {
        return (Class<T>) (((ParameterizedType)AbstractSquidWSConnector.class.getGenericSuperclass()).getActualTypeArguments()[0]);
    }
}

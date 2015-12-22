package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.util.Set;

/**
 * This is a helper class to enable objects that are not capable of injecting Integration layer
 * service objects the ability to get an instance of a bean object.
 *
 * @author Scott Matthews
 */
public class ServiceAccessUtility {

    private static final Log log = LogFactory.getLog(ServiceAccessUtility.class);

    /**
     * Gets a CDI bean of the given type
     * @param beanType  class of the bean
     * @param <T> type of the bean
     * @return CDI bean
     */
    public static <T> T getBean(Class<T> beanType) {
        try {
            InitialContext initialContext = new InitialContext();
            try {
                BeanManager beanManager = (BeanManager) initialContext.lookup("java:comp/BeanManager");
                Set<Bean<?>> beans = beanManager.getBeans(beanType);
                Bean<?> bean = beanManager.resolve(beans);
                CreationalContext<?> ctx = beanManager.createCreationalContext(bean);
                //noinspection unchecked
                return (T) beanManager.getReference(bean, beanType, ctx);
            } finally {
                initialContext.close();
            }
        } catch (NamingException e) {
            throw new RuntimeException("Error trying to look up bean " + beanType.getName(), e);
        }
    }
}

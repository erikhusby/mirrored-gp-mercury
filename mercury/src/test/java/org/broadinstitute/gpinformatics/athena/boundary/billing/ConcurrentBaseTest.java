package org.broadinstitute.gpinformatics.athena.boundary.billing;

import org.broadinstitute.gpinformatics.infrastructure.test.DeploymentBuilder;
import org.jboss.arquillian.testng.Arquillian;

import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public abstract class ConcurrentBaseTest extends Arquillian {
    /**
     * Creates a new jndi context and looks up the given bean
     */
    protected static <T> T getBeanFromJNDI(Class<T> beanClass) {
        T bean = null;
        try {
            InitialContext ctx = new InitialContext();
            bean = (T)ctx.lookup("java:global/" + DeploymentBuilder.MERCURY_APP_NAME + "/" + beanClass.getSimpleName());
        }
        catch(NamingException e) {
            throw new RuntimeException("Could not lookup " + beanClass.getSimpleName() + " from jndi.",e);
        }
        return bean;
    }
}

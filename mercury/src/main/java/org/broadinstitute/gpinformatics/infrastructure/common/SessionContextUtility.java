package org.broadinstitute.gpinformatics.infrastructure.common;

import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;
import org.jboss.weld.context.bound.BoundSessionContext;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;

/**
 * Use this class if you need to execute some code inside a Session Context, when no current session context
 * exists.  This is useful in cases such as JMS message handlers and EJB Timers, where we need to call code
 * that injects {@link UserBean}.
 */
@Dependent
public class SessionContextUtility implements Serializable {
    private static final long serialVersionUID = 20130517L;
    protected final BoundSessionContext sessionContext;
    private final BeanManager beanManager;

    @Inject
    public SessionContextUtility(BoundSessionContext sessionContext,
                                 BeanManager beanManager) {
        this.sessionContext = sessionContext;
        this.beanManager = beanManager;
    }

    public interface Function {
        void apply();
    }

    /**
     * Execute a function in a session context.
     * @param function the function to execute.
     */
    public void executeInContext(Function function) {
        try {
            // Need to associate this operation with a session, to allow injection of Session scoped beans, e.g.
            // UserBean (if there is no session, the UserBean proxy will be injected, but calling any of its methods
            // causes an exception).
            sessionContext.associate(new HashMap<String, Object>());
            sessionContext.activate();

            function.apply();

        } finally {
            // There seems to be a bug in JBoss AS 7.1.1 that causes Stateful RequestScoped beans not to be destroyed
            // at the end of onMessage.  This leads to a memory leak of org.hibernate.internal.util.collections.IdentityMap.
            // If this bug is not fixed, we must manually end the Request context.
            // (Bug appears fixed in WildFly 10)

            // Meanwhile, clear the session context
            sessionContext.invalidate();
            sessionContext.deactivate();
        }
    }
}

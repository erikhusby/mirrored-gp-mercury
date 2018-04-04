package org.broadinstitute.gpinformatics.infrastructure.common;

import org.jboss.weld.context.bound.BoundSessionContext;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashMap;

/**
 * For use in Arquillian tests when the test needs to use the RequestScope (such as for calling a dao method)
 * after the class under test has invoked executeInContext().
 */

@Alternative
@Dependent
public class SessionContextUtilityKeepScope extends SessionContextUtility implements Serializable {
    private static final long serialVersionUID = 20130517L;

    @Inject
    public SessionContextUtilityKeepScope(BoundSessionContext sessionContext,
                                          BeanManager beanManager) {
        super(sessionContext, beanManager);
    }

    public void executeInContext(Function function) {
        try {
            sessionContext.associate(new HashMap<String, Object>());
            sessionContext.activate();
            function.apply();

        } finally {
            // Unlike parent class, does not terminate the Request Scope.  This permits a memory leak but that should
            // be harmless when running container tests.

            sessionContext.invalidate();
            sessionContext.deactivate();
        }
    }
}

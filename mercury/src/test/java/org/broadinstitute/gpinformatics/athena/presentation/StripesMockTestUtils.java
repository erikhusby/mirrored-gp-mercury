package org.broadinstitute.gpinformatics.athena.presentation;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.ExecutionContext;
import net.sourceforge.stripes.controller.Interceptor;
import net.sourceforge.stripes.controller.Intercepts;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.mock.MockRoundtrip;
import net.sourceforge.stripes.mock.MockServletContext;
import net.sourceforge.stripes.util.ReflectUtil;

import javax.inject.Inject;
import javax.servlet.Filter;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for using the Stripes built-in mocking facilities to test Mercury action beans.
 */
public class StripesMockTestUtils {

    /**
     * Creates a Stripes {@link net.sourceforge.stripes.mock.MockRoundtrip} for the specified Mercury action bean class.
     * Includes a Stripes ActionBeanResolution Interceptor, very much like the Stripes Injection Enricher, to inject the
     * given injectables into the action bean before the action event is fired.
     *
     * @param actionBeanClass    the class of the action bean to invoke
     * @param injectables        the objects to make available for injection into the action bean
     * @return a new Stripes MockRoundtrip object for executing events against
     */
    public static MockRoundtrip createMockRoundtrip(Class<? extends ActionBean> actionBeanClass,
                                                    Object... injectables) {
        MockServletContext servletContext = createMockServletContext();
        findInjectionInterceptor(servletContext).setInjectables(injectables);
        return new MockRoundtrip(servletContext, actionBeanClass);
    }

    /**
     * Creates a Stripes {@link net.sourceforge.stripes.mock.MockServletContext} appropriately configured for testing
     * Mercury action beans.
     *
     * @return a new Stripes MockServletContext for Mercury
     */
    public static MockServletContext createMockServletContext() {
        MockServletContext servletContext = new MockServletContext("mercury");

        // Values taken from our web.xml.
        Map<String, String> filterParams = new HashMap<>();
        filterParams.put("ActionResolver.Packages",
                "org.broadinstitute.gpinformatics.mercury.presentation,org.broadinstitute.gpinformatics.athena.presentation");
        filterParams.put("ActionBeanContext.Class",
                "org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBeanContext");

        // Special setup for container-free action bean tests to support injection into action beans.
        filterParams.put("Interceptor.Classes", InjectionInterceptor.class.getName());

        // Add the Stripes filter and dispatcher servlet.
        servletContext.addFilter(StripesFilter.class, "StripesFilter", filterParams);
        servletContext.setServlet(DispatcherServlet.class, "DispatcherServlet", null);

        return servletContext;
    }

    /**
     * Searches the given MockServletContext for the InjectionInterceptor configured as a filter parameter in
     * {@link #createMockServletContext()}.
     *
     * @param servletContext    the Stripes MockServletContext for Mercury to search
     * @return the InjectionInterceptor instance
     * @throws RuntimeException if no InjectionInterceptor is found
     */
    // Find the InjectionInterceptor on the StripesFilter to set the injectables for it to use.
    private static InjectionInterceptor findInjectionInterceptor(MockServletContext servletContext) {
        for (Filter filter : servletContext.getFilters()) {
            if (filter.getClass().equals(StripesFilter.class)) {
                StripesFilter stripesFilter = (StripesFilter) filter;
                for (Interceptor interceptor : stripesFilter.getInstanceConfiguration().getInterceptors(
                        LifecycleStage.ActionBeanResolution)) {
                    if (interceptor instanceof InjectionInterceptor) {
                        return (InjectionInterceptor) interceptor;
                    }
                }
            }
        }
        throw new RuntimeException("Could not find InjectionInterceptor in MockServletContext.");
    }

    /**
     * Stripes interceptor to perform injections into an action bean during container-free tests. Just like the
     * <a href="http://www.stripesframework.org/display/stripes/Stripes+Injection+Enricher">Stripes Injection
     * Enricher</a> that we use in productions, injections are performed using a lifecycle interceptor on
     * ActionBeanResolution after delegating creation of the action bean to Stripes. Rather than delegating to a CDI
     * bean manager like the Stripes Injection Enricher does, this interceptor uses reflection to perform injections on
     * the action bean (which is actually what the Stripes Injection Enricher does for @EJB and @Resource annotations).
     *
     * Note: This will only perform injections directly on the action bean itself and will NOT handle injections into
     * or among the objects being injected into the action bean. This is not seen as a deficiency since the objects
     * typically injected into action beans are specially created test doubles, such as mocks.
     */
    @Intercepts(LifecycleStage.ActionBeanResolution)
    public static class InjectionInterceptor implements Interceptor {

        /**
         * Objects that are available to be injected into an action bean.
         */
        private Object[] injectables;

        @Override
        public Resolution intercept(ExecutionContext context) throws Exception {

            // Delegate to Stripes to create the action bean.
            Resolution resolution = context.proceed();

            // Perform injections on the action bean using reflection.
            ActionBean actionBean = context.getActionBean();
            Collection<Field> fields = ReflectUtil.getFields(actionBean.getClass());
            for (Field field : fields) {
                if (field.isAnnotationPresent(Inject.class)) {
                    for (Object injectable : injectables) {
                        if (field.getType().isAssignableFrom(injectable.getClass())) {
                            if (!field.isAccessible()) {
                                field.setAccessible(true);
                            }
                            field.set(actionBean, injectable);
                        }
                    }
                }
            }

            return resolution;
        }

        public void setInjectables(Object[] injectables) {
            this.injectables = injectables;
        }
    }

}

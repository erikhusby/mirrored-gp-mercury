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
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.PredicateUtils;

import javax.inject.Inject;
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
        getInjectionInterceptor(getStripesFilter(servletContext)).setInjectables(injectables);
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
     * Searches the given MockServletContext for the StripesFilter configured in {@link #createMockServletContext()}.
     *
     * @param servletContext    the Stripes MockServletContext for Mercury to search
     * @return the StripesFilter instance
     * @throws RuntimeException if more or fewer than one StripesFilter is found
     */
    private static StripesFilter getStripesFilter(MockServletContext servletContext) {
        return findOnlyOfType(servletContext.getFilters(), StripesFilter.class);
    }

    /**
     * Searches the given StripesFilter for the InjectionInterceptor configured as a filter parameter in
     * {@link #createMockServletContext()}.
     *
     * @param stripesFilter    the StripesFilter to search
     * @return the InjectionInterceptor instance
     * @throws RuntimeException if more or fewer than one InjectionInterceptor is found
     */
    private static InjectionInterceptor getInjectionInterceptor(StripesFilter stripesFilter) {
        return findOnlyOfType(
                stripesFilter.getInstanceConfiguration().getInterceptors(LifecycleStage.ActionBeanResolution),
                InjectionInterceptor.class);
    }

    /**
     * Finds the only element in the given collection which matches the given predicate.
     * <p>
     * If the input collection or predicate is null, or no element of the collection matches the predicate, null is
     * returned.
     *
     * @param collection    the collection to search, may be null
     * @param predicate     the predicate to use, may be null
     * @param <T>           the type of object the {@link Iterable} contains
     * @return the only element of the collection which matches the predicate
     * @throws RuntimeException if more or fewer than one element is found
     *
     * @see org.apache.commons.collections4.CollectionUtils#find(Iterable, org.apache.commons.collections4.Predicate)
     * @see org.apache.commons.collections4.CollectionUtils#extractSingleton(java.util.Collection)
     */
    public static <T> T findOnly(Iterable<T> collection, Predicate<? super T> predicate) {
        if (collection == null || predicate == null) {
            return null;
        }
        T result = null;
        for (T item : collection) {
            if (predicate.evaluate(item)) {
                if (result != null) {
                    throw new RuntimeException("Found more than one item matching predicate: " + predicate);
                }
                result = item;
            }
        }
        if (result == null) {
            throw new RuntimeException("Could not find any item matching predicate: " + predicate);
        }
        return result;
    }

    /**
     * Finds the only element in the given collection which matches the given type.
     * <p>
     * If the input collection or type is null, or no element of the collection matches the predicate, null is returned.
     *
     * @param collection        the collection to search, may be null
     * @param type              the predicate to use, may be null
     * @param <COLLECTION_T>    the type of object the {@link Iterable} contains
     * @param <RETURN_T>        the type of object to be returned
     * @return the only element of the collection which matches the type
     *
     * @see org.apache.commons.collections4.CollectionUtils#find(Iterable, org.apache.commons.collections4.Predicate)
     * @see org.apache.commons.collections4.CollectionUtils#extractSingleton(java.util.Collection)
     */
    public static <COLLECTION_T, RETURN_T> RETURN_T findOnlyOfType(Iterable<COLLECTION_T> collection,
                                                                    Class<RETURN_T> type) {
        if (collection == null || type == null) {
            return null;
        }
        // instanceOfPredicate on type ensures that cast is OK.
        @SuppressWarnings("unchecked")
        RETURN_T result = (RETURN_T) findOnly(collection, PredicateUtils.instanceofPredicate(type));
        return result;
    }

    /**
     * Stripes interceptor to perform injections into an action bean during container-free tests. The injections are
     * performed for a fixed set of injectables specified by the test case.
     * <p>
     * Note: This will only perform injections directly on the action bean itself and will NOT handle injections into
     * or among the objects being injected into the action bean. This is not seen as a deficiency since the objects
     * typically injected into action beans are specially created test doubles, such as mocks.
     * <p>
     * Just like the <a href="http://www.stripesframework.org/display/stripes/Stripes+Injection+Enricher">Stripes
     * Injection Enricher</a> that we use in productions, injections are performed using a lifecycle interceptor on
     * ActionBeanResolution after delegating creation of the action bean to Stripes. Rather than delegating to a CDI
     * {@link javax.enterprise.inject.spi.BeanManager} like the Stripes Injection Enricher does, this interceptor uses
     * reflection to perform injections on the action bean (which is actually what the Stripes Injection Enricher does
     * for @EJB and @Resource annotations).
     * <p>
     * Even though this class is public, it is only public so that Stripes can instantiate it. It is not designed to be
     * used outside of the utility methods in
     * {@link org.broadinstitute.gpinformatics.athena.presentation.StripesMockTestUtils}. Use at your own risk.
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
                            if (field.get(actionBean) != null) {
                                throw new RuntimeException(String.format(
                                        "Encountered an injection point that multiple injectables could satisfy: %s.%s",
                                        field.getDeclaringClass().getSimpleName(), field.getName()));
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

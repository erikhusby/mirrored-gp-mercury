package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.controller.DispatcherServlet;
import net.sourceforge.stripes.controller.StripesFilter;
import org.broadinstitute.gpinformatics.infrastructure.test.ContainerTest;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.Map;

/**
 * This is used to test action beans.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class ActionBeanBaseTest<T extends ActionBean> extends ContainerTest {
    private TestCoreActionBeanContext ctx;

    private T bean;

    public ActionBeanBaseTest() {
    }

    public ActionBeanBaseTest(T actionBean) {
        this.bean = actionBean;
    }

    public TestCoreActionBeanContext getActionBeanContext() {
        return ctx;
    }

    public T getBean() {
        return bean;
    }

    @BeforeMethod(alwaysRun = true)
    public void initializeActionBean() {
        ctx = new TestCoreActionBeanContext();
        bean.setContext(ctx);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDownActionBean() {
        if (ctx != null && ctx.getServletContext().getFilters() != null) {
            for (Filter f : ctx.getServletContext().getFilters()) {
                f.destroy();
            }
        }

        if (bean.getContext() != null && bean.getContext().getRequest() != null) {
            bean.getContext().getRequest().getParameterMap().clear();
        }
    }
}
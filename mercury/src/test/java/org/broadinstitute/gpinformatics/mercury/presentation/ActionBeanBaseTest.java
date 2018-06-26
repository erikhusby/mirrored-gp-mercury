package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.action.ActionBean;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.servlet.Filter;

/**
 * This is used to test action beans.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
@Test(groups = {TestGroups.STUBBY})
@Dependent
public class ActionBeanBaseTest<T extends ActionBean> extends StubbyContainerTest {
    private TestCoreActionBeanContext ctx;

    private T bean;

    public ActionBeanBaseTest() {}

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
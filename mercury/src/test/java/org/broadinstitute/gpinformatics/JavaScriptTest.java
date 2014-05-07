package org.broadinstitute.gpinformatics;

import org.codehaus.jstestrunner.junit.JSTestSuiteRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 */
@RunWith(JSTestSuiteRunner.class)
@JSTestSuiteRunner.ContextPath("/Mercury")
//@JSTestSuiteRunner.ResourceBase({ "src/main/webapp", "src/test/js" })
@JSTestSuiteRunner.ResourceBase({ "." })
//@JSTestSuiteRunner.ResourceBase({ ".", "src/test/js" })
@JSTestSuiteRunner.Include("**/*_test.html")
public class JavaScriptTest {
}

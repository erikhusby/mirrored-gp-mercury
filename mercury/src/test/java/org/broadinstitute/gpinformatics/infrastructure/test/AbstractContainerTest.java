package org.broadinstitute.gpinformatics.infrastructure.test;


import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;

import javax.naming.InitialContext;

public class AbstractContainerTest extends Arquillian {

    // todo arz have StubbyContainerTest extend this, then recode all tests that
    // use the "if (someDao != null) return" to instead
    // use "if (!isRunningInContainer) {return;}@ArquillianResource

    @ArquillianResource
    protected InitialContext initialContext;

    /**
     * Arquillian runs before/after methods twice.  Once
     * outside the container and once inside the container.
     * Use this method to avoid NPEs on injected
     * fields in @Before/@After methods
     * @return true if the test is being run in
     * container, false if it isn't
     */
    protected boolean isRunningInContainer() {
        return initialContext != null;
    }
}

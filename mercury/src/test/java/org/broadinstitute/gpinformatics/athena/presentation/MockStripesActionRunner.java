package org.broadinstitute.gpinformatics.athena.presentation;

import net.sourceforge.stripes.mock.MockHttpServletRequest;
import net.sourceforge.stripes.mock.MockHttpServletResponse;

import javax.servlet.http.HttpServletRequest;

/**
 * Test utility that takes in a ResolutionCallback,
 * runs the underlying ActionBean method that results
 * in a Resolution, and executes the Resolution
 * using mocked request and response objects.
 */
public class MockStripesActionRunner {

    /**
     * Executes the given callback to get a Resolution,
     * and then executes the resolution using mock
     * request and responses.
     * @return a mock http response that can be interrogated
     * to get the actual String or Stream that comes
     * back to the browser
     */
    public static MockHttpServletResponse runStripesAction(ResolutionCallback resolutionCallback) throws Exception {
        HttpServletRequest request = new MockHttpServletRequest("foo","bar");
        MockHttpServletResponse response = new MockHttpServletResponse();
        resolutionCallback.getResolution().execute(request,response);
        return response;
    }
}

/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.athena.boundary.util;

import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 *
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class ExceptionThrowingTestResourceTest extends RestServiceContainerTest {

    public ExceptionThrowingTestResourceTest(){}

    /**
     * Force stubby alternatives without extending StubbyContainerTest
     * (But this class tagged with TestGroups.STUBBY so it gets rolled into whatever deployment is used for the Arquillian Suite)
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return StubbyContainerTest.buildMercuryWar();
    }

    @Override
    protected String getResourcePath() {
        return "test";
    }

    @Test(enabled = true, groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowsResourceException400(@ArquillianResource URL baseUrl) throws Exception {
        Response.Status statusUnderTest = Response.Status.BAD_REQUEST;
        testResultForStatus(baseUrl, statusUnderTest);
    }

    private void testResultForStatus(URL baseUrl, Response.Status status) throws MalformedURLException {
        String statusCode = String.valueOf(status.getStatusCode());
        WebResource resource = makeWebResource(baseUrl, "throwsResourceException")
                .queryParam(ExceptionThrowingTestResource.RESPONSE_STATUS, statusCode);

        UniformInterfaceException result = getWithError(resource);

        // These responses return jsp pages (defined in web.xml) so ignore them as the response will not be a simple
        // error string.
        if (status != Response.Status.INTERNAL_SERVER_ERROR || status != Response.Status.FORBIDDEN) {
            String content = getResponseContent(result);
            assertThat(content, equalTo("Oopsie, I threw a ResourceException"));
        }
        assertThat(result.getResponse().getClientResponseStatus().getStatusCode(), equalTo(status.getStatusCode()));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowsResourceException403(@ArquillianResource URL baseUrl) throws Exception {
        Response.Status statusUnderTest = Response.Status.GONE;
        testResultForStatus(baseUrl, statusUnderTest);
    }


    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowsResourceException500(@ArquillianResource URL baseUrl) throws Exception {
        Response.Status statusUnderTest = Response.Status.INTERNAL_SERVER_ERROR;
        testResultForStatus(baseUrl, statusUnderTest);
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowsEJBException(@ArquillianResource URL baseUrl) throws Exception {
        WebResource resource = makeWebResource(baseUrl, "throwsEJBException");
        UniformInterfaceException result = getWithError(resource);
        String content = getResponseContent(result);

        assertThat(result.getResponse().getClientResponseStatus().getStatusCode(),
                equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
    }

    @Test(groups = TestGroups.STUBBY, dataProvider = ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testThrowsInformaticsServiceException(@ArquillianResource URL baseUrl) throws Exception {
        WebResource resource = makeWebResource(baseUrl, "throwsInformaticsServiceException");
        UniformInterfaceException result = getWithError(resource);
        String content = getResponseContent(result);

        assertThat(content, equalTo("Oopsie, I threw an InformaticsServiceException"));

        assertThat(result.getResponse().getClientResponseStatus().getStatusCode(),
                equalTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));

    }

}

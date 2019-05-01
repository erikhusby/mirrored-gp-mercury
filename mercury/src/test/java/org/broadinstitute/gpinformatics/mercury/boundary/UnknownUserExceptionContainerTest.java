package org.broadinstitute.gpinformatics.mercury.boundary;

import org.broadinstitute.gpinformatics.infrastructure.test.StubbyContainerTest;
import org.broadinstitute.gpinformatics.infrastructure.test.TestGroups;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.arquillian.testng.Arquillian;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.testng.annotations.Test;

import javax.enterprise.context.Dependent;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

/**
 * Container test for {@link UnknownUserException} to make sure that it returns the appropriate status code and content.
 */
@Test(groups = TestGroups.STUBBY)
@Dependent
public class UnknownUserExceptionContainerTest extends RestServiceContainerTest {

    public UnknownUserExceptionContainerTest(){}

    /**
     * Force stubby alternatives without extending StubbyContainerTest
     * (But this class tagged with TestGroups.STUBBY so it gets rolled into whatever deployment is used for the Arquillian Suite)
     */
    @Deployment
    public static WebArchive buildMercuryWar() {
        return StubbyContainerTest.buildMercuryWar();
    }

    /**
     * Test resource that always throws {@link UnknownUserException} with the username from the request.
     */
    @Path(UnknownUserExceptionContainerTestResource.RESOURCE_PATH)
    public static class UnknownUserExceptionContainerTestResource {

        public static final String RESOURCE_PATH = "UnknownUserExceptionContainerTestResource";
        public static final String METHOD_PATH = "throwUnknownUserException";

        @GET
        @Path(METHOD_PATH)
        public void throwUnknownUserException(@QueryParam("username") String username) {
            throw new UnknownUserException(username);
        }
    }


    @Override
    protected String getResourcePath() {
        return UnknownUserExceptionContainerTestResource.RESOURCE_PATH;
    }

    /**
     * Test that throwing {@link UnknownUserException} from a web service results in a 304 (FORBIDDEN) response and
     * content that contains an error message including the username from the request.
     */
    @Test(dataProvider = Arquillian.ARQUILLIAN_DATA_PROVIDER)
    @RunAsClient
    public void testUnknownUserExceptionReturns304Response(@ArquillianResource URL baseUrl)
            throws MalformedURLException {
        WebTarget resource = makeWebResource(baseUrl, UnknownUserExceptionContainerTestResource.METHOD_PATH);
        String username = "test_user";
        resource = resource.queryParam("username", username);

        WebApplicationException error = getWithError(resource);
        Response response = error.getResponse();

        assertThat(response.getStatus(), equalTo(Response.Status.FORBIDDEN.getStatusCode()));
        assertThat(response.readEntity(String.class), containsString(username));
    }
}

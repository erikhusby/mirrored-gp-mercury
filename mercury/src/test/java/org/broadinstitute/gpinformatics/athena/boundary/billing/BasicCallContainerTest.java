package org.broadinstitute.gpinformatics.athena.boundary.billing;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.mercury.integration.RestServiceContainerTest;

import java.net.URL;

/**
 * TODO scottmat fill in javadoc!!!
 */
public abstract class BasicCallContainerTest extends RestServiceContainerTest {

    protected WebResource makeWebResource(URL baseUrl, String serviceUrl) {
        return Client.create(clientConfig).resource(baseUrl + "Mercury/" + getResourcePath() + "/" + serviceUrl);
    }

}

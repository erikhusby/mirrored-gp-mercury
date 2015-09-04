package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import com.sun.jersey.api.client.Client;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJsonJerseyClientService;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;

/**
 * TODO scottmat fill in javadoc!!!
 */
public interface SalesforceService extends Serializable {
    void pushProduct(String exomeExpressV2PartNumber) throws URISyntaxException, IOException;
}

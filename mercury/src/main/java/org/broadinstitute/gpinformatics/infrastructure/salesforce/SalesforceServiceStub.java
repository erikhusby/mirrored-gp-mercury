package org.broadinstitute.gpinformatics.infrastructure.salesforce;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Stub
@Alternative
public class SalesforceServiceStub implements SalesforceService {
    @Override
    public void pushProducts() throws URISyntaxException, IOException {

    }

    @Override
    public void pushProduct(String exomeExpressV2PartNumber) {

    }
}

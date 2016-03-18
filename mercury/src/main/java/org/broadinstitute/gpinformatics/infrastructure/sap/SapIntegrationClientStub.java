package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.sapservices.SapIntegrationClient;

import javax.enterprise.inject.Alternative;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Stub
@Alternative
public class SapIntegrationClientStub implements SapIntegrationClient {
    @Override
    public String testConnection(String age) {
        return null;
    }
}

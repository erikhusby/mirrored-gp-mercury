package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;
import org.broadinstitute.sapservices.SapIntegrationClient;

import javax.enterprise.inject.Alternative;

@Stub
@Alternative
public class SapIntegrationClientStub implements SapIntegrationService {
    @Override
    public String submitAge(String age) {
        return "What? Just "+age+" - Great !";
    }
}

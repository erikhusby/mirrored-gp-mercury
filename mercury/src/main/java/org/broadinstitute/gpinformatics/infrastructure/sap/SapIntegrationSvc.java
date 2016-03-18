package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.sapservices.SapIntegrationClient;
import org.broadinstitute.sapservices.SapIntegrationClientImpl;

import javax.inject.Inject;
import java.io.IOException;

@Impl
public class SapIntegrationSvc implements SapIntegrationClient {

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationClientImpl.class);

    @Inject
    public SapIntegrationSvc(SapConfig sapConfig) {
        wrappedClient = new SapIntegrationClientImpl(sapConfig.getLogin(), sapConfig.getPassword(),
                sapConfig.getBaseUrl(), sapConfig.getWsdlUri(), "src/main/wsdl/sap/sap_test_service.wsdl");
    }

    @Override
    public String ageSubmission(String age) throws IOException {
        return wrappedClient.ageSubmission(age);
    }
}

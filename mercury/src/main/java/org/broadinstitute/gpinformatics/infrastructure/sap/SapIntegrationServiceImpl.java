package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.sapservices.SapIntegrationClientImpl;

import javax.inject.Inject;
import java.io.IOException;

@Impl
public class SapIntegrationServiceImpl implements SapIntegrationService {

    @Inject
    private SapConfig sapConfig;

    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationServiceImpl.class);

    public SapIntegrationServiceImpl() {
    }

    public SapIntegrationServiceImpl(SapConfig sapConfigIn) {
        initializeClient(sapConfigIn);
    }

    private void initializeClient(SapConfig sapConfigIn) {
        wrappedClient = new SapIntegrationClientImpl(sapConfigIn.getLogin(), sapConfigIn.getPassword(),
                sapConfigIn.getBaseUrl(), sapConfigIn.getWsdlUri(), "/wsdl/sap/sap_test_service.wsdl");
    }

    @Override
    public String submitAge(String age) throws IOException {
        if(wrappedClient == null ) {
            initializeClient(this.sapConfig);
        }
        return wrappedClient.ageSubmission(age);
    }
}

package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.sapservices.SapIntegrationClient;
import org.broadinstitute.sapservices.SapIntegrationClientImpl;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

@Impl
public class SapIntegrationSvc implements SapIntegrationClient {

    private SapConfig sapConfig;
    private SapIntegrationClientImpl wrappedClient;

    private final static Log log = LogFactory.getLog(SapIntegrationClientImpl.class);

    @Inject
    public SapIntegrationSvc(SapConfig sapConfig) {
        this.sapConfig = sapConfig;
        wrappedClient = new SapIntegrationClientImpl(sapConfig.getLogin(), sapConfig.getPassword(),
                sapConfig.getBaseUrl(), sapConfig.getWsdlUri());
    }

    @Override
    public String testConnection(String age) throws IOException {

        return wrappedClient.testConnection(age);
    }
}

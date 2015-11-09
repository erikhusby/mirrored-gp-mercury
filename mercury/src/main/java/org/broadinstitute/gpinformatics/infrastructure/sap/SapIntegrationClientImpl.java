package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Impl
public class SapIntegrationClientImpl implements SapIntegrationClient {


    @Inject
    private SapConfig sapConfig;

    private ZTRY2 getSapService() {
        String namespace = "urn:sap-com:document:sap:soap:functions:mc-style";
        QName serviceName = new QName(namespace, "ztry2");

        URL url;

        try {
            url = new URL(sapConfig.getWsdlPath());
        } catch (MalformedURLException e) {
            throw new InformaticsServiceException(e);
        }

        Service service = Service.create(url, serviceName);

        return service.getPort(serviceName, ZTRY2.class);
    }


    @Override
    public String testConnection(String age) {

        return getSapService().zuwebrfctry2(age);

    }

}

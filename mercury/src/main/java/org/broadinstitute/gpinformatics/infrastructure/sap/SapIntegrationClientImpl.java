package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Impl
public class SapIntegrationClientImpl implements SapIntegrationClient {

    private SapConfig sapConfig;

//    private final static Log log = LogFactory.getLog(SapIntegrationClientImpl.class);

    @Inject
    public SapIntegrationClientImpl(SapConfig sapConfig) {
        this.sapConfig = sapConfig;
    }

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


        ZTRY2 sapService = getSapService();

        Map<String, List<String>> credentials = new HashMap<>();
//        log.error("Using a login of: " + sapConfig.getLogin());
        credentials.put("Username", Collections.singletonList(sapConfig.getLogin()));
//        log.error("Using a password of: " + sapConfig.getPassword());
        credentials.put("Password", Collections.singletonList(sapConfig.getPassword()));

        ((BindingProvider) sapService).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, credentials);
        ((BindingProvider) sapService).getRequestContext()
                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sapConfig.getWsdlPath());

        return sapService.zuwebrfctry2(age);

    }

}

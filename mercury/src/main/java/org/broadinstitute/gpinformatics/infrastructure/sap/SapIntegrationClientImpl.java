package org.broadinstitute.gpinformatics.infrastructure.sap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;

import javax.inject.Inject;
import javax.xml.namespace.QName;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.Service;
import javax.xml.ws.handler.MessageContext;
import java.io.File;
import java.io.IOException;
import java.net.Authenticator;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Impl
public class SapIntegrationClientImpl implements SapIntegrationClient {

    private SapConfig sapConfig;

    private final static Log log = LogFactory.getLog(SapIntegrationClientImpl.class);

    @Inject
    public SapIntegrationClientImpl(SapConfig sapConfig) {
        this.sapConfig = sapConfig;
    }

    private ZTRY2 getSapService() throws IOException{
        String namespace = "urn:sap-com:document:sap:soap:functions:mc-style";
        QName serviceName = new QName(namespace, "ZTRY2");

        URL url;

        try {
            url = new URL(sapConfig.getWsdlPath());
        } catch (MalformedURLException e) {
            throw new InformaticsServiceException(e);
        }

//        Authenticator.setDefault(new Authenticator() {
//            @Override
//            protected PasswordAuthentication getPasswordAuthentication() {
//                return new PasswordAuthentication(sapConfig.getLogin(), sapConfig.getPassword().toCharArray());
//            }
//        });

//        Service service = Service.create(url, serviceName);
        Service service = Service.create(new File("src/main/wsdl/sap/sap_test_service.wsdl").toURI().toURL(), serviceName);

        return service.getPort(serviceName, ZTRY2.class);
    }


    @Override
    public String testConnection(String age) throws IOException {

        ZTRY2 sapService = getSapService();

//        Map<String, List<String>> credentials = new HashMap<>();
//        log.error("Using a login of: " + sapConfig.getLogin());
//
//        credentials.put("username", Collections.singletonList(sapConfig.getLogin()));
//
//        log.error("Using a password of: " + sapConfig.getPassword());
//
//        credentials.put("password", Collections.singletonList(sapConfig.getPassword()));
//
//        ((BindingProvider) sapService).getRequestContext().put(MessageContext.HTTP_REQUEST_HEADERS, credentials);
//        ((BindingProvider) sapService).getRequestContext()
//                .put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, sapConfig.getWsdlPath());

        ((BindingProvider) sapService).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, sapConfig.getLogin());
        ((BindingProvider) sapService).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, sapConfig.getPassword());
//        ((BindingProvider) sapService).getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "GPCOMU");
//        ((BindingProvider) sapService).getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "Enj0ySAP!");

        return sapService.zuwebrfctry2(age);

    }

}

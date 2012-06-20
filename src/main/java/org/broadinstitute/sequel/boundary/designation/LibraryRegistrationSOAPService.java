package org.broadinstitute.sequel.boundary.designation;

import org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType;
import org.broadinstitute.sequel.boundary.squid.SequelLibrary;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfiguration;
import org.broadinstitute.sequel.infrastructure.squid.SquidConfigurationJNDIProfileDrivenImpl;

import javax.jws.WebParam;
import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Scott Matthews
 *         Date: 6/20/12
 *         Time: 4:30 PM
 */
public class LibraryRegistrationSOAPService implements LibraryRegistrationPortType {


    private SquidConfiguration squidConfiguration = new SquidConfigurationJNDIProfileDrivenImpl();


    private org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType squidServicePort;

    private org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType squidCall() {

        if (squidServicePort == null) {
            String namespace = "urn:ExtLibraryRegistration";
            QName serviceName = new QName(namespace, "ExtLibraryRegistrationService");

            String wsdlURL = squidConfiguration.getBaseURL() + "services/ExtLibraryRegistrationService?WSDL";

            URL url;
            try {
                url = new URL(wsdlURL);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            Service service = Service.create(url, serviceName);
            squidServicePort = service.getPort(serviceName, org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType.class);
        }

        return squidServicePort;

    }



    @Override
    public void registerSequeLLibrary(@WebParam(name = "registrationContextIn",
                                                partName = "registrationContextIn") SequelLibrary registrationContextIn) {
        this.squidCall().registerSequeLLibrary(registrationContextIn);
    }

    @Override
    public void registerForDesignation(@WebParam(name = "libraryName", partName = "libraryName") String libraryName,
                                       @WebParam(name = "lanes", partName = "lanes") int lanes,
                                       @WebParam(name = "readLength", partName = "readLength") int readLength, @WebParam(name = "needsControlLane",
                                                                                                                         partName = "needsControlLane") boolean needsControlLane) {
        this.squidCall().registerForDesignation(libraryName, lanes, readLength, needsControlLane);
    }
}

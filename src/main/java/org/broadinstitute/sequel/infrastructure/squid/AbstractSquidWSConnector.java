package org.broadinstitute.sequel.infrastructure.squid;

import org.broadinstitute.sequel.infrastructure.common.AbstractGenericsClass;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * AbstractSquidWSConnector defines the generic set of steps necessary for the SequeL application to connect to a known
 * webservice port exposed by the Squid application.
 *
 * Since the portTypes are generated and do not extend a common class or interface, we are able to share the logic
 * necessary for retrieving a desired port type utilizing Generics.
 *
 * Concrete implementations of this class will define the specific port type {@code T} to be used as well as the port
 * configuration parameters for the associated service.
 *
 * @author Scott Matthews
 *         Date: 6/22/12
 *         Time: 1:35 PM
 */
public abstract class AbstractSquidWSConnector<T> extends AbstractGenericsClass<T> {

    private SquidConfiguration squidConfiguration = new SquidConfigurationJNDIProfileDrivenImpl();
    private T squidServicePort;

    private String squidNameSpace;
    private String squidServiceName;
    private String serviceWsdlLocation;

    /**
     *
     * Constructor which exposes to its subclasses a way to set port configuration parameters for initializing the
     * port specified by {@code T}
     *
     * @param squidNameSpaceIn A String representing the Name Space defined in the WSDL associated with port type {@code T}
     * @param squidServiceNameIn A String representing the Service name associated with the WebService for port type {@code T}
     * @param serviceWsdlLocationIn A string representing the location, relative to the Squid base URL, of the WSDL for port type {@code T}
     */
    protected AbstractSquidWSConnector(String squidNameSpaceIn, String squidServiceNameIn,
                                       String serviceWsdlLocationIn) {
        squidNameSpace = squidNameSpaceIn;
        squidServiceName = squidServiceNameIn;
        serviceWsdlLocation = serviceWsdlLocationIn;

    }

    /**
     *
     * squidCall gives a client access to a Port type instance {@code T} defined by the Concrete class of
     * AbstractSquidWSConnector
     *
     * @return An instance of port type {@code T}.
     */
    public T squidCall() {

        initializePort();
        return squidServicePort;
    }

    /**
     * initializePort is a helper method to create a usable instance of port type {@code T}.  The instance is base on
     * not only the port type defined by the Concrete implementation of AbstractSquidWSConnector but also the
     * connection parameters defined by the concrete implementation
     */
    private void initializePort() {
        if (squidServicePort == null) {
            String namespace = this.squidNameSpace;
            QName serviceName = new QName(namespace, this.squidServiceName);

            String wsdlURL = squidConfiguration.getBaseURL() + this.serviceWsdlLocation;

            URL url;
            try {
                url = new URL(wsdlURL);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            Service service = Service.create(url, serviceName);
            squidServicePort = service.getPort(serviceName, getParameterClass());
        }
    }

}

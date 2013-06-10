package org.broadinstitute.gpinformatics.infrastructure;

import javax.xml.namespace.QName;
import javax.xml.ws.Service;
import java.lang.reflect.ParameterizedType;
import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * AbstractWebServiceClient defines the generic set of steps necessary for the Mercury application to connect to a known
 * webservice port exposed by an application.
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
public abstract class AbstractWebServiceClient<T> {
    private T servicePort;

    protected abstract String getBaseUrl();

    /**
     *
     * @return A String representing the Name Space defined in the WSDL associated with port type {@code T}
     */
    protected abstract String getNameSpace();

    /**
     *
     * @return A String representing the Service name associated with the WebService for port type {@code T}
     */
    protected abstract String getServiceName();

    /**
     *
     * @return A string representing the location, relative to the base URL, of the WSDL for port type {@code T}
     */
    protected abstract String getWsdlLocation();

    /**
     * wsCall gives a client access to a Port type instance {@code T} defined by the Concrete class.
     *
     * @return An instance of port type {@code T}.
     */
    public T wsCall() {
        initializePort();
        return servicePort;
    }

    /**
     * initializePort is a helper method to create a usable instance of port type {@code T}.  The instance is base on
     * not only the port type defined by the Concrete implementation of the class but also the connection parameters
     * defined by the concrete implementation.
     */
    private void initializePort() {
        if (servicePort == null) {
            String namespace = getNameSpace();
            QName serviceName = new QName(namespace, getServiceName());

            String wsdlURL = getBaseUrl() + getWsdlLocation();

            URL url;
            try {
                url = new URL(wsdlURL);
            }
            catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }

            Service service = Service.create(url, serviceName);

            ParameterizedType parameterizedType = (ParameterizedType) getClass().getGenericSuperclass();
            Class<T> typeArgument = (Class<T>) parameterizedType.getActualTypeArguments()[0];
            servicePort = service.getPort(serviceName, typeArgument);
        }
    }
}

package org.broadinstitute.sequel.infrastructure.squid;

import org.broadinstitute.sequel.boundary.squid.LibraryRegistrationPortType;

import javax.enterprise.context.SessionScoped;
import java.io.Serializable;

/**
 * @author Scott Matthews
 *         Date: 6/22/12
 *         Time: 2:24 PM
 */
@SessionScoped
public class LibraryRegistrationSquidWSConnector
        extends AbstractSquidWSConnector<LibraryRegistrationPortType>
        implements Serializable {

    @Override
    protected String getNameSpace() {
        return "urn:ExtLibraryRegistration";
    }

    @Override
    protected String getServiceName() {
        return "ExtLibraryRegistrationService";
    }

    @Override
    protected String getWsdlLocation() {
        return "services/ExtLibraryRegistrationService?WSDL";
    }


}

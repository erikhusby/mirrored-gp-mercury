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

    public LibraryRegistrationSquidWSConnector() {
        super("urn:ExtLibraryRegistration",
              "ExtLibraryRegistrationService",
              "services/ExtLibraryRegistrationService?WSDL");
    }

}

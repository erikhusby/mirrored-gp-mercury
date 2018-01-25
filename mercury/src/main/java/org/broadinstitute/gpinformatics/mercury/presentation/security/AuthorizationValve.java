package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.apache.catalina.connector.Request;
import org.apache.catalina.deploy.LoginConfig;
import org.jboss.as.web.security.ExtendedFormAuthenticator;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Supports mixing FORM and BASIC authentication in the same web application.
 */
public class AuthorizationValve extends ExtendedFormAuthenticator {
    @Override
    public boolean authenticate(Request request, HttpServletResponse response, LoginConfig config) throws IOException {
        return super.authenticate(request, response, config);
    }
}

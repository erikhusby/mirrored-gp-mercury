package org.broadinstitute.gpinformatics.mercury.presentation.security;

import org.owasp.csrfguard.CsrfGuard;
import org.owasp.csrfguard.CsrfGuardException;
import org.owasp.csrfguard.action.AbstractAction;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Differs from the default CSRFGuard Redirect class by caching request parameters, to allow POST to continue after
 * login.
 */
public class CsrfGuardRedirect extends AbstractAction {

    private static final long serialVersionUID = 4755524260317241461L;

    @Override
    public void execute(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse,
            CsrfGuardException e, CsrfGuard csrfGuard) throws CsrfGuardException {

        String errorPage = getParameter("Page");
        AuthorizationFilter.cacheParameters(httpServletRequest);

        try {
            httpServletResponse.sendRedirect(errorPage);
        } catch (IOException var7) {
            throw new CsrfGuardException(var7);
        }
    }
}

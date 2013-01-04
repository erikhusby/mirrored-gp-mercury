package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import net.sourceforge.stripes.util.Log;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationManager;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Comma separated list of groups that this block will allow access.  Using "All" will mean all groups have access.
 */
public class AuthorizeBlockStripesTag extends TagSupport {
    private static Log log = Log.getInstance(AuthorizeBlockStripesTag.class);

    private static final long serialVersionUID = 20090901L;

    private List<String> roles;

    private List<String> exclusionRoles;

    @Inject
    AuthorizationManager authManager;

    public AuthorizeBlockStripesTag() {
        super();
        initValues();
    }

    public String[] getRoles() {
        return (String[]) roles.toArray();
    }

    public void setRoles(String[] roles) {
        this.roles = Arrays.asList(roles);
    }

    public String[] getExclusionRoles() {
        return (String[]) exclusionRoles.toArray();
    }

    public void setExclusionRoles(String[] exclusionRoles) {
        this.exclusionRoles = Arrays.asList(exclusionRoles);
    }

    /**
     * Process the tag. The exclusion tag takes precedence over the inclusion
     * role list. If an exception occurs then the default result is to skip the
     * body.
     *
     */
    @Override
    public int doStartTag() throws JspException {
        if (roles == null || roles.isEmpty())
            return SKIP_BODY;

        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            String pageUri = request.getServletPath();

            // exclusion tag check
            if (!exclusionRoles.isEmpty()) {
                if (authManager.isUserAuthorized(pageUri, request)) {
                    return SKIP_BODY;
                }
            }

            // now check the roles to include the content
            if (!roles.isEmpty()) {
                if (authManager.isUserAuthorized(pageUri, request)) {
                    return EVAL_BODY_INCLUDE;
                }
            }
        } catch (Exception e) {
            log.warn("Problem determining if the user was in the role", e);
        }

        return super.doStartTag();
    }


    /**
     * Initialize the arrays and variables.
     */
    private void initValues() {
        roles = new ArrayList<String>();
        exclusionRoles = new ArrayList<String>();
    }
}

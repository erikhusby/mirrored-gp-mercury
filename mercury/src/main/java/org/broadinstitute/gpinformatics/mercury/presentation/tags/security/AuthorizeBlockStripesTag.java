package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * As input this tag supports a comma separated list of roles that this block will allow access.  Using "All" will
 * mean all roles have access. The tag also supports "context", which lets the user specify CRSP or RESEARCH. If
 * context is omitted, the block will be shown in both instances.
 * <p/>
 * Below is an example of how it is used.
 * <p/>
 * {@code <%@ taglib uri="http://mercury.broadinstitute.org/Mercury/security"" prefix="security"%>}<br/>
 * {@code <%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.*" %>}<br/>
 * {@code <%@ page import="static org.broadinstitute.gpinformatics.infrastructure.security.Role.roles" %>}<br/>
 * {@code<security:authorizeBlock roles="<%= roles(LabUser, LabManager, Developer) %>">}<br/>
 * {@code     Secured content goes in here}<br/>
 * {@code </security:authorizeBlock>}
 */
public class AuthorizeBlockStripesTag extends TagSupport {

    public static final String ALLOW_ALL_ROLES = "All";

    private static final long serialVersionUID = 201300107L;

    @Inject
    UserBean userBean;

    private String[] roles;

    public String[] getRoles() {
        return roles;
    }

    public void setRoles(String[] roles) {
        this.roles = roles;
    }

    /**
     * Process the tag. The exclusion tag takes precedence over the inclusion
     * role list. If an exception occurs then the default result is to skip the
     * body.
     */
    @Override
    public int doStartTag() throws JspException {
        if (roles == null) {
            return SKIP_BODY;
        }

        // Now check the roles to include the jsp code block.
        for (String role : roles) {
            if (userBean.isUserInRole(role) || role.equals(ALLOW_ALL_ROLES)) {
                // User in role or all roles allowed.
                return EVAL_BODY_INCLUDE;
            }
        }

        return super.doStartTag();
    }
}

package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import net.sourceforge.stripes.util.Log;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;

/**
 * Comma separated list of roles that this block will allow access.  Using "All" will mean all roles have access.
 * <p/>
 * Below is an example of how it is used. You will need to define your own tld.
 * <p/>
 * <br/>
 * {@code
 * <%@ taglib uri="http://www.broadinstitute.org/Mercury/AuthorizeBlock"" prefix="security"%>
 * <security:authorizeBlock roles={"Administrator", "Project Manager"}>
 *      Secured content goes in here
 * </security:authorizeBlock>
 * }
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class AuthorizeBlockStripesTag extends TagSupport {
    private static final Log log = Log.getInstance(AuthorizeBlockStripesTag.class);

    private static final long serialVersionUID = 201300107L;

    private String[] roles;

    public static final String ALLOW_ALL_ROLES = "All";

    @Inject
    UserBean userBean;

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

        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

            // Now check the roles to include the jsp code block.
            for (String role : roles) {
                if (userBean.isUserInRole(role) || role.equals(ALLOW_ALL_ROLES)) {
                    // User in role or all roles allowed.
                    return EVAL_BODY_INCLUDE;
                }
            }
        } catch (Exception e) {
            log.warn("Problem determining if the user was in the role", e);
        }

        return super.doStartTag();
    }
}

package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import net.sourceforge.stripes.util.Log;
import org.broadinstitute.gpinformatics.mercury.entity.DB;

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
 * <security:authorizeBlock roles={"Administrator", "Project Manager"} exclusionRoles={"Foobar"}>
 * Secured content goes in here
 * </security:authorizeBlock>
 * }
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class AuthorizeBlockStripesTag extends TagSupport {
    private static final Log log = Log.getInstance(AuthorizeBlockStripesTag.class);

    private static final long serialVersionUID = 201300107L;

    private String roles;

    private String exclusionRoles;

    public String getRoles() {
        return roles;
    }

    public void setRoles(String roles) {
        this.roles = roles;
    }

    public String getExclusionRoles() {
        return exclusionRoles;
    }

    public void setExclusionRoles(String exclusionRoles) {
        this.exclusionRoles = exclusionRoles;
    }

    /**
     * Process the tag. The exclusion tag takes precedence over the inclusion
     * role list. If an exception occurs then the default result is to skip the
     * body.
     */
    @Override
    public int doStartTag() throws JspException {
        if (roles == null && exclusionRoles == null) {
            return SKIP_BODY;
        }

        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();

            // Exclusion tag check to skip the jsp code block.
            if (exclusionRoles != null) {
                String[] splitRoles = exclusionRoles.split(",");
                for (String role : splitRoles) {
                    DB.Role dbRole = DB.Role.valueOf(role.trim());
                    if (request.isUserInRole(dbRole.name) || (dbRole == DB.Role.All)) {
                        // User has a role that should be skipped or "All" roles to be excluded (?).
                        return SKIP_BODY;
                    }
                }
            }

            // Now check the roles to include the jsp code block.
            if (roles != null) {
                String[] splitRoles = roles.split(",");
                for (String role : splitRoles) {
                    DB.Role dbRole = DB.Role.valueOf(role.trim());
                    if (request.isUserInRole(dbRole.name) || (dbRole == DB.Role.All)) {
                        // User in role or all roles allowed.
                        return EVAL_BODY_INCLUDE;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Problem determining if the user was in the role", e);
        }

        return super.doStartTag();
    }
}

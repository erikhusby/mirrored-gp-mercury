package org.broadinstitute.gpinformatics.mercury.presentation.tags.security;

import net.sourceforge.stripes.util.Log;
import org.broadinstitute.gpinformatics.mercury.presentation.security.AuthorizationManager;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.TagSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Comma separated list of roles that this block will allow access.  Using "All" will mean all roles have access.
 * <p/>
 * <p>
 * Below is an example of how it is used. You will need to define your own tld.
 * </p>
 * <br/>
 * <code>
 * <%@ taglib uri="http://www.broadinstitute.org/Mercury/AuthorizeBlock"" prefix="security"%>
 * <security:authorizeBlock roles={"Administrator", "Project Manager"} exclusionRoles={"Foobar"}>
 * Secured content goes in here
 * </security:authorizeBlock>
 * </code>
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class AuthorizeBlockStripesTag extends TagSupport {
    private static Log log = Log.getInstance(AuthorizeBlockStripesTag.class);

    private static final long serialVersionUID = 201300107L;

    private List<String> roles;

    private List<String> exclusionRoles;

    public static final String ALLOW_ALL_ROLES = "All";

    public AuthorizeBlockStripesTag() {
        super();
        initValues();
    }

    public String[] getRoles() {
        return (String[]) roles.toArray();
    }

    public void setRoles(String[] roles) {
        if (roles != null) {
            this.roles = Arrays.asList(roles);
        }
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
     */
    @Override
    public int doStartTag() throws JspException {
        if (roles == null || roles.isEmpty())
            return SKIP_BODY;

        try {
            HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
            String pageUri = request.getServletPath();

            // exclusion tag check to skip the jsp code block
            if (!exclusionRoles.isEmpty()) {
                for (String role : roles) {
                    if (request.isUserInRole(role) || role.equals(ALLOW_ALL_ROLES)) {
                        // use has a role that should be skipped or "All" roles to be excluded (?)
                        return SKIP_BODY;
                    }
                }
            }

            // now check the roles to include the jsp content
            if (!roles.isEmpty()) {
                for (String role : roles) {
                    if (request.isUserInRole(role) || role.equals(ALLOW_ALL_ROLES)) {
                        // user in role or all roles allowed
                        return EVAL_BODY_INCLUDE;
                    }
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

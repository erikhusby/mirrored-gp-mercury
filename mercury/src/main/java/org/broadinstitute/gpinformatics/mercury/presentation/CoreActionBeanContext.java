/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2012 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support whatsoever. Neither
 * the Broad Institute nor MIT can be responsible for its use, misuse, or functionality.
 */
package org.broadinstitute.gpinformatics.mercury.presentation;

import net.sourceforge.stripes.action.ActionBeanContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

/*
 * This class is a core class to extend Stripes actions from, providing some methods for
 * Mercury to use.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class CoreActionBeanContext extends ActionBeanContext {
    /**
     * Short hand method to fetch the session.
     *
     * @return HttpSession
     */
    public HttpSession getSession() {
        HttpServletRequest request = getRequest();
        return request.getSession(true);
    }

    /**
     * Remove the HTTP session.
     */
    public void invalidateSession() {
        getSession().invalidate();
    }

    /**
     * Get the JAAS username.
     *
     * @return the JAAS username
     */
    public String getUsername() {
        HttpServletRequest request = getRequest();
        return request.getRemoteUser();
    }
}


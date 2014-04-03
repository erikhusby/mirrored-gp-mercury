package org.broadinstitute.gpinformatics.athena.presentation.projects;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.RedirectResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

/**
 * This class is for redirecting from old JSL page for research projects to the new Stripes page.  This can be
 * removed once we update the LAB Jira database has the URL replaced with the new Stripes URL.
 */
@UrlBinding("/projects/view.xhtml")
public class ResearchProjectRedirectActionBean extends ResearchProjectActionBean {
    @DefaultHandler
    public Resolution handleRedirect() {
        return new RedirectResolution(ResearchProjectActionBean.class, "view").addParameter("researchProject", getResearchProject());
    }
}
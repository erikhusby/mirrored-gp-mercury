package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.infrastructure.quote.Quote;

/**
 * A portfolio is a container for projects.
 * Any project can go into one or more portfolios.
 *
 * Project managers use portfolios to manage
 * different "kinds" of projects.  For
 * instance, there might be a "Cancer"
 * portfolio, a "microbial" portfolio,
 * etc.
 *
 * It's a place for project manager to hang
 * useful unstructured data.  Hence the
 * reference to a jira ticket.
 */
public interface ProjectPortfolio {

    public String getPortfolioName();

    public JiraTicket getPortfolioTicket();

    /**
     * Add the given quote to this project portfolio
     * so that people working on projects in the
     * portfolio can access this list as a convenient,
     * shorter list of possibly quotes to apply
     * to a project.
     * @param q
     */
    public void addQuote(Quote q);

    public void addProject(Project p);

    public Iterable<Project> getProjects();
}

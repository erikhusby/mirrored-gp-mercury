/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.jira;

/**
 * Objects that implement this can make use of objects that update/create jira issues.
 * @see org.broadinstitute.gpinformatics.athena.boundary.orders.UpdateField
 */
public interface JiraProject {
    public String getJiraTicketKey();
}

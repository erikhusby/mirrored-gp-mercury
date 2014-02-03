/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

public class NoJiraTransitionException extends RuntimeException {
    public NoJiraTransitionException(String s) {
        super(s);
    }

    public NoJiraTransitionException(String transitionName, String key) {
        this("Cannot " + transitionName + " " + key + ": no " + transitionName + " transition found");
    }
}

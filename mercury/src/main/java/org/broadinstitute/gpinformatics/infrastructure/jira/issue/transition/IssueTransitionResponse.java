package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 10/10/12
 *         Time: 11:26 PM
 */
public class IssueTransitionResponse {

    private String expand;

    private List<Transition> transitions;


    public IssueTransitionResponse ( String expandIn, List<Transition> transitionsIn ) {
        expand = expandIn;
        transitions = transitionsIn;
    }


    public String getExpand ( ) {
        return expand;
    }

    public List<Transition> getTransitions ( ) {
        return transitions;
    }
}

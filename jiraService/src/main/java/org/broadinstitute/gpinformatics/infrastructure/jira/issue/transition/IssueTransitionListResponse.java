package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.List;

/**
 * @author Scott Matthews
 *         Date: 10/10/12
 *         Time: 11:26 PM
 */
public class IssueTransitionListResponse {

    private String expand;

    private List<Transition> transitions;


    public IssueTransitionListResponse() {
    }


    public IssueTransitionListResponse(String expandIn, List<Transition> transitionsIn) {
        expand = expandIn;
        transitions = transitionsIn;
    }


    public Transition getTransitionByName(String transitionName) {

        for (Transition currentTransition : transitions) {
            if (transitionName.equals(currentTransition.getName())) {
                return currentTransition;
            }
        }

        return null;
    }

    public String getExpand() {
        return expand;
    }

    public void setExpand(String expand) {
        this.expand = expand;
    }

    public List<Transition> getTransitions() {
        return transitions;
    }

    public void setTransitions(List<Transition> transitions) {
        this.transitions = transitions;
    }
}

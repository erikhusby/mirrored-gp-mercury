package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 10/11/12
 *         Time: 10:10 AM
 */
public class IssueTransitionRequest {


    private Map<String, TransitionFields> fields;

    private Transition transition;

    public IssueTransitionRequest ( Transition transitionIn ) {
        transition = transitionIn;
    }

    public IssueTransitionRequest ( Map<String, TransitionFields> fieldsIn, Transition transitionIn ) {
        fields = fieldsIn;
        transition = transitionIn;
    }

    public Map<String, TransitionFields> getFields ( ) {
        return fields;
    }

    public Transition getTransition ( ) {
        return transition;
    }
}

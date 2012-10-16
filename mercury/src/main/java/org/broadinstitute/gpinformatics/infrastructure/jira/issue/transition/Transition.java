package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

import java.util.Map;

/**
 * @author Scott Matthews
 *         Date: 10/10/12
 *         Time: 11:26 PM
 */
public class Transition {

    private String id;

    private String name;

    private NextTransition to;

    private Map<String, TransitionFields> fields;

    public Transition ( String idIn ) {
        id = idIn;
    }

    public Transition ( String idIn, String nameIn, NextTransition toIn ) {
        id = idIn;
        name = nameIn;
        to = toIn;
    }

    public Transition ( String idIn, String nameIn, NextTransition toIn, Map<String, TransitionFields> fieldsIn ) {
        id = idIn;
        name = nameIn;
        to = toIn;
        fields = fieldsIn;
    }

    public String getId ( ) {
        return id;
    }

    public String getName ( ) {
        return name;
    }

    public NextTransition getTo ( ) {
        return to;
    }

    public Map<String, TransitionFields> getFields ( ) {
        return fields;
    }
}

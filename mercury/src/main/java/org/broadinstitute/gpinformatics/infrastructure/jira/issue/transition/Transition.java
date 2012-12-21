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

    /**
     * needed for deserialization
     */
    public Transition() {
    }

    public Transition (String idIn, String nameIn, NextTransition toIn) {
        id = idIn;
        name = nameIn;
        to = toIn;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public NextTransition getTo() {
        return to;
    }

    public void setTo(NextTransition to) {
        this.to = to;
    }

    public Map<String, TransitionFields> getFields() {
        return fields;
    }

    public void setFields(Map<String, TransitionFields> fields) {
        this.fields = fields;
    }
}

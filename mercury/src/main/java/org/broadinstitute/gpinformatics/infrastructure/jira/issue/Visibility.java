package org.broadinstitute.gpinformatics.infrastructure.jira.issue;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.broadinstitute.gpinformatics.infrastructure.jira.JsonLabopsJiraVisibilitySerializer;

import java.io.Serializable;


@JsonSerialize(using = JsonLabopsJiraVisibilitySerializer.class)
public class Visibility implements Serializable {


    public enum Type {
        role
    }

    public enum Value {

        Administrators("Administrators"),QA_Jira_Users("qa-jira-users");

        private final String name;

        private Value(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    private Type type;

    private Value value;

    public Visibility() {}

    public Visibility(Type type, Value value) {
        this.type = type;
        this.value = value;
    }


    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }
    
    
    public static Visibility create(Type type, Value value) {
        return new Visibility(type, value);
    }

    @Override
    public String toString() {
        return "Visibility{" +
                "type=" + type +
                ", value=" + value +
                '}';
    }
}

package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import java.util.Optional;

public interface Transition {

    State getFromState();

    void setFromState(State fromState);

    State getToState();

    void setToState(State toState);

    Optional<Task> getTask();

    void setTask(Task task);

    String getName();

    void setName(String name);
}

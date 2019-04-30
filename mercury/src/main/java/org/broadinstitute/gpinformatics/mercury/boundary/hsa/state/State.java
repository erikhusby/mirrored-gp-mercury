package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import java.util.Optional;

public interface State {

    public String getName();

    public void setName(String name);

    public Optional<Task> getOnExit();

    public void setOnExit(Task task);

    public Optional<Task> getOnEnter();

    public void setOnEnter(Task task);

    public boolean isStart();

    public void setStart(boolean isStart);
}

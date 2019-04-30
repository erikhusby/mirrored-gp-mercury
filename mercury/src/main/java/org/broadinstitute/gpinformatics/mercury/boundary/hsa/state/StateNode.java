package org.broadinstitute.gpinformatics.mercury.boundary.hsa.state;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.Optional;

public class StateNode implements State {
    private String name;
    private Task onExit;
    private Task onEnter;
    private boolean start;

    public StateNode(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public Optional<Task> getOnExit() {
        return Optional.ofNullable(onExit);
    }

    @Override
    public void setOnExit(Task exitTask) {
        this.onExit = exitTask;
    }

    public Optional<Task> getOnEnter() {
        return Optional.ofNullable(onEnter);
    }

    public void setOnEnter(Task enterTask) {
        this.onEnter = enterTask;
    }

    public boolean isStart() {
        return start;
    }

    public void setStart(boolean start) {
        this.start = start;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("name", name)
                .append("start", start)
                .append("onExit", onExit)
                .append("onEnter", onEnter)
                .toString();
    }
}

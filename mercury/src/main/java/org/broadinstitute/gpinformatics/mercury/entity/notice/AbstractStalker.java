package org.broadinstitute.gpinformatics.mercury.entity.notice;

public abstract class AbstractStalker implements Stalker {

    @Override
    public void disable() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void enable() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isMuted() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

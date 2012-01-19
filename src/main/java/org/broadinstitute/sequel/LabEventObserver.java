package org.broadinstitute.sequel;

/**
 * Implementations for doing workflow
 * validation, realtime progress updates
 * to project managers, lab staff, etc.
 */
public interface LabEventObserver {

    public void observeEvent(LabEvent labEvent);
}

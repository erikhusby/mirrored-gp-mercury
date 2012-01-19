package org.broadinstitute.sequel;


import java.util.Collection;
import java.util.Date;

/**
 * Basic source/destination lab vessel
 * access is implemented here.  It's up to
 * subclasses to implement molecular state
 * changes.
 *
 * This class primarily performs the magic between
 * optimized (compressed plate section storage,
 * as it comes in from automation or UI)
 * persistent storage format and the more
 * useful representation in terms
 * of src/dest mapping.
 */
public abstract class AbstractLabEvent implements LabEvent {

    @Override
    public Collection<LabVessel> getTargetLabVessels() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getSourceLabVessels() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getSourcesForTarget(LabVessel targetVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getTargetsForSource(LabVessel sourceVessl) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getAllLabVessels() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getEventLocation() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Person getEventOperator() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Date getEventDate() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Reagent> getReagents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addTargetLabVessel(LabVessel targetVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addSourceLabVessel(LabVessel sourceVessel) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addReagent(Reagent reagent) {
        throw new RuntimeException("I haven't been written yet.");
    }
}

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
    private String eventLocation;
    private Person eventOperator;
    private Date eventDate;

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
        return eventLocation;
    }

    @Override
    public Person getEventOperator() {
        return eventOperator;
    }

    @Override
    public Date getEventDate() {
        return eventDate;
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

    public void setEventLocation(String eventLocation) {
        this.eventLocation = eventLocation;
    }

    public void setEventOperator(Person eventOperator) {
        this.eventOperator = eventOperator;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }
}

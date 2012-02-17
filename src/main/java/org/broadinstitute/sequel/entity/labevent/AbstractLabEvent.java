package org.broadinstitute.sequel.entity.labevent;


import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.AbstractLabVessel;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

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
    private Set<LabVessel> sourceLabVessels = new HashSet<LabVessel>();
    private Set<LabVessel> targetLabVessels = new HashSet<LabVessel>();
    private Set<Reagent> reagents = new HashSet<Reagent>();

    @Override
    public Collection<LabVessel> getTargetLabVessels() {
        return targetLabVessels;
    }

    @Override
    public Collection<LabVessel> getSourceLabVessels() {
        return sourceLabVessels;
    }

    @Override
    public Collection<LabVessel> getSourcesForTarget(LabVessel targetVessel) {
        // todo jmt need some kind of mapping for cherry picks
        return sourceLabVessels;
    }

    @Override
    public Collection<LabVessel> getTargetsForSource(LabVessel sourceVessl) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getAllLabVessels() {
        Set<LabVessel> allLabVessels = new HashSet<LabVessel>();
        allLabVessels.addAll(sourceLabVessels);
        allLabVessels.addAll(targetLabVessels);
        return allLabVessels;
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
        return reagents;
    }

    @Override
    public void addTargetLabVessel(LabVessel targetVessel) {
        if(sourceLabVessels.isEmpty()) {
            // todo jmt method for adding source / target pairs
            throw new RuntimeException("Add source lab vessels first");
        }
        if(targetVessel.getTransfersTo().isEmpty()) {
            for (LabVessel sourceLabVessel : sourceLabVessels) {
                if (((AbstractLabVessel) sourceLabVessel).getSampleSheetReferences().isEmpty()) {
                    ((AbstractLabVessel)targetVessel).getSampleSheetReferences().add(sourceLabVessel);
                } else {
                    ((AbstractLabVessel)targetVessel).getSampleSheetReferences().addAll(
                            ((AbstractLabVessel) sourceLabVessel).getSampleSheetReferences());
                }
            }
        }
        targetLabVessels.add(targetVessel);
    }

    @Override
    public void addSourceLabVessel(LabVessel sourceVessel) {
        sourceLabVessels.add(sourceVessel);
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

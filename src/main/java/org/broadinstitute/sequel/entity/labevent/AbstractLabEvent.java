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
    /** for transfers using a tip box, e.g. Bravo */
    private Set<SectionTransfer> sectionTransfers = new HashSet<SectionTransfer>();
    /** for random access transfers, e.g. MultiProbe */
    private Set<CherryPickTransfer> cherryPickTransfers = new HashSet<CherryPickTransfer>();
    private Set<VesselToSectionTransfer> vesselToSectionTransfers = new HashSet<VesselToSectionTransfer>();
    // todo jmt tube to tube transfers, or will they always be in a rack?

    private String quoteServerBatchId;
    
    
    @Override
    public Collection<LabVessel> getTargetLabVessels() {
        return this.targetLabVessels;
    }

    @Override
    public Collection<LabVessel> getSourceLabVessels() {
        return this.sourceLabVessels;
    }

    @Override
    public Collection<LabVessel> getSourcesForTarget(LabVessel targetVessel) {
        // todo jmt need some kind of mapping for cherry picks
        return this.sourceLabVessels;
    }

    @Override
    public Collection<LabVessel> getTargetsForSource(LabVessel sourceVessl) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<LabVessel> getAllLabVessels() {
        Set<LabVessel> allLabVessels = new HashSet<LabVessel>();
        allLabVessels.addAll(this.sourceLabVessels);
        allLabVessels.addAll(this.targetLabVessels);
        return allLabVessels;
    }

    @Override
    public String getEventLocation() {
        return this.eventLocation;
    }

    @Override
    public Person getEventOperator() {
        return this.eventOperator;
    }

    @Override
    public Date getEventDate() {
        return this.eventDate;
    }

    @Override
    public Collection<Reagent> getReagents() {
        return this.reagents;
    }

    @Override
    public void addTargetLabVessel(LabVessel targetVessel) {
        // todo jmt move to SectionTransfer?
        if(this.sourceLabVessels.isEmpty()) {
            // todo jmt method for adding source / target pairs
            throw new RuntimeException("Add source lab vessels first");
        }
/*
        if(targetVessel.getTransfersTo().isEmpty()) {
            for (LabVessel sourceLabVessel : this.sourceLabVessels) {
                if (((AbstractLabVessel) sourceLabVessel).getSampleSheetAuthorities().isEmpty()) {
                    if(sourceLabVessel.getReagentContents().isEmpty()) {
                        ((AbstractLabVessel)targetVessel).getSampleSheetAuthorities().add(sourceLabVessel);
                    }
                } else {
                    ((AbstractLabVessel)targetVessel).getSampleSheetAuthorities().addAll(
                            ((AbstractLabVessel) sourceLabVessel).getSampleSheetAuthorities());
                }
            }
        } else {
            // pooling

        }
*/
        targetVessel.getTransfersTo().add(this);
        this.targetLabVessels.add(targetVessel);
    }

    @Override
    public void addSourceLabVessel(LabVessel sourceVessel) {
        this.sourceLabVessels.add(sourceVessel);
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

    @Override
    public Set<SectionTransfer> getSectionTransfers() {
        return this.sectionTransfers;
    }

    public void setSectionTransfers(Set<SectionTransfer> sectionTransfers) {
        this.sectionTransfers = sectionTransfers;
    }

    @Override
    public Set<CherryPickTransfer> getCherryPickTransfers() {
        return cherryPickTransfers;
    }

    public void setCherryPickTransfers(Set<CherryPickTransfer> cherryPickTransfers) {
        this.cherryPickTransfers = cherryPickTransfers;
    }

    @Override
    public Set<VesselToSectionTransfer> getVesselToSectionTransfers() {
        return vesselToSectionTransfers;
    }

    public void setVesselToSectionTransfers(Set<VesselToSectionTransfer> vesselToSectionTransfers) {
        this.vesselToSectionTransfers = vesselToSectionTransfers;
    }

    @Override
    public String getQuoteServerBatchId() {
        return quoteServerBatchId;
    }

    @Override
    public void setQuoteServerBatchId(String batchId) {
        this.quoteServerBatchId = batchId;
    }
}

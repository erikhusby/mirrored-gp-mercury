package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.person.Person;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.SampleSheet;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Date;

/**
 * Most general form of lab event
 */
@Entity
public class GenericLabEvent extends LabEvent {

    @Enumerated(EnumType.STRING)
    private LabEventType labEventType;

    public GenericLabEvent(LabEventType labEventType, Date eventDate, String eventLocation, Person operator) {
        this.labEventType = labEventType;
        this.setEventDate(eventDate);
        this.setEventLocation(eventLocation);
        this.setEventOperator(operator);
    }

    protected GenericLabEvent() {
    }

    @Override
    public LabEventName getEventName() {
        return LabEventName.GENERIC;
    }

    /**
     * Are we going to change the molecular
     * state?
     *
     * Perhaps this should be up at {@link LabEvent}
     *
     * Events that denature or that transform from
     * RNA into DNA also change molecular state.  So perhaps
     * these
     * @return
     */
    @Transient
    private boolean isMolecularStateBeingChanged() {
        boolean hasMolStateChange = false;
        for (Reagent reagent: getReagents()) {
            if (reagent.getMolecularEnvelopeDelta() != null) {
                hasMolStateChange = true;
            }
        }
        return hasMolStateChange;
    }

    /**
     * After writing this method, I know think we only need
     * a single {@link LabEvent} class to handle most Logic for properly handling
     * {@link org.broadinstitute.sequel.entity.vessel.MolecularState} changes can be written
     * once.  The need to customize behavior of
     * {@link #validateSourceMolecularState()}, {@link #validateTargetMolecularState()},
     * and {@link #applyMolecularStateChanges()}  is
     * pretty unlikely.
     * @throws InvalidMolecularStateException
     */
    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        // apply reagents in message
        for (LabVessel target: getTargetLabVessels()) {
/*
            for (LabVessel source: getSourcesForTarget(target)) {
                // apply all goop from all sources
                for (SampleSheet sampleSheet : source.getSampleSheets()) {
                    target.addSampleSheet(sampleSheet);
                }
            }
*/
            // after the target goop is transferred,
            // apply the reagent
/*
            for (Reagent reagent : getReagents()) {
                target.applyReagent(reagent);
            }
*/
        }

        /**
         * Here is why we probably only need a single {@link #applyMolecularStateChanges()}
         * method.
         */
        for (LabVessel target: getTargetLabVessels()) {
/*
            // todo jmt restore this
            // check the molecular state per target.
            Set<MolecularStateTemplate> molecularStateTemplatesInTarget = new HashSet<MolecularStateTemplate>();
            for (SampleInstance sampleInstance : target.getSampleInstances()) {
                molecularStateTemplatesInTarget.add(sampleInstance.getMolecularState().getMolecularStateTemplate());
            }
            // allowing for jumbled {@link MolecularState} is probably
            // one of those things we'd override per {@link LabEvent}
            // subclass.  In the worst case, an implementation of
            // {@link LabEvent} might have to dip into {@link Project}
            // data to make some sort of special case
            if (molecularStateTemplatesInTarget.size() > 1) {
                StringBuilder errorMessage = new StringBuilder("Molecular state will not be uniform as a result of this operation.  " + target.getLabCentricName() + " has " + molecularStateTemplatesInTarget.size() + " different molecular states:\n");
                for (MolecularStateTemplate stateTemplate : molecularStateTemplatesInTarget) {
                    errorMessage.append(stateTemplate.toText());
                }
                // todo post this error message back to PM jira
                throw new InvalidMolecularStateException(errorMessage.toString());
            }
*/
            // if no molecular envelope change, set backlink

            // create pool, or set backlink
            // how to determine that it's a pooling operation? destination section has samples (is not empty), source section has samples

            // set molecular state from map (or set backlink?)

            // setting backlinks must be section based, unless the section is ALL* (without flips)
        }

        for (SectionTransfer sectionTransfer : getSectionTransfers()) {
            sectionTransfer.applyTransfer();
        }
    }

    /**
     * Probably we'll want generic source/target
     * molecular state checks done up at the
     * abstract superclass level and then let
     * subclasses override them?
     *
     * @throws InvalidMolecularStateException
     */
    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        if (getSourceLabVessels().isEmpty() && !this.labEventType.isExpectedEmptySources()) {
            throw new InvalidMolecularStateException("No sources.");
        }
/*
        for (LabVessel source: getSourceLabVessels()) { // todo jmt remove
            if (!this.labEventType.isExpectedEmptySources()) {
                if (source.getSampleInstances().isEmpty()) {
                    throw new InvalidMolecularStateException("Source " + source.getLabCentricName() + " is empty");
                }
            }
        }
*/
    }

    /**
     * Probably we'll want generic source/target
     * molecular state checks done up at the
     * abstract superclass level and then let
     * subclasses override them?
     * @throws InvalidMolecularStateException
     */
    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        if (getTargetLabVessels().isEmpty()) {
            throw new InvalidMolecularStateException("No destinations!");
        }
/*
        for (LabVessel target: getTargetLabVessels()) {
            if (!this.labEventType.isExpectedEmptyTargets()) {
                if (target.getSampleInstances().isEmpty()) {
                    throw new InvalidMolecularStateException("Target " + target.getLabCentricName() + " is empty");
                }
            }
        }
*/
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }

    public LabEventType getLabEventType() {
        return this.labEventType;
    }
}

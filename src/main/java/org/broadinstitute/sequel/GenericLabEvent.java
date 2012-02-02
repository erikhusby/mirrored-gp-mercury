package org.broadinstitute.sequel;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Most general form of lab event
 */
public class GenericLabEvent extends AbstractLabEvent {
    private final LabEventType labEventType;

    public GenericLabEvent(LabEventType labEventType, Date eventDate, String eventLocation, Person operator) {
        this.labEventType = labEventType;
        this.setEventDate(eventDate);
        this.setEventLocation(eventLocation);
        this.setEventOperator(operator);
    }

    @Override
    public LabEventName getEventName() {
        return LabEventName.GENERIC;
    }

    @Override
    public boolean isBillable() {
        return false;
    }

    /**
     * Are we going to change the molecular
     * state?
     *
     * Perhaps this should be up at {@link AbstractLabEvent}
     *
     * Events that denature or that transform from
     * RNA into DNA also change molecular state.  So perhaps
     * these
     * @return
     */
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
     * {@link MolecularState} changes can be written
     * once.  The need to customize behavior of
     * {@link #validateSourceMolecularState()}, {@link #validateTargetMolecularState()},
     * and {@link #applyMolecularStateChanges()}  is
     * pretty unlikely.
     * @throws InvalidMolecularStateException
     */
    @Override
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        for (LabVessel target: getTargetLabVessels()) {
            for (LabVessel source: getSourcesForTarget(target)) {
                // apply all goop from all sources
                target.getGoop().applyGoop(source.getGoop());
            }
            // after the target goop is transferred,
            // apply the reagent
            for (Reagent reagent : getReagents()) {
                target.getGoop().applyReagent(reagent);
            }
        }

        /**
         * Here is why we probably only need a single {@link #applyMolecularStateChanges()}
         * method.
         */
        for (LabVessel target: getTargetLabVessels()) {
            // check the molecular state per target.
            Set<MolecularStateTemplate> molecularStateTemplatesInTarget = new HashSet<MolecularStateTemplate>();
            for (SampleSheet sampleSheet: target.getGoop().getSampleSheets()) {
                for (SampleInstance sampleInstance : sampleSheet.getSamples()) {
                    molecularStateTemplatesInTarget.add(sampleInstance.getMolecularState().getMolecularStateTemplate());
                }
            }
            // allowing for jumbled {@link MolecularState} is probably
            // one of those things we'd override per {@link LabEvent}
            // subclass.  In the worst case, an implementation of
            // {@link LabEvent} might have to dip into {@link Project}
            // data to make some sort of special case
            if (molecularStateTemplatesInTarget.size() > 1) {
                StringBuilder errorMessage = new StringBuilder("Molecular state will not be uniform as a result of this operation.  " + target.getGoop().getLabCentricName() + " has " + molecularStateTemplatesInTarget.size() + " different molecular states:\n");
                for (MolecularStateTemplate stateTemplate : molecularStateTemplatesInTarget) {
                    errorMessage.append(stateTemplate.toText());
                }
                // todo post this error message back to PM jira
                throw new InvalidMolecularStateException(errorMessage.toString());
            }
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
        if (getSourceLabVessels().isEmpty()) {
            throw new InvalidMolecularStateException("No sources.");
        }
        for (LabVessel source: getSourceLabVessels()) {
            if (!labEventType.isExpectedEmptySources()) {
                for (SampleSheet sampleSheet : source.getGoop().getSampleSheets()) {
                    if (sampleSheet.getSamples().isEmpty()) {
                        throw new InvalidMolecularStateException("Source " + source.getGoop().getLabCentricName() + " is empty");
                    }
                }
            }
        }
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
        for (LabVessel target: getTargetLabVessels()) {
            if (!labEventType.isExpectedEmptyTargets()) {
                for (SampleSheet sampleSheet : target.getGoop().getSampleSheets()) {
                    if (sampleSheet.getSamples().isEmpty()) {
                        throw new InvalidMolecularStateException("Target " + target.getGoop().getLabCentricName() + " is empty");
                    }
                }
            }
        }
    }

    @Override
    public Collection<SampleSheet> getAllSampleSheets() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

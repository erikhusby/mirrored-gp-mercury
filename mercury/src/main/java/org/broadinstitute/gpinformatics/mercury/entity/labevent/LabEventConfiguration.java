package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.MolecularStateRange;

/**
 * Our automation instruments are very
 * componentized.  By this I mean that
 * each discrete step in the lab process
 * expected certain characteristics of
 * the input.  
 * 
 * If we know the expected input {@link #getExpectedMolecularState()},
 * then we can give people a heads up when it looks like
 * the wrong kind of operation might
 * be happening.  This will help us
 * prevent the accidental destruction
 * of a sample.
 * 
 * For most of our events--at least in the automation
 * world--we like to know whether the event
 * results in the creation of some
 * new material.  That's what {@link #getOutputMode()}  tells
 * us.
 * 
 * For most events, we expect the "target" vessel
 * (aka "destination") to be empty.  But sometimes,
 * we may have pre-dispensed reagent into it,
 * or maybe we're doing a multi-step pooling,
 * in which case we expect that the destination
 * should already have some information associated
 * with it in the lims.  This is what {@link #targetShouldBeEmpty()}
 * tells us.
 * 
 * When it comes to recouping costs, we tend to mark
 * specific operations as billable events.  This
 * is what {@link #isBillable()}  tells us.
 *
 * If we know all this information about every
 * lab event and treat it all as configuration
 * information, then we should be able to
 * add a lot of events to lims with minimal
 * code.
 */
public class LabEventConfiguration {

    public MolecularStateRange getExpectedMolecularState() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public enum OutputMaterialMode {
        NEW_LIBRARY,SAME_LIBRARY
    }

    public boolean isBillable() {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Do the target vessels contain the same
     * library or do we create a new library?
     * See Containable for further discussion.
     * @return
     */
    public OutputMaterialMode getOutputMode() {
        throw new RuntimeException("Method not yet implemented.");
    }

    /**
     * Do we expect the target of this event to
     * be empty?  Put another way, do we expect
     * destination plates to be empty?
     * @return
     */
    public boolean targetShouldBeEmpty() {
        throw new RuntimeException("Method not yet implemented.");
    }

}

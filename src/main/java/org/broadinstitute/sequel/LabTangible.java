package org.broadinstitute.sequel;

import java.util.Collection;

/**
 * Some tangible thing, or just enough
 * information about said thing, that
 * the lab can take this and go do
 * some lab work.
 *
 * This might seem like an over-generalization,
 * but when you see all the lists of "things"
 * that our users keep (libraries, aliquots,
 * plates, tubes, flowcells, lanes, runs,
 * etc.), you realize that being able
 * to just group "things" is a very
 * useful feature.  A LabTangible
 * is our basic "thing".
 */
public interface LabTangible {

    /**
     * Get the name of the thing.  This
     * isn't just getName() because that would
     * probably clash with something else.
     * @return
     */
    public String getLabCentricName();

    /**
     * May return null if no sample sheet
     * has been registered.  Consider
     * a "new" destination in a transfer event.
     * When first encountered, the plate
     * may have no sample sheet, and it's up
     * to the message processor to fill
     * in the sample sheet.
     * @return
     */
    public Collection<SampleSheet> getSampleSheets();
    
    public Collection<StateChange> getStateChanges();

    /**
     * We want GSP to let you walk in with a tube, declare
     * the contents of the tube, and then inject the tube
     * into the middle of the lab process, into
     * whatever LabWorkQueue you need.
     *
     * This method is the key to this feature.  We do not
     * care <b>how</b> the sample metadata was built
     * for a container, but we care deeply that
     * we have consistent metadata.  Whether it was
     * built half at 320, half at the collaborator,
     * all at 320, all in Mozambique, it does not
     * matter.  Just declare the sample metadata
     * in one place.
     *
     * @param sampleSheet
     */
    public void addSampleSheet(SampleSheet sampleSheet);
    
    public void addStateChange(StateChange stateChange);
    
    public MolecularState buildMolecularState();


}

package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

/**
 * A template for how different reagents
 * go into stock plates.  Basically
 * a mapping between well names
 * and reagents.  Lab users will have a UI
 * where they can say "I have a new pile
 * of molecular index plates that match
 * index plate template X".
 */
public interface ReagentPlateTemplate {

    public String getTemplateName();

    public VesselPosition getWell();

    public Reagent getReagent();
}

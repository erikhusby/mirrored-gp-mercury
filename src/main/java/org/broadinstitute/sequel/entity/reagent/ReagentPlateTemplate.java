package org.broadinstitute.sequel.entity.reagent;

import org.broadinstitute.sequel.entity.vessel.WellName;

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

    public WellName getWell();

    public Reagent getReagent();
}

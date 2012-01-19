package org.broadinstitute.sequel;

public interface ReagentRegistrar {

    /**
     * Lets users say "Take this stack of plates and
     * apply template XYZ, which describes
     * a well-to-molecular-index mapping"
     *
     * The reagent template describes what reagent
     * is in each well, for example.  When the
     * lab receives a plate, they can tell us
     * the reagent layout to apply to the plate.
     * @param reagentContainer
     * @param template
     */
    public void applyTemplate(LabVessel reagentContainer, ReagentPlateTemplate template);
}

package org.broadinstitute.gpinformatics.mercury.entity.queue;


public class AliquotParameters {

//    private ProjectPlan projectPlan;

    private float volume;

    private float concentration;

    public AliquotParameters(/*ProjectPlan projectPlan,*/float volume,float concentration) {
//        if (projectPlan == null) {
//             throw new IllegalArgumentException("project must be non-null in AliquotParameters.AliquotParameters");
//        }
//        this.projectPlan = projectPlan;
        this.volume = volume;
        this.concentration = concentration;
    }
    
    public Float getTargetVolume() {
        return volume;
    }

    public Float getTargetConcentration() {
        return concentration;
    }

    public boolean handoffWholeTube() {
        throw new RuntimeException("Method not yet implemented.");
    }

//    public ProjectPlan getProjectPlan() {
//        return projectPlan;
//    }

}

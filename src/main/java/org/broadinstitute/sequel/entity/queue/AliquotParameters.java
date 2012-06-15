package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.project.BasicProjectPlan;

public class AliquotParameters implements LabWorkQueueParameters {

    private BasicProjectPlan projectPlan;

    private float volume;

    private float concentration;

    public AliquotParameters(BasicProjectPlan projectPlan,float volume,float concentration) {
        if (projectPlan == null) {
             throw new IllegalArgumentException("project must be non-null in AliquotParameters.AliquotParameters");
        }
        this.projectPlan = projectPlan;
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

    public BasicProjectPlan getProjectPlan() {
        return projectPlan;
    }
    

    @Override
    public String toText() {
        return "Volume: " + getTargetVolume(); // etc.
    }
    
}

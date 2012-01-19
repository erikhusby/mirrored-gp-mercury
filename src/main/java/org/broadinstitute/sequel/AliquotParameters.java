package org.broadinstitute.sequel;


public class AliquotParameters implements LabWorkQueueParameters {

    private Project project;

    private float volume;

    private float concentration;

    public AliquotParameters(Project project,float volume,float concentration) {
        if (project == null) {
             throw new IllegalArgumentException("project must be non-null in AliquotParameters.AliquotParameters");
        }
        this.project = project;
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

    public Project getProject() {
        return project;
    }
    

    @Override
    public String toText() {
        return "Volume: " + getTargetVolume(); // etc.
    }
    
}

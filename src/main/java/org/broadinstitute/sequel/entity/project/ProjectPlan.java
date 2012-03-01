package org.broadinstitute.sequel.entity.project;

import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashSet;

public class ProjectPlan {

    private Collection<LabVessel> starters = new HashSet<LabVessel>();
    
    private Collection<ProjectPlanDetail> planDetails = new HashSet<ProjectPlanDetail>();
    
    private Project project;
    
    private String planName;
    
    private String notes;
    
    public ProjectPlan(Project project,String name)  {
        if (project == null) {
             throw new NullPointerException("project cannot be null."); 
        }
        if (name == null) {
             throw new NullPointerException("name cannot be null."); 
        }
        this.project = project;
        this.planName = name;
    }
    
    public void addStarters(LabVessel vessel) {
        if (vessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        starters.add(vessel);
    }

    public void addPlanDetail(ProjectPlanDetail detail) {
        if (detail == null) {
             throw new NullPointerException("detail cannot be null.");
        }
        planDetails.add(detail);
    }

    public Collection<ProjectPlanDetail> getPlanDetails() {
        return planDetails;
    }

    /**
     * What's the name of this plan?
     * @return
     */
    public String getName() {
        return planName;
    }
    
    public String getNotes() {
        return notes;
    }

    /**
     * Basically how much sequencing are we going
     * to do for this project?  We assume that the
     * sequencing goal is uniform for every sample
     * in the project.
     * @return
     */

    // todo way to abstract categories for weekly prioritization/capacity
    // planning from critter.

    // organism, collaborator, initiative/funding source/quote, prep
    // type, sequencing type, outbreak,

}

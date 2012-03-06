package org.broadinstitute.sequel.entity.project;

import org.apache.commons.collections.map.HashedMap;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ProjectPlan {

    private Collection<LabVessel> starters = new HashSet<LabVessel>();
    
    private Collection<SequencingPlanDetail> planDetails = new HashSet<SequencingPlanDetail>();
    
    private Project project;
    
    private String planName;
    
    private String notes;

    // todo where does analysis type go here?
    
    private Collection<PoolGroup> poolGroups = new HashSet<PoolGroup>();

    private Collection<ReagentDesign> reagentDesigns = new HashSet<ReagentDesign>();
    
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

    public Project getProject() {
        return project;
    }
    
    public Collection<ReagentDesign> getReagentDesigns() {
        return reagentDesigns;
    }
    
    public void addReagentDesign(ReagentDesign design) {
        reagentDesigns.add(design);
    }
    
    public void addPoolGroup(PoolGroup poolGroup) {
        if (poolGroup == null) {
             throw new NullPointerException("poolGroup cannot be null."); 
        }
        poolGroups.add(poolGroup);
    }

    public Collection<PoolGroup> getPoolGroups() {
        return poolGroups;
    }
    
    public void addStarter(LabVessel vessel) {
        if (vessel == null) {
            throw new NullPointerException("vessel cannot be null.");
        }
        project.addStarter(vessel);
        starters.add(vessel);
    }
    
    public Collection<LabVessel> getStarters() {
        return starters;
    }

    public void addSequencingDetail(SequencingPlanDetail detail) {
        if (detail == null) {
             throw new NullPointerException("detail cannot be null.");
        }
        planDetails.add(detail);
    }

    public Collection<SequencingPlanDetail> getPlanDetails() {
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

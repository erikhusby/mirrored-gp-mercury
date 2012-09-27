package org.broadinstitute.gpinformatics.mercury.entity.project;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import java.util.Collection;
import java.util.HashSet;

/**
 * A group of {@link LabVessel}s that 
 * can be pooled together.  Subsets can be pooled together,
 * but two {@link LabVessel}s in cannot be pooled
 * together unless they're in the same
 * {@link PoolGroup}.
 */
public class PoolGroup {
    
    private String poolName;
    
    private Collection<LabVessel> potentialPoolMates = new HashSet<LabVessel>();
    
    public PoolGroup(String name) {
        if (name == null) {
             throw new NullPointerException("name cannot be null."); 
        }
        poolName = name;
    }

    /**
     * Is it okay to add #labVessel to this
     * pool?
     * @param labVessel
     * @return
     */
    public boolean canAddToPool(LabVessel labVessel) {
        return potentialPoolMates.contains(labVessel);
    }

    public String getPoolName() {
        return poolName;
    }
    
    public void addPotentialPoolMate(LabVessel potentialPartner) {
        potentialPoolMates.add(potentialPartner);
    }
    
    public Collection<LabVessel> getPotentialPoolMates() {
        return potentialPoolMates;
    }

}

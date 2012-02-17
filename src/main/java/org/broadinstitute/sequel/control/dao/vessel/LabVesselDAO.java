package org.broadinstitute.sequel.control.dao.vessel;

import org.broadinstitute.sequel.entity.vessel.LabVessel;

public interface LabVesselDAO {
    
    public LabVessel findByLabel(String label);
}

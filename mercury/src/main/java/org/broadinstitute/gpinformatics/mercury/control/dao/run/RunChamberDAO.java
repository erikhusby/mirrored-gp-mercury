package org.broadinstitute.gpinformatics.mercury.control.dao.run;


import org.broadinstitute.gpinformatics.mercury.entity.run.RunChamber;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;

/**
 * Data access object for run/chamber
 */
@Stateful
@RequestScoped
public class RunChamberDAO {
    
    public RunChamber findByRunNameAndChamber(String runName,String chamber) {
        throw new RuntimeException("not implemented yet");
    }
}

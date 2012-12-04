package org.broadinstitute.gpinformatics.mercury.entity.run;

import java.util.Collection;

public interface SequenceAccessControlModel {
    
    public String getIRB();
    
    public Collection<Long> getUsersWhoCanReadTheData();
}

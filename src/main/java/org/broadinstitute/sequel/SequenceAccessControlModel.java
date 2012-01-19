package org.broadinstitute.sequel;

import java.util.Collection;

public interface SequenceAccessControlModel {
    
    public String getIRB();
    
    public Collection<Person> getUsersWhoCanReadTheData();
}

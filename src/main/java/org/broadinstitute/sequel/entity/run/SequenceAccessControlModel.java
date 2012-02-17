package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.person.Person;

import java.util.Collection;

public interface SequenceAccessControlModel {
    
    public String getIRB();
    
    public Collection<Person> getUsersWhoCanReadTheData();
}

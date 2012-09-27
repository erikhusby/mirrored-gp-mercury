package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.entity.person.Person;

import java.util.Collection;

public interface SequenceAccessControlModel {
    
    public String getIRB();
    
    public Collection<Person> getUsersWhoCanReadTheData();
}

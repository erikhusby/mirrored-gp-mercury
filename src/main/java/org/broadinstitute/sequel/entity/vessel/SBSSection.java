package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.vessel.WellName;

import java.util.Collection;

public interface SBSSection {

    public String getSectionName();

    public Collection<WellName> getWells();
}

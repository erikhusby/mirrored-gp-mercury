package org.broadinstitute.sequel;

import java.util.Collection;

public interface SBSSection {

    public String getSectionName();

    public Collection<WellName> getWells();
}

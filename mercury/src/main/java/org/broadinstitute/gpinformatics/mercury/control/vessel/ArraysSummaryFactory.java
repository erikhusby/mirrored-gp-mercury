package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.PrintStream;
import java.util.List;

/**
 * Generates Summary.txt file for arrays.  Used by PMs to review Array projects, and sometimes delivered to
 * collaborators.
 */
public class ArraysSummaryFactory {
    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs,
            ResearchProject researchProject) {
        // Preamble
        // Header Group
        // Headers
        
    }
}

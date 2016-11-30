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
        printStream.println("PED Data\t\tCall Rate\tSample\t\t\t\t\t\tFingerprint\tGender\t\t\t\tTrio\t" +
                "BEADSTUDIO\t\t\t\t\t\tZCALL\t\tScan\t\t\t\t\tPlate\t\t\t\n");
        // Headers
        // Family ID

        // Individual ID
        // BEADSTUDIO
        // Aliquot
        // Root Sample
        // Stock Sample
        // Participant
        // Collaborator Sample
        // Collaborator Participant
        // Called Infinium SNPs
        // Reported Gender
        // Fldm FP Gender
        // Beadstudio Gender
        // Algorithm Gender Concordance
        // Family
        // Call Rate
        // Het %
        // Hap Map Concordance
        // Version
        // Last Cluster File
        // Run
        // Version
        // Chip
        // Scan Date
        // Amp Date
        // Scanner
        // Genotyping Run
        // DNA Plate
        // DNA Plate Well
    }
}

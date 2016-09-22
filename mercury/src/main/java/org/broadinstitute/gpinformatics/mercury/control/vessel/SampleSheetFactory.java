package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.io.PrintStream;
import java.util.List;

/**
 * Create an arrays sample sheet, for Genome Studio.
 */
public class SampleSheetFactory {
    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs) {
        printStream.println("[Header]");
        printStream.print("Investigator Name,");
        printStream.println();
        printStream.print("Project Name,");
        printStream.println();
        printStream.println("Experiment Name");
        printStream.print("Date,");
        printStream.println();

        printStream.println();
        printStream.println("**NOTE**");
        printStream.println("\"The following columns are required: Sample_ID, SentrixBarcode_A, SentrixPosition_A " +
                "(and _B, _C etc.. if you have multiple manifests)\"");
        printStream.println("All other columns are optional");
        printStream.println("Column order doesn't matter");
        printStream.println();

        printStream.println("[Manifests]");
        printStream.print("A,");
        printStream.println();
        printStream.println("[Data]");
        printStream.println("Sample_ID,SentrixBarcode_A,SentrixPosition_A,Sample_Plate,Sample_Well,Sample_Group," +
                "Gender,Sample_Name,Replicate,Parent1,Parent2,CallRate");
        for (Pair<LabVessel, VesselPosition> vesselPositionPair : vesselPositionPairs) {
            printStream.print(masterPlateName);
            printStream.print("_");
            printStream.print(masterPlatePosition);
            printStream.print("_");
            printStream.print(participantId);
            printStream.print("_");
            printStream.print(vesselPositionPair.getLeft().getLabel());
            printStream.print("_");
            printStream.print(vesselPositionPair.getRight());
            printStream.print(",");

            printStream.print(vesselPositionPair.getLeft().getLabel());
            printStream.print(",");

            printStream.print(vesselPositionPair.getRight());
            printStream.print(",");

            printStream.print(chemistryPlateName);
            printStream.print(",");

            printStream.print(chemistryPlatePosition);
            printStream.print(",");

            printStream.print(collabPtId);
            printStream.print(",");

            printStream.print(gender);
            printStream.print(",");

            printStream.print(participantId);
            // todo jmt parent1, parent2, call rate
            printStream.println(",,,");
        }
    }
}

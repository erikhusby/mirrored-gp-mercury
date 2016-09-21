package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.jboss.marshalling.Pair;

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
    }
}

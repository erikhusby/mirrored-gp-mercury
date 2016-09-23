package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Create an arrays sample sheet, for Genome Studio.
 */
public class SampleSheetFactory {

    private static final Set<LabEventType> LAB_EVENT_TYPES = new HashSet<LabEventType>() {{
        add(LabEventType.INFINIUM_AMPLIFICATION);
        add(LabEventType.INFINIUM_HYBRIDIZATION);
    }};

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs,
            MessageCollection messageCollection) {
        // Write header
        printStream.println("[Header]");
        printStream.print("Investigator Name,");
        printStream.println();
        printStream.print("Project Name,");
        printStream.println();
        printStream.println("Experiment Name");
        printStream.print("Date,");
        printStream.println();

        // Write manifests
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

        // Write data columns
        printStream.println("[Data]");
        printStream.println("Sample_ID,SentrixBarcode_A,SentrixPosition_A,Sample_Plate,Sample_Well,Sample_Group," +
                "Gender,Sample_Name,Replicate,Parent1,Parent2,CallRate");

        // Get samples
        ArrayList<String> sampleNames = new ArrayList<>();
        Map<Pair<LabVessel, VesselPosition>, SampleInstanceV2> mapPairToSampleInstance = new HashMap<>();
        for (Pair<LabVessel, VesselPosition> vesselPositionPair : vesselPositionPairs) {
            Set<SampleInstanceV2> sampleInstances = vesselPositionPair.getLeft().getContainerRole().
                    getSampleInstancesAtPositionV2(vesselPositionPair.getRight());
            if (sampleInstances.size() == 1) {
                SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                sampleNames.add(sampleInstance.getNearestMercurySampleName());
                mapPairToSampleInstance.put(vesselPositionPair, sampleInstance);
            } else {
                messageCollection.addError("Expecting 1 sample at " + vesselPositionPair.getLeft().getLabel() + "_" +
                vesselPositionPair.getRight() + ", found " + sampleInstances.size());
            }
        }
        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleData(sampleNames);

        // Write a row per chip well
        for (Pair<LabVessel, VesselPosition> vesselPositionPair : vesselPositionPairs) {
            LabVessel labVessel = vesselPositionPair.getLeft();
            VesselPosition vesselPosition = vesselPositionPair.getRight();
            TransferTraverserCriteria.VesselPositionForEvent traverserCriteria =
                    new TransferTraverserCriteria.VesselPositionForEvent(LAB_EVENT_TYPES);
            labVessel.getContainerRole().evaluateCriteria(vesselPosition, traverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, 0);

            VesselAndPosition dnaPlateAndPosition = traverserCriteria.getMapTypeToVesselPosition().get(
                    LabEventType.INFINIUM_AMPLIFICATION);
            printStream.print(dnaPlateAndPosition.getVessel().getName());
            printStream.print("_");
            printStream.print(dnaPlateAndPosition.getPosition());
            printStream.print("_");
            SampleData sampleData = mapSampleNameToData.get(mapPairToSampleInstance.get(
                    vesselPositionPair).getNearestMercurySampleName());
            printStream.print(sampleData.getPatientId());
            printStream.print("_");
            printStream.print(labVessel.getLabel());
            printStream.print("_");
            printStream.print(vesselPosition);
            printStream.print(",");

            printStream.print(labVessel.getLabel());
            printStream.print(",");

            printStream.print(vesselPosition);
            printStream.print(",");

            VesselAndPosition chemistryPlateAndPosition = traverserCriteria.getMapTypeToVesselPosition().get(
                    LabEventType.INFINIUM_AMPLIFICATION);
            printStream.print(chemistryPlateAndPosition.getVessel().getLabel());
            printStream.print(",");

            printStream.print(chemistryPlateAndPosition.getPosition());
            printStream.print(",");

            printStream.print(sampleData.getCollaboratorParticipantId());
            printStream.print(",");

            printStream.print(sampleData.getGender());
            printStream.print(",");

            printStream.print(sampleData.getPatientId());
            // todo jmt parent1, parent2, call rate
            printStream.println(",,,");
        }
    }
}

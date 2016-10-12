package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.entity.bucket.BucketEntry;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
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

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy");

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private ProductEjb productEjb;

    public List<Pair<LabVessel, VesselPosition>> loadByPdo(ProductOrder productOrder) {
        List<Pair<LabVessel, VesselPosition>> vesselPositionPairs = new ArrayList<>();
        // todo jmt include controls?
        for (BucketEntry bucketEntry : productOrder.getBucketEntries()) {
            TransferTraverserCriteria.VesselPositionForEvent transferTraverserCriteria =
                    new TransferTraverserCriteria.VesselPositionForEvent(
                            Collections.singleton(LabEventType.INFINIUM_HYBRIDIZATION),
                            bucketEntry.getLabBatch());
            bucketEntry.getLabVessel().evaluateCriteria(transferTraverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Descendants);

            VesselAndPosition vesselAndPosition = transferTraverserCriteria.getMapTypeToVesselPosition().
                    get(LabEventType.INFINIUM_HYBRIDIZATION);
            if (vesselAndPosition != null) {
                vesselPositionPairs.add(new ImmutablePair<>(vesselAndPosition.getVessel(),
                        vesselAndPosition.getPosition()));
            }
        }

        return vesselPositionPairs;
    }

    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs,
            ResearchProject researchProject) {
        // Get samples
        ArrayList<String> sampleNames = new ArrayList<>();
        Map<Pair<LabVessel, VesselPosition>, SampleInstanceV2> mapPairToSampleInstance = new HashMap<>();
        boolean errors = false;
        for (Pair<LabVessel, VesselPosition> vesselPositionPair : vesselPositionPairs) {
            Set<SampleInstanceV2> sampleInstances = vesselPositionPair.getLeft().getContainerRole().
                    getSampleInstancesAtPositionV2(vesselPositionPair.getRight());
            if (sampleInstances.size() == 1) {
                SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                sampleNames.add(sampleInstance.getNearestMercurySampleName());
                mapPairToSampleInstance.put(vesselPositionPair, sampleInstance);
            } else {
                printStream.println("Expecting 1 sample at " + vesselPositionPair.getLeft().getLabel() + "_" +
                        vesselPositionPair.getRight() + ", found " + sampleInstances.size());
                errors = true;
            }
        }
        if (errors) {
            return;
        }

        // Write header
        printStream.println("[Header]");

        printStream.print("Investigator Name,");
        String piName = "";
        Long[] broadPIs = researchProject.getBroadPIs();
        if (broadPIs != null && broadPIs.length > 0) {
            piName = bspUserList.getById(broadPIs[0]).getFullName();
        }
        printStream.println(piName);

        printStream.print("Project Name,");
        printStream.println(researchProject.getName());

        printStream.println("Experiment Name");

        printStream.print("Date,");
        printStream.println(DATE_FORMAT.format(researchProject.getCreatedDate()));

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
        LabVessel labVessel1 = vesselPositionPairs.get(0).getLeft();
        String chipType = productEjb.getGenotypingChip(researchProject.getProductOrders().get(0),
                labVessel1.getEvents().iterator().next().getEventDate()).getRight();
        printStream.println(chipType);

        // Write data columns
        printStream.println("[Data]");
        printStream.println("Sample_ID,SentrixBarcode_A,SentrixPosition_A,Sample_Plate,Sample_Well,Sample_Group," +
                "Gender,Sample_Name,Replicate,Parent1,Parent2,CallRate");

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
            printStream.print(sampleData.getCollaboratorParticipantId());
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
                    LabEventType.INFINIUM_HYBRIDIZATION);
            printStream.print(chemistryPlateAndPosition.getVessel().getLabel());
            printStream.print(",");

            printStream.print(chemistryPlateAndPosition.getPosition());
            printStream.print(",");

            printStream.print(sampleData.getCollaboratorParticipantId());
            printStream.print(",");

            if (!StringUtils.isBlank(sampleData.getGender())) {
                printStream.print(sampleData.getGender().charAt(0));
            }
            printStream.print(",");

            printStream.print(sampleData.getPatientId());
            // todo jmt parent1, parent2, call rate?
            printStream.println(",,,");
        }
    }
}

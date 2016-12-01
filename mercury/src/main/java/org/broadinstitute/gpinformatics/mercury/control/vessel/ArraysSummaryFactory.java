package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.inject.Inject;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Summary.txt file for arrays.  Used by PMs to review Array projects, and sometimes delivered to
 * collaborators.
 */
public class ArraysSummaryFactory {

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ArraysQcDao arraysQcDao;

    @Inject
    private ProductEjb productEjb;

    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs,
            ResearchProject researchProject) {

        // Get samples
        List<MercurySample> sampleNames = new ArrayList<>();
        List<String> chipWellBarcodes = new ArrayList<>();
        Map<Pair<LabVessel, VesselPosition>, SampleInstanceV2> mapPairToSampleInstance = new HashMap<>();
        boolean errors = false;
        for (Pair<LabVessel, VesselPosition> vesselPositionPair : vesselPositionPairs) {
            Set<SampleInstanceV2> sampleInstances = vesselPositionPair.getLeft().getContainerRole().
                    getSampleInstancesAtPositionV2(vesselPositionPair.getRight());
            String chipWellBarcode = vesselPositionPair.getLeft().getLabel() + "_" + vesselPositionPair.getRight();
            chipWellBarcodes.add(chipWellBarcode);
            if (sampleInstances.size() == 1) {
                SampleInstanceV2 sampleInstance = sampleInstances.iterator().next();
                sampleNames.add(sampleInstance.getNearestMercurySample());
                mapPairToSampleInstance.put(vesselPositionPair, sampleInstance);
            } else {
                printStream.println("Expecting 1 sample at " + chipWellBarcode + ", found " + sampleInstances.size());
                errors = true;
            }
        }
        if (errors) {
            return;
        }
        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleDataForSamples(sampleNames,
                BSPSampleSearchColumn.PARTICIPANT_ID, BSPSampleSearchColumn.COLLABORATOR_FAMILY_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                BSPSampleSearchColumn.STOCK_SAMPLE);
        Map<String, ArraysQc> mapBarcodeToArrayQc = arraysQcDao.findMapByBarcodes(chipWellBarcodes);
        LabVessel labVessel1 = vesselPositionPairs.get(0).getLeft();
        String chipType = productEjb.getGenotypingChip(researchProject.getProductOrders().get(0),
                labVessel1.getEvents().iterator().next().getEventDate()).getRight();

        // Preamble
        // Header Group
        printStream.println("PED Data\t\tCall Rate\tSample\t\t\t\t\t\tFingerprint\tGender\t\t\t\tTrio\t" +
                "BEADSTUDIO\t\t\t\t\tZCALL\t\tScan\t\t\t\t\tPlate\t\t\t");
        // Headers
        printStream.println("Family ID\tIndividual ID\tBEADSTUDIO\tAliquot\tRoot Sample\tStock Sample\tParticipant\t" +
                "Collaborator Sample\tCollaborator Participant\tCalled Infinium SNPs\tReported Gender\tFldm FP Gender\t" +
                "Beadstudio Gender\tAlgorithm Gender Concordance\tFamily\tCall Rate\tHet %\tHap Map Concordance\t" +
                "Version\tLast Cluster File\tRun\tVersion\tChip\tScan Date\tAmp Date\tScanner\tGenotyping Run\t" +
                "DNA Plate\tDNA Plate Well");
        for (int i = 0; i < vesselPositionPairs.size(); i++) {
            Pair<LabVessel, VesselPosition> vesselPositionPair = vesselPositionPairs.get(i);
            LabVessel labVessel = vesselPositionPair.getLeft();
            VesselPosition vesselPosition = vesselPositionPair.getRight();
            SampleInstanceV2 sampleInstanceV2 = mapPairToSampleInstance.get(vesselPositionPair);
            SampleData sampleData = mapSampleNameToData.get(sampleInstanceV2.getNearestMercurySampleName());
            ArraysQc arraysQc = mapBarcodeToArrayQc.get(chipWellBarcodes.get(i));
            if (arraysQc == null) {
                arraysQc = new ArraysQc();
            }
            TransferTraverserCriteria.VesselPositionForEvent traverserCriteria =
                    new TransferTraverserCriteria.VesselPositionForEvent(SampleSheetFactory.LAB_EVENT_TYPES);
            labVessel.getContainerRole().evaluateCriteria(vesselPosition, traverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, 0);

            VesselAndPosition dnaPlateAndPosition = traverserCriteria.getMapTypeToVesselPosition().get(
                    LabEventType.INFINIUM_AMPLIFICATION);

            // Family ID
            printStream.print(sampleData.getCollaboratorFamilyId() + "\t");
            // Individual ID
            printStream.print(sampleData.getPatientId() + "\t");
            // BEADSTUDIO
            printStream.print(arraysQc.getCallRatePct() + "\t");
            // Aliquot
            printStream.print(sampleInstanceV2.getNearestMercurySampleName() + "\t");
            // Root Sample
            printStream.print(sampleInstanceV2.getMercuryRootSampleName() + "\t");
            // Stock Sample
            printStream.print(sampleData.getStockSample() + "\t");
            // Participant
            printStream.print(sampleData.getPatientId() + "\t");
            // Collaborator Sample
            printStream.print(sampleData.getCollaboratorsSampleName() + "\t");
            // Collaborator Participant
            printStream.print(sampleData.getCollaboratorParticipantId() + "\t");
            // Called Infinium SNPs
            printStream.print(arraysQc.getTotalSnps() + "\t");
            // Reported Gender
            printStream.print(arraysQc.getReportedGender() + "\t");
            // Fldm FP Gender
            printStream.print(arraysQc.getFpGender() + "\t");
            // Beadstudio Gender
            printStream.print(arraysQc.getAutocallGender() + "\t");
            // Algorithm Gender Concordance
            printStream.print(arraysQc.getGenderConcordancePf() + "\t");
            // Family
            printStream.print(sampleData.getCollaboratorFamilyId() + "\t");
            // Call Rate
            printStream.print(arraysQc.getCallRatePct() + "\t");
            // Het %
            printStream.print(arraysQc.getHetPct100() + "\t");
            // Hap Map Concordance
            if (arraysQc.getArraysQcGtConcordances() != null) {
                for (ArraysQcGtConcordance arraysQcGtConcordance: arraysQc.getArraysQcGtConcordances()) {
                    if (arraysQcGtConcordance.getVariantType().equals("SNP")) {
                        printStream.print(ColumnValueType.TWO_PLACE_DECIMAL.format(
                                arraysQcGtConcordance.getGenotypeConcordance().multiply(BigDecimal.valueOf(100)), ""));
                    }
                }
            }
            printStream.print("\t");
            // Version
            printStream.print(arraysQc.getAutocallVersion() + "\t");
            // Last Cluster File
            printStream.print(arraysQc.getClusterFileName() + "\t");
            // Run
            printStream.print(arraysQc.getZcalled() + "\t");
            // Version
            printStream.print(arraysQc.getZcallVersion() + "\t");
            // Chip
            printStream.print(chipType + "\t");
            // todo jmt fix next 3
            // Scan Date
            // Amp Date
            // Scanner
            // Genotyping Run
            printStream.print(chipWellBarcodes.get(i) + "\t");
            // DNA Plate
            printStream.print(dnaPlateAndPosition.getVessel().getName() + "\t");
            // DNA Plate Well
            printStream.print(dnaPlateAndPosition.getPosition() + "\t");
            printStream.println();
        }
    }
}

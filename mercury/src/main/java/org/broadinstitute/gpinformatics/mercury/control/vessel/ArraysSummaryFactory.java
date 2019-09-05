package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.athena.boundary.products.ProductEjb;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.analytics.ArraysQcDao;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQc;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcBlacklisting;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcFingerprint;
import org.broadinstitute.gpinformatics.infrastructure.analytics.entity.ArraysQcGtConcordance;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.columns.ColumnValueType;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.InfiniumStarterConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.run.InfiniumRunProcessor;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.AbandonVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TransferTraverserCriteria;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselAndPosition;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.text.Format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates Summary.txt file for arrays.  Used by PMs to review Array projects, and sometimes delivered to
 * collaborators.
 */
@Dependent
public class ArraysSummaryFactory {

    private static final Format DATE_TIME_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy hh:mm a");
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("MM/dd/yyyy");

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ArraysQcDao arraysQcDao;

    @Inject
    private ProductEjb productEjb;

    @Inject
    private InfiniumStarterConfig infiniumStarterConfig;

    public void write(PrintStream printStream, List<Pair<LabVessel, VesselPosition>> vesselPositionPairs,
            ProductOrder productOrder, boolean includeScannerName) {

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
            }
        }

        Map<String, SampleData> mapSampleNameToData = sampleDataFetcher.fetchSampleDataForSamples(sampleNames,
                BSPSampleSearchColumn.PARTICIPANT_ID, BSPSampleSearchColumn.COLLABORATOR_FAMILY_ID,
                BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
                BSPSampleSearchColumn.STOCK_SAMPLE, BSPSampleSearchColumn.COLLECTION,
                BSPSampleSearchColumn.BSP_COLLECTION_BARCODE, BSPSampleSearchColumn.PRIMARY_DISEASE);
        Map<String, ArraysQc> mapBarcodeToArrayQc = arraysQcDao.findMapByBarcodes(chipWellBarcodes);
        MultiValuedMap<String, ArraysQcBlacklisting> mapBarcodeToArrayQcBlacklist
                = arraysQcDao.findBlacklistMapByBarcodes(chipWellBarcodes);
        LabVessel labVessel1 = vesselPositionPairs.get(0).getLeft();
        String chipType = productOrder==null?"":productEjb.getGenotypingChip(productOrder,
                labVessel1.getEvents().iterator().next().getEventDate()).getRight();

        // Header Group
        printStream.println("PED Data\t\t\t\t\tCall Rate\t\t\tSample\t\t\t\t\t\tFingerprint\t\tGender\t\t\t\tTrio\t\t\t" +
                "BEADSTUDIO\t\t\t\t\tZCALL\t\tScan\t\t\tPlate\t\tFailures\t\t\t\t");
        // Headers
        printStream.println("Family ID\tIndividual ID\tCollection\tCollection ID\tPrimary Disease" +
                "\tAutoCall\tzCall\tCall Date\tAliquot\tRoot Sample\tStock Sample\tParticipant\t" +
                "Collaborator Sample\tCollaborator Participant\tCalled Infinium SNPs\tLOD\tReported Gender\tFldm FP Gender\t" +
                "Beadstudio Gender\tAlgorithm Gender Concordance\tFamily\tHet %\tHap Map Concordance\t" +
                "Version\tLast Cluster File\tRun\tVersion\tChip\tScan Date\tAmp Date\tScanner\tChip Well Barcode\t" +
                "Analysis Version\tDNA Plate\tDNA Plate Well\tLab Abandon\tBlacklisted On\tBlacklist Reason\tWhitelisted On\t");
        for (int i = 0; i < vesselPositionPairs.size(); i++) {
            Pair<LabVessel, VesselPosition> vesselPositionPair = vesselPositionPairs.get(i);
            LabVessel chip = vesselPositionPair.getLeft();
            VesselPosition vesselPosition = vesselPositionPair.getRight();
            SampleInstanceV2 sampleInstanceV2 = mapPairToSampleInstance.get(vesselPositionPair);
            if( sampleInstanceV2 == null ) {
                continue;
            }
            SampleData sampleData = mapSampleNameToData.get(sampleInstanceV2.getNearestMercurySampleName());
            boolean foundArraysQc = true;
            ArraysQc arraysQc = mapBarcodeToArrayQc.get(chipWellBarcodes.get(i));
            Collection<ArraysQcBlacklisting> arraysQcBlacklistings = mapBarcodeToArrayQcBlacklist.get(chipWellBarcodes.get(i));
            if (arraysQc == null) {
                arraysQc = new ArraysQc();
                foundArraysQc = false;
            }
            TransferTraverserCriteria.VesselPositionForEvent traverserCriteria =
                    new TransferTraverserCriteria.VesselPositionForEvent(SampleSheetFactory.LAB_EVENT_TYPES);
            chip.getContainerRole().evaluateCriteria(vesselPosition, traverserCriteria,
                    TransferTraverserCriteria.TraversalDirection.Ancestors, 0);

            VesselAndPosition dnaPlateAndPosition = traverserCriteria.getMapTypeToVesselPosition().get(
                    LabEventType.INFINIUM_AMPLIFICATION);

            // Family ID
            printStream.print(sampleData.getCollaboratorFamilyId() + "\t");
            // Individual ID
            printStream.print(sampleData.getPatientId() + "\t");
            // Collection
            printStream.print(sampleData.getCollection() + "\t");
            // Collection ID
            printStream.print(sampleData.getCollectionId() + "\t");
            // Primary Disease
            printStream.print(sampleData.getPrimaryDisease() + "\t");
            // AutoCall
            BigDecimal autocallCallRatePct = arraysQc.getAutocallCallRatePct();
            printStream.print((autocallCallRatePct == null ? "" : autocallCallRatePct) + "\t");
            // zCall
            printStream.print(arraysQc.getCallRatePct() + "\t");
            // Call Date
            if (arraysQc.getAutocallDate() != null) {
                printStream.print(DATE_FORMAT.format(arraysQc.getAutocallDate()));
            }
            printStream.print("\t");
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
            // LOD
            if (arraysQc.getArraysQcFingerprints() != null && !arraysQc.getArraysQcFingerprints().isEmpty()) {
                ArraysQcFingerprint arraysQcFingerprint = arraysQc.getArraysQcFingerprints().iterator().next();
                printStream.print(MathUtils.scaleThreeDecimalPlaces(arraysQcFingerprint.getLodExpectedSample()));
            }
            printStream.print("\t");
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
            // Het %
            printStream.print(arraysQc.getHetPct100() + "\t");
            // Hap Map Concordance
            if (arraysQc.getArraysQcGtConcordances() != null) {
                for (ArraysQcGtConcordance arraysQcGtConcordance: arraysQc.getArraysQcGtConcordances()) {
                    if (arraysQcGtConcordance.getVariantType().equals("SNP")) {
                        printStream.print(ColumnValueType.THREE_PLACE_DECIMAL.format(
                                arraysQcGtConcordance.getGenotypeConcordance().multiply(BigDecimal.valueOf(100L)), ""));
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
            // Scan Date
            for (LabEvent labEvent: chip.getInPlaceLabEvents()) {
                if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_SOME_STARTED) {
                    printStream.print(DATE_TIME_FORMAT.format(labEvent.getEventDate()));
                    break;
                }
            }
            printStream.print("\t");
            // Amp Date
            for (LabEvent labEvent : dnaPlateAndPosition.getVessel().getTransfersFrom()) {
                if (labEvent.getLabEventType() == LabEventType.INFINIUM_AMPLIFICATION) {
                    printStream.print(DATE_TIME_FORMAT.format(labEvent.getEventDate()));
                    break;
                }
            }
            printStream.print("\t");
            // Scanner
            if (includeScannerName) {
                String scannerName = null;
                for (LabEvent labEvent : chip.getInPlaceLabEvents()) {
                    if (labEvent.getLabEventType() == LabEventType.INFINIUM_AUTOCALL_SOME_STARTED) {
                        scannerName = labEvent.getEventLocation();
                        break;
                    }
                }

                if (scannerName == null) {
                    scannerName = InfiniumRunProcessor.findScannerName(chip.getLabel(), infiniumStarterConfig);
                }
                printStream.print(scannerName == null ? "" : scannerName);
            }
            printStream.print("\t");
            // Chip Well Barcode
            printStream.print(chipWellBarcodes.get(i) + "\t");
            // Analysis Version
            printStream.print(arraysQc.getAnalysisVersion() + "\t");
            // DNA Plate
            printStream.print(dnaPlateAndPosition.getVessel().getName() + "\t");
            // DNA Plate Well
            printStream.print(dnaPlateAndPosition.getPosition() + "\t");

            // Lab abandon vessel reason and date
            printStream.print( getAbandon(chip, vesselPosition) + "\t" );

            // Pipeline blacklist date, reason, whitelist date
            if( arraysQcBlacklistings == null || arraysQcBlacklistings.isEmpty() ) {
                printStream.print(" \t");
                printStream.print(" \t");
                printStream.print(" \t");
            } else {
                String blon = "", reas = "", wlon = "";
                boolean isFirst = true;
                for( ArraysQcBlacklisting blacklist : arraysQcBlacklistings ) {
                    if (blacklist.getAnalysisVersion().equals(arraysQc.getAnalysisVersion())) {
                        blon += (isFirst?"":", ") + DATE_FORMAT.format(blacklist.getBlacklistedOn());
                        reas += (isFirst?"":", ") + blacklist.getBlacklistReason();
                        // Retain only the latest
                        wlon = blacklist.getWhitelistedOn() == null?"":DATE_FORMAT.format(blacklist.getWhitelistedOn());
                        isFirst = false;
                    }
                }
                printStream.print(blon + "\t");
                printStream.print(reas + "\t");
                printStream.print(wlon + "\t");
            }
            printStream.println();
        }
    }

    /**
     * Gets output formatted lab abandon vessel reason and date (only the most recent in ancestor transfers <br/>
     * Format as "Abandon reason(abandon simple date)" e.g. Equipment failure(10/21/2017)
     */
    private String getAbandon(LabVessel chip, VesselPosition well){
        TransferTraverserCriteria.AbandonedLabVesselCriteria abandonCriteria = new TransferTraverserCriteria.AbandonedLabVesselCriteria();
        chip.getContainerRole().evaluateCriteria(well, abandonCriteria, TransferTraverserCriteria.TraversalDirection.Ancestors,0);
        if( abandonCriteria.isAncestorAbandoned() ) {
            MultiValuedMap<LabVessel,AbandonVessel> vesselMultiValuedMap = abandonCriteria.getAncestorAbandonVessels();
            AbandonVessel abandonVessel = vesselMultiValuedMap.values().iterator().next();
            return abandonVessel.getReason().getDisplayName() + "(" + DATE_FORMAT.format( abandonVessel.getAbandonedOn() ) + ")";
        } else {
            return "";
        }
    }
}

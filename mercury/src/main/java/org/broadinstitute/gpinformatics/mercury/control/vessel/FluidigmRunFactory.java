package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.SnpListDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.ControlDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.control.run.ConcordanceCalculator;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.run.Snp;
import org.broadinstitute.gpinformatics.mercury.entity.run.SnpList;
import org.broadinstitute.gpinformatics.mercury.entity.sample.Control;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.PlateWell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Upload a Fingerprinting Run and its associated metrics/metadata
 */
@Stateful
@RequestScoped
public class FluidigmRunFactory {

    @Inject
    private StaticPlateDao staticPlateDao;

    @Inject
    private LabMetricRunDao labMetricRunDao;

    @Inject
    private SnpDao snpDao;

    @Inject
    private SnpListDao snpListDao;

    @Inject
    private ControlDao controlDao;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    public Pair<StaticPlate, LabMetricRun> createFluidigmChipRun(InputStream inputStream, Long decidingUser, MessageCollection messageCollection) {
        FluidigmChipProcessor fluidigmChipProcessor = new FluidigmChipProcessor();
        FluidigmChipProcessor.FluidigmRun fluidigmRun = fluidigmChipProcessor.parse(inputStream);
        messageCollection.addAll(fluidigmChipProcessor.getMessageCollection());

        if (messageCollection.hasErrors()) {
            return null;
        }
        // Fetch Chip
        String chipBarcode = fluidigmRun.getChipBarcode();
        String formattedBarcode = StringUtils.leftPad(chipBarcode, 12, '0');
        StaticPlate staticPlate = staticPlateDao.findByBarcode(formattedBarcode);
        if (staticPlate == null) {
            messageCollection.addError("Failed to find chip " + formattedBarcode);
            return null;
        }

        SnpList snpList = snpListDao.findByName(fluidigmRun.getReagentPlateName());
        if (snpList == null) {
            messageCollection.addError("Failed to find Snp List with name " + fluidigmRun.getReagentPlateName());
            return null;
        }

        StaticPlate.TubeFormationByWellCriteria.Result traverserResult =
                staticPlate.nearestFormationAndTubePositionByWell();

        if (traverserResult.getTubeFormation() == null) {
            messageCollection.addError("Cannot find source tubes for plate " + staticPlate.getLabel());
            return null;
        }

        LabMetricRun labMetricRun = labMetricRunDao.findByName(fluidigmRun.buildRunName());
        if (labMetricRun != null) {
            messageCollection.addError("This run has been uploaded previously.");
            return Pair.of(staticPlate, labMetricRun);
        }

        // Run date must be unique so that a search can reveal the latest quant.
        List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(fluidigmRun.getRunDate());
        if (CollectionUtils.isNotEmpty(sameDateRuns)) {
            messageCollection.addError("A previous upload has the same Run Started timestamp.");
            return Pair.of(staticPlate, sameDateRuns.iterator().next());
        }

        Set<String> polyAssays = fluidigmRun.getPolyAssays();
        Map<String, Snp> mapAssayToSnp = snpDao.findByRsIds(polyAssays);
        for (Map.Entry<String, Snp> entry: mapAssayToSnp.entrySet()) {
            if (entry.getValue() == null) {
                messageCollection.addError("Failed to find assay " + entry.getKey() + " in database.");
                return null;
            }
        }

        Set<MercurySample> mercurySamples = new HashSet<>();
        for (SampleInstanceV2 sampleInstanceV2 : staticPlate.getContainerRole().getSampleInstancesV2()) {
            mercurySamples.add(sampleInstanceV2.getRootOrEarliestMercurySample());
        }
        sampleDataFetcher.fetchSampleDataForSamples(mercurySamples, BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID);

        LabMetricRun run = createFluidigmRunDaoFree(fluidigmRun, staticPlate, decidingUser,
                traverserResult, mapAssayToSnp, snpList, controlDao.findAllActive());

        if (!messageCollection.hasErrors()) {
            labMetricRunDao.persist(run);
        }
        return Pair.of(staticPlate, run);
    }

    @DaoFree
    public LabMetricRun createFluidigmRunDaoFree(FluidigmChipProcessor.FluidigmRun fluidigmRun,
                                                 StaticPlate ifcChip, Long decidingUser,
                                                 StaticPlate.TubeFormationByWellCriteria.Result traverserResult,
                                                 Map<String, Snp> mapAssayToSnp,
                                                 SnpList snpList,
                                                 List<Control> controls) {
        LabMetricRun labMetricRun = new LabMetricRun(fluidigmRun.buildRunName(), fluidigmRun.getRunDate(),
                LabMetric.MetricType.FLUIDIGM_FINGERPRINTING);
        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                fluidigmRun.getInstrumentName()));

        Map<VesselPosition, VesselPosition> tubePositionToWell = new HashMap<>();
        for (Map.Entry<VesselPosition, VesselPosition> entry : traverserResult.getWellToTubePosition().entrySet()) {
            tubePositionToWell.put(entry.getValue(), entry.getKey());
        }

        Map<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> mapLabVesselToRecords = new HashMap<>();
        Map<String, PlateWell> mapSampleNameToPlateWell = new HashMap<>();
        for (FluidigmChipProcessor.FluidigmDataRow record: fluidigmRun.getRecords()) {
            String chamberId = record.getId();
            String sampleName = chamberId.split("-")[0]; // S01
            int sampleNum = Integer.parseInt(sampleName.substring(1));
            String well = convertIntToWellPosition(sampleNum, 12);
            VesselPosition tubeVesselPosition = VesselPosition.getByName(well);
            if (!tubePositionToWell.containsKey(tubeVesselPosition)) {
                continue;
            }
            VesselPosition vesselPosition = tubePositionToWell.get(tubeVesselPosition);
            PlateWell plateWell = ifcChip.getContainerRole().getVesselAtPosition(vesselPosition);
            if (plateWell == null) {
                plateWell = new PlateWell(ifcChip, vesselPosition);
                ifcChip.getContainerRole().addContainedVessel(plateWell, vesselPosition);
            }
            if (!mapLabVesselToRecords.containsKey(plateWell)) {
                mapLabVesselToRecords.put(plateWell, new ArrayList<>());
            }
            mapSampleNameToPlateWell.put(sampleName, plateWell);
            mapLabVesselToRecords.get(plateWell).add(record);
        }

        double q17Threshold = 98.0;
        double q20Threshold = 99.0;
        generateCallRateLabMetrics(labMetricRun, LabMetric.MetricType.CALL_RATE_Q17, decidingUser, mapAssayToSnp,
                mapLabVesselToRecords, q17Threshold);

        Map<PlateWell, LabMetricDecision> mapPlateWellToDecision = generateCallRateLabMetrics(
                labMetricRun, LabMetric.MetricType.CALL_RATE_Q20, decidingUser, mapAssayToSnp, mapLabVesselToRecords, q20Threshold);

        // ROX Data
        for (Map.Entry<String, DescriptiveStatistics>  entry: fluidigmRun.getMapSampleToRawStatistics().entrySet()) {
            String sampleKey = entry.getKey();
            DescriptiveStatistics descriptiveStatistics = entry.getValue();
            PlateWell plateWell = mapSampleNameToPlateWell.get(sampleKey);
            if (plateWell != null && descriptiveStatistics.getN() > 0) {
                BigDecimal sum = new BigDecimal(descriptiveStatistics.getSum());
                LabMetric sumMetric = new LabMetric(sum, LabMetric.MetricType.ROX_SAMPLE_RAW_DATA_COUNT,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(sumMetric);
                plateWell.addMetric(sumMetric);

                BigDecimal mean = new BigDecimal(descriptiveStatistics.getMean());
                LabMetric meanMetric = new LabMetric(mean, LabMetric.MetricType.ROX_SAMPLE_RAW_DATA_MEAN,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(meanMetric);
                plateWell.addMetric(meanMetric);

                BigDecimal median = new BigDecimal(descriptiveStatistics.getPercentile(50));
                LabMetric medianMetric = new LabMetric(median, LabMetric.MetricType.ROX_SAMPLE_RAW_DATA_MEDIAN,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(medianMetric);
                plateWell.addMetric(medianMetric);

                BigDecimal stdDev = new BigDecimal(descriptiveStatistics.getStandardDeviation());
                LabMetric stdDevMetric = new LabMetric(stdDev, LabMetric.MetricType.ROX_SAMPLE_RAW_DATA_STD_DEV,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(stdDevMetric);
                plateWell.addMetric(stdDevMetric);
            }
        }

        for (Map.Entry<String, DescriptiveStatistics>  entry: fluidigmRun.getMapSampleToBackgroundStatistics().entrySet()) {
            String sampleKey = entry.getKey();
            DescriptiveStatistics descriptiveStatistics = entry.getValue();
            PlateWell plateWell = mapSampleNameToPlateWell.get(sampleKey);
            if (plateWell != null && descriptiveStatistics.getN() > 0) {
                BigDecimal sum = new BigDecimal(descriptiveStatistics.getSum());
                LabMetric sumMetric = new LabMetric(sum, LabMetric.MetricType.ROX_SAMPLE_BKGD_DATA_COUNT,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(sumMetric);
                plateWell.addMetric(sumMetric);

                BigDecimal mean = new BigDecimal(descriptiveStatistics.getMean());
                LabMetric meanMetric = new LabMetric(mean, LabMetric.MetricType.ROX_SAMPLE_BKGD_DATA_MEAN,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(meanMetric);
                plateWell.addMetric(meanMetric);

                BigDecimal median = new BigDecimal(descriptiveStatistics.getPercentile(50));
                LabMetric medianMetric = new LabMetric(median, LabMetric.MetricType.ROX_SAMPLE_BKGD_DATA_MEDIAN,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(medianMetric);
                plateWell.addMetric(medianMetric);

                BigDecimal stdDev = new BigDecimal(descriptiveStatistics.getStandardDeviation());
                LabMetric stdDevMetric = new LabMetric(stdDev, LabMetric.MetricType.ROX_SAMPLE_BKGD_DATA_STD_DEV,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date());
                labMetricRun.addMetric(stdDevMetric);
                plateWell.addMetric(stdDevMetric);
            }
        }

        Map<PlateWell, Gender> mapPlateWellToGender =
                generateGenderAndConfidenceMetric(labMetricRun, mapAssayToSnp, mapLabVesselToRecords);

        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
        for (Map.Entry<PlateWell, Gender> entry: mapPlateWellToGender.entrySet()) {
            PlateWell plateWell = entry.getKey();
            MercurySample mercurySample =
                    plateWell.getSampleInstancesV2().iterator().next().getNearestMercurySample();
            LabMetricDecision.Decision decision = mapPlateWellToDecision.get(plateWell).getDecision();

            Fingerprint.Disposition disposition =
                    (decision == LabMetricDecision.Decision.PASS) ? Fingerprint.Disposition.PASS : Fingerprint.Disposition.FAIL;

            Fingerprint.Platform platform = Fingerprint.Platform.FLUIDIGM;
            Fingerprint.GenomeBuild genomeBuild = Fingerprint.GenomeBuild.HG19;
            Fingerprint.Gender gender = Fingerprint.Gender.byAbbreviation(entry.getValue().getSymbol());
            Fingerprint fingerprint = new Fingerprint(mercurySample, disposition, platform, genomeBuild,
                    fluidigmRun.getRunDate(), snpList, gender, null);
            for (FluidigmChipProcessor.FluidigmDataRow fluidigmDataRow : mapLabVesselToRecords.get(plateWell)) {
                Genotype genotype = parseGenotype(fluidigmDataRow);
                fingerprint.addFpGenotype(new FpGenotype(fingerprint, mapAssayToSnp.get(fluidigmDataRow.getAssayName()),
                        (genotype.getAllele1() == null ? "-" : genotype.getAllele1()) +
                                (genotype.getAllele2() == null ? "-" : genotype.getAllele2()),
                        new BigDecimal(fluidigmDataRow.getConfidence())));
            }

            String collaboratorParticipantId = mercurySample.getSampleData().getCollaboratorParticipantId();
            Optional<Control> optionalControl = controls.stream().
                    filter(control -> control.getCollaboratorParticipantId().equals(collaboratorParticipantId)).
                    findFirst();
            if (optionalControl.isPresent()) {
                double lodScore = concordanceCalculator.calculateHapMapConcordance(fingerprint, optionalControl.get());
                // todo jmt common date for all metrics?
                plateWell.addMetric(new LabMetric(new BigDecimal(lodScore), LabMetric.MetricType.HAPMAP_CONCORDANCE_LOD,
                        LabMetric.LabUnit.NUMBER, plateWell.getVesselPosition().name(), new Date()));
            }
        }

        return labMetricRun;
    }

    private void calculateHapMapConcordance(LabMetricRun labMetricRun, Map<String, Snp> mapAssayToSnp,
                                            Map<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> mapLabVesselToRecords) {
        
    }

    /**
     * Creates a Call Rate Metric of type Q17 or Q20 depending on their thresholds. Stores the num passing calls
     * and total calls based on this threshold as metadata so ratio can be shown in UDS.
     */
    private Map<PlateWell, LabMetricDecision> generateCallRateLabMetrics(LabMetricRun run, LabMetric.MetricType metricType, long decidingUser,
                                                                         Map<String, Snp> mapAssayToSnp,
                                                                         Map<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> mapLabVesselToRecords,
                                                                         double threshold) {
        Map<PlateWell, LabMetricDecision> mapPlateWellToDecision = new HashMap<>();
        for (Map.Entry<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> entry: mapLabVesselToRecords.entrySet()) {
            PlateWell plateWell = entry.getKey();
            double calls = 0.0;
            double totalPossibleCalls = 0.0;
            for (FluidigmChipProcessor.FluidigmDataRow row: entry.getValue()) {
                Genotype genotype = parseGenotype(row);
                if (!mapAssayToSnp.get(row.getAssayName()).isFailed() &&
                        !mapAssayToSnp.get(row.getAssayName()).isGender()) {
                    totalPossibleCalls++;
                    boolean userCall = genotype.isUserCall();
                    if (!userCall) {
                        if (genotype.getConfidence() >= threshold &&
                                genotype.getAllele1() != null && genotype.getAllele2() != null) {
                            calls++;
                        }
                    } else {
                        calls++;
                    }
                }
            }

            BigDecimal callRate = new BigDecimal((calls / totalPossibleCalls ) * 100);
            callRate = callRate.setScale(2, BigDecimal.ROUND_HALF_UP);
            LabMetric labMetric = new LabMetric(callRate, metricType, LabMetric.LabUnit.PERCENTAGE,
                    plateWell.getVesselPosition().name(), new Date());
            plateWell.addMetric(labMetric);
            run.addMetric(labMetric);
            LabMetricDecision decision = metricType.getDecider().makeDecision(plateWell, labMetric, decidingUser);
            labMetric.setLabMetricDecision(decision);
            Metadata totalCallsMetadata = new Metadata(Metadata.Key.TOTAL_POSSIBLE_CALLS, new BigDecimal(totalPossibleCalls));
            Metadata callsMetadata = new Metadata(Metadata.Key.CALLS, new BigDecimal(calls));
            labMetric.getMetadataSet().add(totalCallsMetadata);
            labMetric.getMetadataSet().add(callsMetadata);
            mapPlateWellToDecision.put(plateWell, decision);
        }

        return mapPlateWellToDecision;
    }

    private Map<PlateWell, Gender> generateGenderAndConfidenceMetric(LabMetricRun run, Map<String, Snp> mapAssayToSnp,
                                                                     Map<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> mapLabVesselToRecords) {
        Map<PlateWell, Gender> mapPlateWellToGender = new HashMap<>();
        for (Map.Entry<PlateWell, List<FluidigmChipProcessor.FluidigmDataRow>> entry: mapLabVesselToRecords.entrySet()) {
            PlateWell plateWell = entry.getKey();
            for (FluidigmChipProcessor.FluidigmDataRow row : entry.getValue()) {
                Snp polyAssay = mapAssayToSnp.get(row.getAssayName());
                if(polyAssay.isGender()) {
                    Gender gender = null;
                    Genotype genotype = parseGenotype(row);
                    if (polyAssay.getRsId().contains("AMG_3b")) {
                        if (genotype.isCall()) {
                            gender = genotype.isHet() ? Gender.MALE : Gender.FEMALE;
                        } else {
                            gender = Gender.UNKNOWN;       // No call symbol...
                        }
                    } else {
                        // TODO JW handle chromosome? See FingerprintManager.getFluidigmGenotypeGenderCall
                    }
                    if (gender != null) {
                        LabMetric labMetric = new LabMetric(new BigDecimal(gender.getNumXChromosomes()), LabMetric.MetricType.FLUIDIGM_GENDER, LabMetric.LabUnit.GENDER,
                                plateWell.getVesselPosition().name(), new Date());
                        plateWell.addMetric(labMetric);
                        run.getLabMetrics().add(labMetric);
                        mapPlateWellToGender.put(plateWell, gender);
                    }
                }
            }
        }
        return mapPlateWellToGender;
    }

    private Genotype parseGenotype(FluidigmChipProcessor.FluidigmDataRow record) {
        Genotype genotype = new Genotype();
        genotype.setConfidence(new Double(record.getConfidence()));

        String autocall = record.getAutoGenotype();
        String finalCall = record.getFinalGenotype();
        String convertedFinalCall = record.getConverted(); // A:A, G:T, NTC, ... (of the form Allele[X|Y]:Allele[X|Y]

        String convertedFinalCallA;
        String convertedFinalCallB;
        if (finalCall.equals("NTC") || finalCall.equals("No Call") || finalCall.equals("Invalid")) {
            convertedFinalCallA = null;
            convertedFinalCallB = null;
        } else {
            String[] genotypes = convertedFinalCall.split(":");
            if (genotypes.length < 2) {
                throw new RuntimeException("Sample: " + record.getSampleName() +
                                           " did not have a genotype with at least one colon and it is not NTC, No Call or Invalid");
            }

            convertedFinalCallA = genotypes[0];
            convertedFinalCallB = genotypes[1];
        }

        genotype.setAllele1(convertedFinalCallA);
        genotype.setAllele2(convertedFinalCallB);
        boolean userCall = false;
        if (!finalCall.equals(autocall)) {
            userCall = true;
        }

        genotype.setUserCall(userCall);
        return genotype;
    }

    private class Genotype {
        static public final String NO_CALL_ALLELE_CHAR =  "-";

        private double confidence;
        private String allele1;
        private String allele2;
        private boolean userCall;

        public void setConfidence(double confidence) {
            this.confidence = confidence;
        }

        public double getConfidence() {
            return confidence;
        }

        public void setAllele1(String allele1) {
            this.allele1 = allele1;
        }

        public String getAllele1() {
            return allele1;
        }

        public void setAllele2(String allele2) {
            this.allele2 = allele2;
        }

        public String getAllele2() {
            return allele2;
        }

        public void setUserCall(boolean userCall) {
            this.userCall = userCall;
        }

        public boolean isUserCall() {
            return userCall;
        }

        public boolean isHet() {
            return !allele1.equals(allele2);
        }

        public boolean isNoCall() {
            return ((this.allele1.equals(NO_CALL_ALLELE_CHAR)) && (this.allele2.equals(NO_CALL_ALLELE_CHAR)));
        }

        public boolean isCall() {
            return !isNoCall();
        }
    }

    public String convertIntToWellPosition(int number, int multiplier) {
        number--;
        char c = (char) (number / multiplier + 65);
        int i = number % multiplier + 1;
        StringBuilder sb = new StringBuilder(3);
        sb.append(c);
        if (i <= 9) {
            sb.append("0");
        }
        sb.append(i);
        return sb.toString().toUpperCase();
    }

    public enum Gender {
        MALE("M", "Male", 1),
        FEMALE("F", "Female", 2),
        UNKNOWN("U", "Unknown", 0),
        UNRECOGNIZED("?", "Unrecognized", -1);

        private final String symbol;
        private final String name;
        private final int numXChromosomes;

        private static final Map<Integer, Gender> MAP_X_CHROMOSOMES_TO_GENDER =
                new HashMap<>(Gender.values().length);

        static {
            for (Gender gender : Gender.values()) {
                MAP_X_CHROMOSOMES_TO_GENDER.put(gender.getNumXChromosomes(), gender);
            }
        }

        Gender(String symbol, String name, int numXChromosomes) {
            this.symbol = symbol;
            this.name = name;
            this.numXChromosomes = numXChromosomes;
        }

        public String getSymbol() {
            return symbol;
        }

        public String getName() {
            return name;
        }

        public int getNumXChromosomes() {
            return numXChromosomes;
        }

        public static Gender getByNumberOfXChromosomes(int numXChromosomes) {
            return MAP_X_CHROMOSOMES_TO_GENDER.get(numXChromosomes);
        }

    }
}

package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.collections4.CollectionUtils;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.infrastructure.jpa.DaoFree;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabMetricRunDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDao;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricDecision;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetricRun;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TubeFormation;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public LabMetricRun createFluidigmChipRun(InputStream inputStream, Long decidingUser, MessageCollection messageCollection) {
        FluidigmChipProcessor fluidigmChipProcessor = new FluidigmChipProcessor();
        FluidigmChipProcessor.FluidigmRun fluidigmRun = fluidigmChipProcessor.parse(inputStream);
        messageCollection.addAll(fluidigmChipProcessor.getMessageCollection());

        // Fetch Chip
        StaticPlate staticPlate = staticPlateDao.findByBarcode(fluidigmRun.getChipBarcode());
        if (staticPlate == null) {
            messageCollection.addError("Failed to find chip " + fluidigmRun.getChipBarcode());
            return null;
        }

        StaticPlate.TubeFormationByWellCriteria.Result traverserResult =
                staticPlate.nearestFormationAndTubePositionByWell();

        if (traverserResult.getTubeFormation() == null) {
            messageCollection.addError("Cannot find source tubes for plate " + staticPlate.getLabel());
            return null;
        }

        // TODO Maybe get back to this? There isn't really a concept of a run here?
        LabMetricRun labMetricRun = labMetricRunDao.findByName(fluidigmRun.buildRunName());
        if (labMetricRun != null) {
            messageCollection.addError("This run has been uploaded previously.");
            return labMetricRun;
        }

        // Run date must be unique so that a search can reveal the latest quant.
        List<LabMetricRun> sameDateRuns = labMetricRunDao.findSameDateRuns(fluidigmRun.getRunDate());
        if (CollectionUtils.isNotEmpty(sameDateRuns)) {
            messageCollection.addError("A previous upload has the same Run Started timestamp.");
            return sameDateRuns.iterator().next();
        }

        traverserResult.getWellToTubePosition();

        // TODO Can you re run the same chip? Like Accept Re-Pico?
        LabMetricRun run = createFluidigmRunDaoFree(fluidigmRun, staticPlate, decidingUser, messageCollection,
                traverserResult);

        return null;
    }

    @DaoFree
    public LabMetricRun createFluidigmRunDaoFree(FluidigmChipProcessor.FluidigmRun fluidigmRun,
                                                 StaticPlate staticPlate, Long decidingUser,
                                                 MessageCollection messageCollection,
                                                 StaticPlate.TubeFormationByWellCriteria.Result traverserResult) {
        // TODO Lab Metric Run
        LabMetricRun labMetricRun = new LabMetricRun(fluidigmRun.buildRunName(), fluidigmRun.getRunDate(),
                LabMetric.MetricType.FINAL_LIBRARY_SIZE);
        labMetricRun.getMetadata().add(new Metadata(Metadata.Key.INSTRUMENT_NAME,
                fluidigmRun.getInstrumentName()));

        TubeFormation tubeFormation = traverserResult.getTubeFormation();
        Map<LabVessel, List<FluidigmChipProcessor.FluidigmDataRow>> mapLabVesselToRecords = new HashMap<>();
        for (FluidigmChipProcessor.FluidigmDataRow record: fluidigmRun.getRecords()) {
            String chamberId = record.getId();
            String sampleName = chamberId.split("-")[0]; // S01
            int sampleNum = Integer.parseInt(sampleName.substring(1));
            String well = convertIntToWellPosition(sampleNum, 12);
            VesselPosition vesselPosition = VesselPosition.getByName(well);
            LabVessel sourceTube = tubeFormation.getContainerRole().getVesselAtPosition(vesselPosition);
            if (!mapLabVesselToRecords.containsKey(sourceTube)) {
                mapLabVesselToRecords.put(sourceTube, new ArrayList<FluidigmChipProcessor.FluidigmDataRow>());
            }
            mapLabVesselToRecords.get(sourceTube).add(record);
        }

        double q17CallConfidenceThreshold = 98.0;

        // For each tube build Q20 Scores??
        for (Map.Entry<LabVessel, List<FluidigmChipProcessor.FluidigmDataRow>> entry: mapLabVesselToRecords.entrySet()) {
            LabVessel labVessel = entry.getKey();

            VesselPosition position = tubeFormation.getContainerRole().getPositionOfVessel(labVessel);
            // Calculate Q17 Score
            // Calculate Gender Discordances
            // Try Count can be in UDS
            double totalGenotypes = entry.getValue().size();
            double calls = 0.0;
            for (FluidigmChipProcessor.FluidigmDataRow row: entry.getValue()) {
                Genotype genotype = parseGenotype(row);
                if (false) { //TODO PolyAssay

                } else {
                    boolean userCall = genotype.isUserCall();
                    if (!userCall) {
                        if (genotype.getConfidence() >= q17CallConfidenceThreshold) {
                            calls++;
                        }
                    } else {
                        calls++; //TODO Bad Logic here? Not actually
                    }
                }
            }

            BigDecimal callRate = new BigDecimal((calls / totalGenotypes ) * 100);
            callRate = callRate.setScale(2, BigDecimal.ROUND_HALF_UP);
                System.out.println("Call Rate: " + position.name() + " " + calls + "/" + totalGenotypes);

                LabMetric.MetricType metricType = LabMetric.MetricType.CALL_RATE_Q17;
            LabMetric labMetric = new LabMetric(callRate, LabMetric.MetricType.CALL_RATE_Q17,
                    LabMetric.LabUnit.PERCENTAGE, position.name(), new Date());
            labMetricRun.addMetric(labMetric);
            LabMetricDecision decision = metricType.getDecider().makeDecision(labVessel, labMetric, decidingUser);
            labMetric.setLabMetricDecision(decision);
            Metadata totalCallsMetadata = new Metadata(Metadata.Key.TOTAL_POSSIBLE_CALLS, new BigDecimal(totalGenotypes));
            Metadata callsMetadata = new Metadata(Metadata.Key.CALLS, new BigDecimal(calls));
            labMetric.getMetadataSet().add(totalCallsMetadata);
            labMetric.getMetadataSet().add(callsMetadata);
        }

        return labMetricRun;
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

            // This used to get an array index out of bounds error, so now we throw an error to let the user
            // know that their file is invalid.
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
    }

    public static String convertIntToWellPosition(int number, int multiplier) {
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
}

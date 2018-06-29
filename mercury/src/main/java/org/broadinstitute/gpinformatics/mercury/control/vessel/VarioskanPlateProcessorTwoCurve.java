package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

public final class VarioskanPlateProcessorTwoCurve {

    private final List<String> validationMessages = new ArrayList<>();

    public enum PicoCurve {
        BROAD_RANGE("QuantitativeCurveFit1", BigDecimal.TEN, new BigDecimal("100")),
        HIGH_SENSE("QuantitativeCurveFit2", BigDecimal.ZERO, BigDecimal.TEN);

        private final String sheetname;
        private final BigDecimal lowestAccurateRead;
        private final BigDecimal highestAccurateRead;

        PicoCurve(String sheetname, BigDecimal lowestAccurateRead, BigDecimal highestAccurateRead) {
            this.sheetname = sheetname;
            this.lowestAccurateRead = lowestAccurateRead;
            this.highestAccurateRead = highestAccurateRead;
        }

        public String getSheetname() {
            return sheetname;
        }

        public BigDecimal getLowestAccurateRead() {
            return lowestAccurateRead;
        }

        public BigDecimal getHighestAccurateRead() {
            return highestAccurateRead;
        }
    }

    private Workbook workbook;

    public VarioskanPlateProcessorTwoCurve(Workbook workbook) throws ValidationException {
        this.workbook = workbook;
        for (PicoCurve curve: PicoCurve.values()) {
            if (workbook.getSheet(curve.getSheetname()) == null) {
                throw new ValidationException(curve.getSheetname() + " Sheet doesn't exist in Workbook");
            }
        }
    }

    public List<VarioskanPlateProcessor.PlateWellResult> processMultipleCurves(
            Map<String, StaticPlate.TubeFormationByWellCriteria.Result> mapBarcodeToTraverser,
            LabMetric.MetricType metricType)
            throws ValidationException {
        List<VarioskanPlateProcessor.PlateWellResult> finalValues = new ArrayList<>();
        List<VarioskanPlateProcessor.PlateWellResult> broadRange = parseSheet(PicoCurve.BROAD_RANGE, metricType);
        List<VarioskanPlateProcessor.PlateWellResult> highSense = parseSheet(PicoCurve.HIGH_SENSE, metricType);

        Iterator<VarioskanPlateProcessor.PlateWellResult> brIter = broadRange.iterator();
        Iterator<VarioskanPlateProcessor.PlateWellResult> hsIter = highSense.iterator();
        Map<LabVessel, List<VarioskanPlateProcessor.PlateWellResult>> mapBarcodeToBroadRange = new HashMap<>();
        Map<LabVessel, List<VarioskanPlateProcessor.PlateWellResult>> mapBarcodeToHighSense= new HashMap<>();
        while (brIter.hasNext() && hsIter.hasNext()) {
            VarioskanPlateProcessor.PlateWellResult hsResult = hsIter.next();
            VarioskanPlateProcessor.PlateWellResult brResult = brIter.next();
            StaticPlate.TubeFormationByWellCriteria.Result result = mapBarcodeToTraverser.get(hsResult.getPlateBarcode());
            if (result == null) {
                continue;
            }
            VesselPosition tubePos = result.getWellToTubePosition().get(hsResult.getVesselPosition());
            LabVessel sourceTube = result.getTubeFormation().getContainerRole().getVesselAtPosition(tubePos);
            if (sourceTube != null) {
                // Broad Range Over the Curve - Set to top of the curve.
                if (brResult.isNaN() && brResult.getValue().compareTo(BigDecimal.ZERO) > 0) {
                    brResult.setResult(PicoCurve.BROAD_RANGE.getHighestAccurateRead());
                }
                if (!mapBarcodeToBroadRange.containsKey(sourceTube)) {
                    mapBarcodeToBroadRange.put(sourceTube, new ArrayList<>());
                }
                if (!mapBarcodeToHighSense.containsKey(sourceTube)) {
                    mapBarcodeToHighSense.put(sourceTube, new ArrayList<>());
                }
                mapBarcodeToBroadRange.get(sourceTube).add(brResult);
                mapBarcodeToHighSense.get(sourceTube).add(hsResult);
            }
        }

        // Go through triplicates and determine if BR should be used or HS curve
        for (LabVessel sourceTube: mapBarcodeToBroadRange.keySet()) {
            List<VarioskanPlateProcessor.PlateWellResult> brTrips = mapBarcodeToBroadRange.get(sourceTube);
            List<VarioskanPlateProcessor.PlateWellResult> hsTrips = mapBarcodeToHighSense.get(sourceTube);
            List<VarioskanPlateProcessor.PlateWellResult> brNaN =
                    brTrips.stream().filter(VarioskanPlateProcessor.PlateWellResult::isNaN)
                            .collect(Collectors.toList());
            boolean useHighSense = false;
            if (brNaN.size() >= 2) { // Check HS Curve
                useHighSense = true;
            } else {
                OptionalDouble optAverage = brTrips.stream().filter(res -> !res.isNaN())
                        .mapToDouble(res -> res.getResult().floatValue()).average();
                if (optAverage.isPresent()) {
                    BigDecimal avg = new BigDecimal(optAverage.getAsDouble());
                    if (avg.compareTo(PicoCurve.BROAD_RANGE.getLowestAccurateRead()) > 0) {
                        finalValues.addAll(brTrips);
                    } else {
                        useHighSense = true;
                    }
                } else {
                    System.out.println("What ahppended?");
                }
            }

            if (useHighSense) {
                finalValues.addAll(hsTrips);
            }
        }

        return finalValues;
    }

    private List<VarioskanPlateProcessor.PlateWellResult> parseSheet(PicoCurve curve, LabMetric.MetricType metricType)
            throws ValidationException {
        VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                curve.getSheetname(), metricType, false, curve);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.emptyMap());
        parser.processRows(workbook.getSheet(curve.getSheetname()), varioskanPlateProcessor);
        validationMessages.addAll(varioskanPlateProcessor.getMessages());
        return varioskanPlateProcessor.getPlateWellResults();
    }

    public List<String> getMessages() {
        return validationMessages;
    }
}

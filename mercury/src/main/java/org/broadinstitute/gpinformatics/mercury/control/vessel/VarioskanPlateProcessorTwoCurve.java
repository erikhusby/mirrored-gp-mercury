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
        // Plating Pico curve actually goes to 20 but the software extrapolates to a max of 100
        PLATING_BROAD_RANGE("QuantitativeCurveFit1", BigDecimal.ZERO, new BigDecimal("100")),
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

    public List<VarioskanPlateProcessor.PlateWellResult> processMultipleCurves(LabMetric.MetricType metricType)
            throws ValidationException {
        List<VarioskanPlateProcessor.PlateWellResult> finalValues = new ArrayList<>();
        List<VarioskanPlateProcessor.PlateWellResult> broadRange = parseSheet(PicoCurve.BROAD_RANGE, metricType);
        List<VarioskanPlateProcessor.PlateWellResult> highSense = parseSheet(PicoCurve.HIGH_SENSE, metricType);

        Iterator<VarioskanPlateProcessor.PlateWellResult> brIter = broadRange.iterator();
        Iterator<VarioskanPlateProcessor.PlateWellResult> hsIter = highSense.iterator();
        while (brIter.hasNext() && hsIter.hasNext()) {
            VarioskanPlateProcessor.PlateWellResult brResult = brIter.next();
            VarioskanPlateProcessor.PlateWellResult hsResult = hsIter.next();
            if (brResult.isNaN()) {
                if (brResult.getValue().compareTo(PicoCurve.BROAD_RANGE.getHighestAccurateRead()) > 0) {
                    brResult.setResult(PicoCurve.BROAD_RANGE.getHighestAccurateRead());
                    brResult.setOverTheCurve(true);
                    finalValues.add(brResult);
                } else if (hsResult.isNaN()) {
                    hsResult.setResult(null);
                    finalValues.add(hsResult);
                } else {
                    finalValues.add(hsResult);
                }
            } else if (brResult.getResult().compareTo(PicoCurve.BROAD_RANGE.getLowestAccurateRead()) > 0) {
                finalValues.add(brResult);
            } else if (hsResult.isNaN()) {
                hsResult.setResult(null);
                finalValues.add(hsResult);
            } else {
                finalValues.add(hsResult);
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

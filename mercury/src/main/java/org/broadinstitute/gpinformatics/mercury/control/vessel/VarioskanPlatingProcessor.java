package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VarioskanPlatingProcessor {
    private final List<String> validationMessages = new ArrayList<>();

    private Workbook workbook;
    private VarioskanPlateProcessorTwoCurve.PicoCurve picoCurve = VarioskanPlateProcessorTwoCurve.PicoCurve.PLATING_BROAD_RANGE;

    public VarioskanPlatingProcessor(Workbook workbook) throws ValidationException {
        this.workbook = workbook;
        if (workbook.getSheet(picoCurve.getSheetname()) == null) {
            throw new ValidationException(picoCurve.getSheetname() + " Sheet doesn't exist in Workbook");
        }
    }

    public List<VarioskanPlateProcessor.PlateWellResult> process(LabMetric.MetricType metricType)
            throws ValidationException {
        List<VarioskanPlateProcessor.PlateWellResult> finalValues = new ArrayList<>();
        List<VarioskanPlateProcessor.PlateWellResult> broadRange = parseSheet(picoCurve, metricType);

        for (VarioskanPlateProcessor.PlateWellResult brResult: broadRange) {
            if (brResult.isNaN()) {
                if (brResult.getValue().compareTo(picoCurve.getHighestAccurateRead()) > 0) {
                    brResult.setResult(picoCurve.getHighestAccurateRead());
                    brResult.setOverTheCurve(true);
                } else {
                    brResult.setResult(BigDecimal.ZERO);
                }
            }
            finalValues.add(brResult);
        }

        return finalValues;
    }

    private List<VarioskanPlateProcessor.PlateWellResult> parseSheet(VarioskanPlateProcessorTwoCurve.PicoCurve curve, LabMetric.MetricType metricType)
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

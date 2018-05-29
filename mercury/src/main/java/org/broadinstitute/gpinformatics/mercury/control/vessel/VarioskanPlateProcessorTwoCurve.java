package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.parsers.TableProcessor;
import org.broadinstitute.gpinformatics.infrastructure.parsers.poi.PoiSpreadsheetParser;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class VarioskanPlateProcessorTwoCurve {

    private final List<String> validationMessages = new ArrayList<>();

    public enum PicoCurve {
        BROAD_RANGE("QuantitativeCurveFit1", new BigDecimal("10")),
        HIGH_SENSE("QuantitativeCurveFit2", BigDecimal.ZERO);

        private final String sheetname;
        private final BigDecimal lowestAccurateRead;

        PicoCurve(String sheetname, BigDecimal lowestAccurateRead) {
            this.sheetname = sheetname;
            this.lowestAccurateRead = lowestAccurateRead;
        }

        public String getSheetname() {
            return sheetname;
        }

        public BigDecimal getLowestAccurateRead() {
            return lowestAccurateRead;
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
            VarioskanPlateProcessor.PlateWellResult hsResult = hsIter.next();
            VarioskanPlateProcessor.PlateWellResult brResult = brIter.next();

            if (brResult.getResult().compareTo(PicoCurve.BROAD_RANGE.getLowestAccurateRead()) > 0) {
                finalValues.add(brResult);
            } else {
                finalValues.add(hsResult);
            }
        }
        return finalValues;
    }

    private List<VarioskanPlateProcessor.PlateWellResult> parseSheet(PicoCurve curve, LabMetric.MetricType metricType)
            throws ValidationException {
        VarioskanPlateProcessor varioskanPlateProcessor = new VarioskanPlateProcessor(
                curve.getSheetname(), metricType);
        PoiSpreadsheetParser parser = new PoiSpreadsheetParser(Collections.<String, TableProcessor>emptyMap());
        parser.processRows(workbook.getSheet(curve.getSheetname()), varioskanPlateProcessor);
        validationMessages.addAll(varioskanPlateProcessor.getMessages());
        return varioskanPlateProcessor.getPlateWellResults();
    }

    public List<String> getMessages() {
        return validationMessages;
    }
}

package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.common.MathUtils;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import java.math.BigDecimal;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class LunaticPlateProcessor {
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance("dd/MM/yyyy HH:mm:ss");
    private static final String COLUMN_LABELS = "ABCDEFGHIJK";
    private static final int PLATE_ID_COLUMN = 0;
    private static final int POSITION_COLUMN = 1;
    private static final int CONC_COLUMN = 4;
    private static final String DATE_ROW_HEADER = "Date";
    private static final String PLATE_ID_HEADER = "Plate ID";
    private static final String POSITION_HEADER = "Plate Position";
    private static final String CONC_HEADER_ENDING = "Concentration (ng/ul)";

    public static Triple<String, Date, List<VarioskanPlateProcessor.PlateWellResult>> parse(Workbook workbook,
            String filename) throws ValidationException {

        final Sheet sheet = workbook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();

        // Parses the spreadsheet up to and including the data table header row.
        MutablePair<Date, Boolean> dateAndFoundHeader = new MutablePair(new Date(), null);
        while (dateAndFoundHeader.getRight() == null && rowIterator.hasNext()) {
            Row row = rowIterator.next();
            parseUpperRow(dateAndFoundHeader, row);
        }
        if (dateAndFoundHeader.getRight() == null) {
            throw new ValidationException("Cannot find \"" + PLATE_ID_HEADER + "\" header in column " +
                    COLUMN_LABELS.charAt(PLATE_ID_COLUMN) + " on any row.");
        }
        // Parses the data table rows.
        List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = new ArrayList<>();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            parseDataRow(plateWellResults, row);
        }
        return Triple.of(filename, dateAndFoundHeader.getLeft(), plateWellResults);
    }

    /** Parses the spreadsheet row and sets the runDate and header status in the mutable pair. */
    private static void parseUpperRow(MutablePair<Date, Boolean> pair, Row row) throws ValidationException {
        Cell cell0 = row.getCell(0);
        if (cell0 != null && cell0.getCellType() == Cell.CELL_TYPE_STRING) {
            if (cell0.getStringCellValue().equals(DATE_ROW_HEADER)) {
                // Captures the run date.
                try {
                    pair.setLeft(dateFormat.parse(row.getCell(1).getStringCellValue()));
                } catch (ParseException e) {
                    throw new ValidationException("Cannot parse run date on row " + (row.getRowNum() + 1));
                } catch (Exception e) {
                    throw new ValidationException("Invalid date on row " + (row.getRowNum() + 1) + " : " +
                            e.toString());
                }
            } else if (row.getCell(PLATE_ID_COLUMN) != null &&
                    StringUtils.normalizeSpace(row.getCell(PLATE_ID_COLUMN).getStringCellValue()).
                            equals(PLATE_ID_HEADER)) {
                // Looks for the required headers in the expected columns.
                if (row.getCell(POSITION_COLUMN) == null ||
                        !StringUtils.normalizeSpace(row.getCell(POSITION_COLUMN).getStringCellValue()).
                                equals(POSITION_HEADER)) {
                    throw new ValidationException("Cannot find \"" + POSITION_HEADER + "\" header in column " +
                            COLUMN_LABELS.charAt(POSITION_COLUMN) + " on row " + (row.getRowNum() + 1));
                }
                if (row.getCell(CONC_COLUMN) == null ||
                        !StringUtils.normalizeSpace(row.getCell(CONC_COLUMN).getStringCellValue()).
                                endsWith(CONC_HEADER_ENDING)) {
                    throw new ValidationException(
                            "Cannot find header ending with \"" + CONC_HEADER_ENDING + "\" in column " +
                                    COLUMN_LABELS.charAt(POSITION_COLUMN) + " on row " + (row.getRowNum() + 1));
                }
                pair.setRight(true);
            }
        }
    }

    /* Parses the spreadsheet row and adds a data dto to the the list. */
    private static void parseDataRow(List<VarioskanPlateProcessor.PlateWellResult> plateWellResults, Row row)
            throws ValidationException {
        if (row.getCell(PLATE_ID_COLUMN) != null && row.getCell(POSITION_COLUMN) != null &&
                row.getCell(CONC_COLUMN) != null) {
            String plateId = null;
            try {
                plateId = row.getCell(PLATE_ID_COLUMN).getStringCellValue();
            } catch (Exception e) {
                throw new ValidationException("Invalid plate barcode on row " + (row.getRowNum() + 1) + " : " +
                        e.toString());
            }
            String position = null;
            try {
                position = row.getCell(POSITION_COLUMN).getStringCellValue();
            } catch (Exception e) {
                throw new ValidationException("Invalid well position on row " + (row.getRowNum() + 1) + " : " +
                        e.toString());
            }
            VesselPosition vesselPosition = VesselPosition.getByName(position);
            if (vesselPosition == null) {
                throw new ValidationException("Invalid well position on row " + (row.getRowNum() + 1));
            }
            BigDecimal concentration = BigDecimal.ZERO;
            try {
                double conc = (row.getCell(CONC_COLUMN).getCellTypeEnum() == CellType.NUMERIC) ?
                        row.getCell(CONC_COLUMN).getNumericCellValue() :
                        // "N/A" or any other non-numeric string becomes 0.0
                        NumberUtils.toDouble(row.getCell(CONC_COLUMN).getStringCellValue());
                if (conc > 0) {
                    concentration = MathUtils.scaleTwoDecimalPlaces(new BigDecimal(conc));
                }
            } catch (Exception e) {
                throw new ValidationException("Invalid concentration on row " + (row.getRowNum() + 1) + " : " +
                        e.toString());
            }
            plateWellResults.add(new VarioskanPlateProcessor.PlateWellResult(plateId, vesselPosition,
                    concentration));
        }
    }
}

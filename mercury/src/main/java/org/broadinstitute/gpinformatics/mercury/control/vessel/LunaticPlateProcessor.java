package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.time.FastDateFormat;
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
    private static final String DATE_ROW_HEADER = "Date";
    private static final int DATE_VALUE_COLUMN_INDEX = 1;
    private static final String PLATE_ID_HEADER = "Plate ID";
    private static final String POSITION_HEADER = "Plate Position";
    private static final String CONC_HEADER_ENDING = "Concentration (ng/ul)";

    private Sheet sheet;
    private int plateIdColumnIndex = -1;
    private int positionColumnIndex = -1;
    private int concColumnIndex = -1;
    private Date runDate = null;
    private int headerRowIndex = -1;
    private List<VarioskanPlateProcessor.PlateWellResult> plateWellResults = new ArrayList<>();

    public static Triple<String, Date, List<VarioskanPlateProcessor.PlateWellResult>> parse(Workbook workbook,
            String filename) throws ValidationException {

        LunaticPlateProcessor processor = new LunaticPlateProcessor(workbook);
        processor.parseHeaders();
        processor.parseData();
        return Triple.of(filename, processor.getRunDate(), processor.getPlateWellResults());
    }

    public LunaticPlateProcessor(Workbook workbook) {
        this.sheet = workbook.getSheetAt(0);
    }

    /**
     * Parses the spreadsheet looking for the date and the header row.
     */
    private void parseHeaders() throws ValidationException {
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            Cell cell0 = row.getCell(0);
            if (cell0 != null && cell0.getCellTypeEnum() == CellType.STRING) {
                String cell0Value = StringUtils.normalizeSpace(cell0.getStringCellValue());

                // Looks for a row with "Date" in column 0 and captures the run date.
                if (cell0Value.equals(DATE_ROW_HEADER) && row.getCell(DATE_VALUE_COLUMN_INDEX) != null) {
                    try {
                        runDate = dateFormat.parse(row.getCell(DATE_VALUE_COLUMN_INDEX).getStringCellValue());
                    } catch (ParseException e) {
                        throw new ValidationException("Cannot parse run date on row " + (row.getRowNum() + 1));
                    } catch (Exception e) {
                        throw new ValidationException("Invalid date on row " + (row.getRowNum() + 1) + " : " +
                                e.toString());
                    }
                } else if (cell0Value.equals(PLATE_ID_HEADER)) {
                    // Looks for a row with "Plate ID" in column 0 which indicates the header row.
                    // Finds the column indexes for the headers of interest.
                    setPlateIdColumnIndex(0);
                    for (int columnIndex = 1; columnIndex < row.getLastCellNum(); ++columnIndex) {
                        Cell cell = row.getCell(columnIndex);
                        if (cell != null && cell.getCellTypeEnum() == CellType.STRING) {
                            String value = StringUtils.normalizeSpace(cell.getStringCellValue());
                            if (value.equals(POSITION_HEADER)) {
                                setPositionColumnIndex(columnIndex);
                            } else if (value.endsWith(CONC_HEADER_ENDING)) {
                                setConcColumnIndex(columnIndex);
                            }
                        }
                    }
                    if (getPositionColumnIndex() < 0) {
                        throw new ValidationException("Expected to find \"" + POSITION_HEADER + "\" in row " +
                                (row.getRowNum() + 1));
                    } else if (getConcColumnIndex() < 0) {
                        throw new ValidationException("Expected to find \"" + CONC_HEADER_ENDING + "\" in row " +
                                (row.getRowNum() + 1));
                    }
                    setHeaderRowIndex(row.getRowNum());
                }
            }
        }
        if (getRunDate() == null) {
            setRunDate(new Date());
        }
        if (getPlateIdColumnIndex() < 0) {
            throw new ValidationException("Cannot find the header row which is expected to have \"" +
                    PLATE_ID_HEADER + "\" in column A.");
        }
    }

    /**
     * Parses the spreadsheet data starting after the header row.
     */
    private void parseData() throws ValidationException {
        Iterator<Row> rowIterator = sheet.rowIterator();
        while (rowIterator.hasNext()) {
            Row row = rowIterator.next();
            if (row.getRowNum() > getHeaderRowIndex() &&
                    row.getCell(getPlateIdColumnIndex()) != null &&
                    row.getCell(getPositionColumnIndex()) != null &&
                    row.getCell(getConcColumnIndex()) != null) {
                String plateId;
                try {
                    plateId = row.getCell(getPlateIdColumnIndex()).getStringCellValue();
                } catch (Exception e) {
                    throw new ValidationException("Cannot parse the value of " + PLATE_ID_HEADER +
                            " on row " + (row.getRowNum() + 1) + " : " + e.toString());
                }
                String position;
                try {
                    position = row.getCell(getPositionColumnIndex()).getStringCellValue();
                } catch (Exception e) {
                    throw new ValidationException("Cannot parse the value of " + POSITION_HEADER +
                            " on row " + (row.getRowNum() + 1) + " : " + e.toString());
                }
                VesselPosition vesselPosition = VesselPosition.getByName(position);
                if (vesselPosition == null) {
                    throw new ValidationException("Invalid value of " + POSITION_HEADER +
                            " on row " + (row.getRowNum() + 1));
                }

                Double doubleConc = null;
                Cell concCell = row.getCell(getConcColumnIndex());
                // "N/A" or other non-numeric value causes no quant saved for this position.
                if (concCell.getCellTypeEnum() == CellType.STRING) {
                    String value = concCell.getStringCellValue();
                    if (NumberUtils.isCreatable(value)) {
                        doubleConc = NumberUtils.createDouble(value);
                    }
                } else if (concCell.getCellTypeEnum() == CellType.NUMERIC) {
                    doubleConc = concCell.getNumericCellValue();
                }
                if (doubleConc != null) {
                    // Negative values are set to 0.
                    BigDecimal concentration = (doubleConc > 0) ?
                            MathUtils.scaleTwoDecimalPlaces(new BigDecimal(doubleConc)) : BigDecimal.ZERO;
                    plateWellResults.add(new VarioskanPlateProcessor.PlateWellResult(plateId, vesselPosition,
                            concentration));
                }
            }
        }

    }

    public int getPlateIdColumnIndex() {
        return plateIdColumnIndex;
    }

    public void setPlateIdColumnIndex(int plateIdColumnIndex) {
        this.plateIdColumnIndex = plateIdColumnIndex;
    }

    public int getPositionColumnIndex() {
        return positionColumnIndex;
    }

    public void setPositionColumnIndex(int positionColumnIndex) {
        this.positionColumnIndex = positionColumnIndex;
    }

    public int getConcColumnIndex() {
        return concColumnIndex;
    }

    public void setConcColumnIndex(int concColumnIndex) {
        this.concColumnIndex = concColumnIndex;
    }

    public Date getRunDate() {
        return runDate;
    }

    public void setRunDate(Date runDate) {
        this.runDate = runDate;
    }

    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    public List<VarioskanPlateProcessor.PlateWellResult> getPlateWellResults() {
        return plateWellResults;
    }

    public void setPlateWellResults(
            List<VarioskanPlateProcessor.PlateWellResult> plateWellResults) {
        this.plateWellResults = plateWellResults;
    }
}
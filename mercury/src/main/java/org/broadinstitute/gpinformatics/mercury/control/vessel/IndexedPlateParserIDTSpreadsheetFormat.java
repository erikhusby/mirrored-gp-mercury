package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of parser for solexa indexed plate upload input data.
 * Input is an Excel file
 */
public class IndexedPlateParserIDTSpreadsheetFormat implements IndexedPlateParser {

    private final DataFormatter dataFormatter = new DataFormatter();

    private final String technology = MolecularIndexingScheme.IndexPosition.ILLUMINA_P7.getTechnology();

    abstract static class ColumnParser {
        public abstract int getColumnIndex();
        public abstract String getColumnName();

        public String getString(Row row) {
            Cell cell = row.getCell(getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(getColumnName() + " is empty in row " + row.getRowNum());
            }

            return IndexedPlateParserIDTSpreadsheetFormat.getString(cell);
        }
    }

    private static String getString(Cell cell) {
        switch (cell.getCellType()) {
            case Cell.CELL_TYPE_NUMERIC :
                double value = cell.getNumericCellValue();
                if (value % 1 == 0) {
                    // getNumericCellValue() returns a primitive double. If it's not
                    // actually a double -- i.e., if it could be represented as an
                    // int -- return just the integer part.
                    return String.valueOf((long) value);
                } else {
                    return String.valueOf(value);
                }

            case Cell.CELL_TYPE_STRING :
                return cell.getStringCellValue();

            case Cell.CELL_TYPE_FORMULA :
                throw new IllegalArgumentException(
                        "The cell in column " + (cell.getColumnIndex() + 1) +
                                ", row " + (cell.getRowIndex() + 1) +
                                "is a formula. Please expand the formulas to literal values and try again.");

            case Cell.CELL_TYPE_BLANK :
                throw new IllegalArgumentException(
                        "The cell in column " + (cell.getColumnIndex() + 1) +
                                ", row " + (cell.getRowIndex() + 1) + "is blank.");

            case Cell.CELL_TYPE_BOOLEAN :
                return cell.getBooleanCellValue() ? "true" : "false";

            case Cell.CELL_TYPE_ERROR :
                throw new IllegalStateException(
                        "The sheet has an error in column " + (cell.getColumnIndex() + 1) +
                                ", row " + (cell.getRowIndex() + 1));

            default :
                throw new IllegalStateException("An unknown cell type was passed to the parser.");
        }
    }

    private final ColumnParser broadBarcodeColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 3;
        }

        @Override
        public String getColumnName() {
            return "Broad Barcode";
        }

        @Override
        public String getString(Row row) {
            Cell cell = row.getCell(getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(getColumnName() + " is empty in row " + row.getRowNum());
            }

            // The barcode may be formatted with leading zeros, so we want the formatted value
            return dataFormatter.formatCellValue(cell);
        }
    };

    private final ColumnParser wellPositionColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 6;
        }

        @Override
        public String getColumnName() {
            return "Well Position";
        }
    };

    private final ColumnParser antisenseSequenceColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 11;
        }

        @Override
        public String getColumnName() {
            return "Antisense Sequence";
        }

        @Override
        public String getString(Row row) {
            Cell cell = row.getCell(getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(getColumnName() + " is empty in row " + row.getRowNum());
            }
            return cell.getStringCellValue().substring(39, 39 + 8);
        }
    };

    List<ColumnParser> getColumnParsers() {
        List<ColumnParser> parsers = new ArrayList<>(5);
        parsers.add(antisenseSequenceColumnParser);
        parsers.add(broadBarcodeColumnParser);
        parsers.add(wellPositionColumnParser);
        return parsers;
    }

    @Override
    public List<PlateWellIndexAssociation> parseInputStream(InputStream inputStream) {
        Sheet sheet;
        try {
            sheet = WorkbookFactory.create(inputStream).getSheetAt(0);
        } catch (Exception e) {
            throw new RuntimeException("Could not open the uploaded sheet: " + e.getMessage(), e);
        }

        validateWorksheet(sheet);

        List<PlateWellIndexAssociation> plateIndexes = new ArrayList<>();

        int lastRowNum = sheet.getLastRowNum();
        int i = 1;
        try {
            for ( ; i <= lastRowNum; i++) {
                Row row = sheet.getRow(i);
                String plateBarcode = broadBarcodeColumnParser.getString(row);
                String wellName = wellPositionColumnParser.getString(row);
                String molecularIndex = antisenseSequenceColumnParser.getString(row);

                PlateWellIndexAssociation association =
                        new PlateWellIndexAssociation(plateBarcode, wellName, technology);
                association.addIndex(
                        MolecularIndexingScheme.getDefaultPositionHint(technology),
                        molecularIndex);

                plateIndexes.add(association);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not parse row " + (i + 1) + ": " + e.getMessage(), e);
        }

        return plateIndexes;
    }

    void validateWorksheet(Sheet sheet) {
        Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new RuntimeException("The spreadsheet is empty.");
        }

        for (ColumnParser parser : getColumnParsers()) {
            // Verify that the headers are in the correct column and contain
            // the correct text, else the indexes will be invalid
            String headerString = headerRow.getCell(parser.getColumnIndex()).getStringCellValue();
            if (headerString == null) {
                throw new RuntimeException("There's no header text in column " + parser.getColumnIndex());
            }

            if ( ! headerString.equals(parser.getColumnName())) {
                throw new RuntimeException(
                        "Expected header " + parser.getColumnName() +
                                " in column " + parser.getColumnIndex() +
                                ", but found " + headerString);
            }
        }
    }

    String getTechnology() {
        return technology;
    }
}

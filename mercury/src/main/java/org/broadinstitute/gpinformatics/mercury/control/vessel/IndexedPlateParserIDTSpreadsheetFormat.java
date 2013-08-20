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

//	private enum Columns {
//		BROAD_BARCODE(3, "Broad Barcode");
//
//		final int columnIndex;
//		final String columnName;
//
//		Columns(final int index, final String name) {
//			this.columnIndex = index;
//			this.columnName = name;
//		}
//	}

    abstract class ColumnParser {
        public abstract int getColumnIndex();
        public abstract String getColumnName();

        public String getString(final Row row) {
            final Cell cell = row.getCell(this.getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(this.getColumnName() + " is empty in row " + row.getRowNum());
            }

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
        public String getString(final Row row) {
            final Cell cell = row.getCell(this.getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(this.getColumnName() + " is empty in row " + row.getRowNum());
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

    private final ColumnParser antisenseSequenceNameColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 10;
        }

        @Override
        public String getColumnName() {
            return "Antisense Seq Name";
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
        public String getString(final Row row) {
            final Cell cell = row.getCell(this.getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(this.getColumnName() + " is empty in row " + row.getRowNum());
            }
            return cell.getStringCellValue().substring(39, 39 + 8);
        }
    };

    List<ColumnParser> getColumnParsers() {
        final List<ColumnParser> parsers = new ArrayList<>(5);
        parsers.add(this.antisenseSequenceColumnParser);
        parsers.add(this.antisenseSequenceNameColumnParser);
        parsers.add(this.broadBarcodeColumnParser);
        parsers.add(this.wellPositionColumnParser);
        return parsers;
    }

    final DataFormatter dataFormatter = new DataFormatter();

    final String technology = MolecularIndexingScheme.IndexPosition.ILLUMINA_P7.getTechnology();

    @Override
    public List<PlateWellIndexAssociation> parseInputStream(final InputStream inputStream) {
        final Sheet sheet;
        try {
            sheet = WorkbookFactory.create(inputStream).getSheetAt(0);
        } catch (final Exception e) {
            throw new RuntimeException("Could not open the uploaded sheet: " + e.getMessage(), e);
        }

        this.validateWorksheet(sheet);

        final List<PlateWellIndexAssociation> plateIndexes = new ArrayList<>();

        final int lastRowNum = sheet.getLastRowNum();
        int i = 1;
        try {
            for ( ; i <= lastRowNum; i++) {
                final Row row = sheet.getRow(i);
                final String plateBarcode = this.broadBarcodeColumnParser.getString(row);
                final String wellName = this.wellPositionColumnParser.getString(row);
                final String molecularIndex = this.antisenseSequenceColumnParser.getString(row);

                final PlateWellIndexAssociation association =
                        new PlateWellIndexAssociation(plateBarcode, wellName, technology);
                association.addIndex(
                        MolecularIndexingScheme.getDefaultPositionHint(this.technology),
                        molecularIndex);

                plateIndexes.add(association);
            }

        } catch (final Exception e) {
            throw new RuntimeException("Could not parse row " + (i + 1) + ": " + e.getMessage(), e);
        }

        return plateIndexes;
    }

    void validateWorksheet(final Sheet sheet) {
        final Row headerRow = sheet.getRow(0);
        if (headerRow == null) {
            throw new RuntimeException("The spreadsheet is empty.");
        }

        for (final ColumnParser parser : this.getColumnParsers()) {
            // Verify that the headers are in the correct column and contain
            // the correct text, else the indexes will be invalid
            final String headerString = headerRow.getCell(parser.getColumnIndex()).getStringCellValue();
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
}

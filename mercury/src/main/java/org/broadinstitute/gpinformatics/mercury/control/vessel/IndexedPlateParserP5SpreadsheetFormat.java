package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for P5 index plate spreadsheets from IDT
 */
public class IndexedPlateParserP5SpreadsheetFormat extends IndexedPlateParserIDTSpreadsheetFormat {

    public IndexedPlateParserP5SpreadsheetFormat(MolecularIndexingScheme.IndexPosition illuminaPositionHint) {
        super(illuminaPositionHint);
    }

    final ColumnParser sequenceColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 8;
        }

        @Override
        public String getColumnName() {
            return "Sequence";
        }

        @Override
        public String getString(Row row) {
            Cell cell = row.getCell(getColumnIndex());
            if (cell == null) {
                throw new RuntimeException(getColumnName() + " is empty in row " + row.getRowNum());
            }
            // The sequence is triplets of bases, space separated.  The 8 variable bases are at position 37, and
            // include 2 spaces, so we have to remove the spaces
            return cell.getStringCellValue().substring(37, 37 + 10).replace(" ", "");
        }
    };

    @Override
    List<ColumnParser> getColumnParsers() {
        List<ColumnParser> parsers = new ArrayList<>(5);
        parsers.add(sequenceColumnParser);
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
                // Skip blank rows
                if(row.getLastCellNum() < 0) {
                    continue;
                }
                String plateBarcode = broadBarcodeColumnParser.getString(row);
                String wellName = wellPositionColumnParser.getString(row);
                String p5MolecularIndex = sequenceColumnParser.getString(row);

                PlateWellIndexAssociation association =
                        new PlateWellIndexAssociation(plateBarcode, wellName, MolecularIndexingScheme.IndexPosition.ILLUMINA_P5.getTechnology());
                association.addIndex(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5, p5MolecularIndex);

                plateIndexes.add(association);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not parse row " + (i + 1) + ": " + e.getMessage(), e);
        }

        return plateIndexes;
    }
}

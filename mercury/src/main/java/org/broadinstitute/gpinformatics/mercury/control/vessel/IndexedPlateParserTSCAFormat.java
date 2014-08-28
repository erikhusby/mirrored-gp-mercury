package org.broadinstitute.gpinformatics.mercury.control.vessel;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser for dual indexes from IDT
 */
public class IndexedPlateParserTSCAFormat extends IndexedPlateParserIDTSpreadsheetFormat {

    final ColumnParser broadBarcodeColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 3;
        }

        @Override
        public String getColumnName() {
            return "Broad Barcode";
        }
    };

    final ColumnParser wellPositionColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 6;
        }

        @Override
        public String getColumnName() {
            return "Well Position";
        }
    };

    final ColumnParser p5AntisenseSequenceColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 11;
        }

        @Override
        public String getColumnName() {
            return "i5 Primer Antisense Sequence";
        }
    };

    final ColumnParser p7AntisenseSequenceColumnParser = new ColumnParser() {
        @Override
        public int getColumnIndex() {
            return 13;
        }

        @Override
        public String getColumnName() {
            return "i7 Primer Antisense Sequence";
        }
    };


    List<ColumnParser> getColumnParsers() {
        List<ColumnParser> parsers = new ArrayList<>(5);
        parsers.add(p5AntisenseSequenceColumnParser);
        parsers.add(p7AntisenseSequenceColumnParser);
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
                String p5MolecularIndex = p5AntisenseSequenceColumnParser.getString(row);
                String p7MolecularIndex = p7AntisenseSequenceColumnParser.getString(row);

                PlateWellIndexAssociation association =
                        new PlateWellIndexAssociation(plateBarcode, wellName, getTechnology());
                association.addIndex(MolecularIndexingScheme.IndexPosition.ILLUMINA_P5, p5MolecularIndex);
                association.addIndex(MolecularIndexingScheme.IndexPosition.ILLUMINA_P7, p7MolecularIndex);

                plateIndexes.add(association);
            }

        } catch (Exception e) {
            throw new RuntimeException("Could not parse row " + (i + 1) + ": " + e.getMessage(), e);
        }

        return plateIndexes;
    }
}

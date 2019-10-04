package org.broadinstitute.gpinformatics.mercury.control.run;

import com.google.common.collect.Multimap;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.run.FpGenotype;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Stateful
@RequestScoped
public class FingerprintEjb {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private SampleDataFetcher sampleDataFetcher;



    public Workbook makeMatrix(List<Fingerprint> fingerprints, Set<String> platforms) {
        Map<String, Object[][]> sheets = new HashMap<>();
        Map<Fingerprint, Boolean> mapFpTest = new HashMap<>();

        for (Fingerprint fingerprint : fingerprints) {
            boolean include = fingerprint.getGender() != null && (platforms.contains(fingerprint.getPlatform().name()))
                              && fingerprint.getDisposition() == Fingerprint.Disposition.PASS;
            mapFpTest.put(fingerprint, include);
        }


        int numFingerprintCells = fingerprints.size() + 1;

        String[][] fluiLodCells = new String[numFingerprintCells][numFingerprintCells];

        int rowIndex = 0;
        int colIndex = 1;

        for (Fingerprint fingerprint : fingerprints) {
            if (mapFpTest.get(fingerprint)) {
                fluiLodCells[rowIndex][colIndex] = fingerprint.getMercurySample().getSampleKey();
                ++colIndex;
            }
        }
        ++rowIndex;

        for (Fingerprint fingerprint : fingerprints) {
            colIndex = 0;
            if (mapFpTest.get(fingerprint)) {
                fluiLodCells[rowIndex][colIndex] = fingerprint.getMercurySample().getSampleKey();
                ++colIndex;
                for (Fingerprint fingerprint1 : fingerprints) {
                    ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
                    if (mapFpTest.get(fingerprint1)) {
                        double lodScore = concordanceCalculator.calculateLodScore(fingerprint, fingerprint1);
                        fluiLodCells[rowIndex][colIndex] = Double.toString(lodScore);
                        ++colIndex;
                    }
                }
                ++rowIndex;
            }
        }

        String[] sheetNames = {"Fluidigm Matrix"};
        sheets.put(sheetNames[0], fluiLodCells);
        Workbook workbook = SpreadsheetCreator.createSpreadsheet(sheets);
        Sheet sheet = workbook.getSheet("Fluidigm Matrix");


        Row row;
        Cell cell;

        for (int i = 1; i < numFingerprintCells; i++) {
            row = sheet.createRow(i);
            for (int j = 0; j < numFingerprintCells; j++) {
                cell = row.createCell(j);
                if (fluiLodCells[i][j] != null) {
                    if (j == 0) {
                        cell.setCellValue(fluiLodCells[i][j]);
                    } else {
                        cell.setCellValue(Double.valueOf(fluiLodCells[i][j]));
                    }
                }
            }
        }


        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        //TODO Determine what cutoff(s) for highlighted cells
        ConditionalFormattingRule rule = sheetCF.createConditionalFormattingRule(ComparisonOperator.GT, "30");
        PatternFormatting fill = rule.createPatternFormatting();
        fill.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
        fill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        ConditionalFormattingRule[] cfRules = new ConditionalFormattingRule[]{rule};

        CellRangeAddress[] regions = new CellRangeAddress[]{
                new CellRangeAddress(1, numFingerprintCells, 1, numFingerprintCells)};

        sheetCF.addConditionalFormatting(regions, cfRules);

        return workbook;
    }


    public List<Fingerprint> findFingerints(Map<String, MercurySample> mapIdToMercurySample) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        Map<String, String> mapSmidToQueriedLsid = new HashMap<>();
        List<String> lsids = new ArrayList<>();
        for (String id : mapIdToMercurySample.keySet()) {
            String lsid = BSPSampleSearchServiceStub.LSID_PREFIX + id.substring(3);
            lsids.add(lsid);
            mapSmidToQueriedLsid.put(id, lsid);
        }

        // Make list of samples for which to query BSP
        List<String> bspLsids = new ArrayList<>();
        Map<String, String> mapSmidToFpLsid =
                FingerprintResource.getSmidToFpLsidMap(mapSmidToQueriedLsid, mapIdToMercurySample, bspLsids);

        //Query BSP for FP aliquots for given (sequencing) aliquots and map queried LSIDs to MercurySamples that may have fingerprints
        Multimap<String, MercurySample> mapLsidToFpSamples =
                FingerprintResource.getMercurySampleMultimap(lsids, mapIdToMercurySample, bspLsids, mapSmidToFpLsid,
                        bspGetExportedSamplesFromAliquots, mercurySampleDao);

        //
        Collection<MercurySample> mercurySamples = mapLsidToFpSamples.values();
        for (MercurySample mercurySample : mercurySamples) {
            fingerprints.addAll(mercurySample.getFingerprints());
        }

        //
        Map<String, SampleData> mapIdToData =
                sampleDataFetcher.fetchSampleDataForSamples(mercurySamples,
                        BSPSampleSearchColumn.PARTICIPANT_ID, BSPSampleSearchColumn.ROOT_SAMPLE);
        for (MercurySample mercurySample : mercurySamples) {
            mercurySample.setSampleData(mapIdToData.get(mercurySample.getSampleKey()));
        }

        return fingerprints;
    }


}

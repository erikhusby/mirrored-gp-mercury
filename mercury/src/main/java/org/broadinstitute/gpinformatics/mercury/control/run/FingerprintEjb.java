package org.broadinstitute.gpinformatics.mercury.control.run;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
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
import org.broadinstitute.bsp.client.search.SearchItem;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.SampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchServiceStub;
import org.broadinstitute.gpinformatics.infrastructure.search.LabMetricSearchDefinition;
import org.broadinstitute.gpinformatics.infrastructure.spreadsheet.SpreadsheetCreator;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
public class FingerprintEjb {

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    /**
     * Builds a pairwise matrix to display calculated lod scores for each combination of samples
     *
     * @param fingerprints list of fingerprints to compare in matrix
     * @param platforms    fingerprinting platforms used to compare
     * @return formatted workbook containing pairwise matrix of samples and lod scores
     */
    public Workbook makeMatrix(List<Fingerprint> fingerprints, Set<Fingerprint.Platform> platforms) {
        Map<String, Object[][]> sheets = new HashMap<>();
        Map<Fingerprint, Boolean> mapFpTest = new HashMap<>();

        if (platforms == null) {
            platforms = EnumSet.allOf(Fingerprint.Platform.class);
        }

        for (Fingerprint fingerprint : fingerprints) {
            boolean include = fingerprint.getGender() != null && (platforms.contains(fingerprint.getPlatform()))
                              && fingerprint.getDisposition() == Fingerprint.Disposition.PASS;
            mapFpTest.put(fingerprint, include);
        }

        int numFingerprintCells = fingerprints.size() + 1;
        int rowIndex = 0;
        int colIndex = 1;

        String[][] fluiLodCells = new String[numFingerprintCells][numFingerprintCells];

        for (Fingerprint fingerprint : fingerprints) {
            if (mapFpTest.get(fingerprint)) {
                fluiLodCells[rowIndex][colIndex] = fingerprint.getMercurySample().getSampleKey();
                ++colIndex;
            }
        }
        ++rowIndex;

        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();

        for (Fingerprint fingerprint : fingerprints) {
            colIndex = 0;
            if (mapFpTest.get(fingerprint)) {
                fluiLodCells[rowIndex][colIndex] = fingerprint.getMercurySample().getSampleKey();
                ++colIndex;
                for (Fingerprint fingerprint1 : fingerprints) {
                    if (mapFpTest.get(fingerprint1)) {
                        double lodScore = concordanceCalculator.calculateLodScore(fingerprint, fingerprint1);
                        fluiLodCells[rowIndex][colIndex] = Double.toString(lodScore);
                        ++colIndex;
                    }
                }
                ++rowIndex;
            }
        }
        concordanceCalculator.done();

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
                        cell.setCellValue(Double.parseDouble(fluiLodCells[i][j]));
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

    /**
     * Build list of fingerprints from mercury samples
     *
     * @param mapSmidToMercurySample map linking sm-ids with mercury sample
     * @return unique list of fingerprints
     */
    public List<Fingerprint> findFingerprints(Map<String, MercurySample> mapSmidToMercurySample) {
        List<Fingerprint> fingerprints = new ArrayList<>();
        Map<String, String> mapSmidToQueriedLsid = new HashMap<>();
        List<String> lsids = new ArrayList<>();
        for (String id : mapSmidToMercurySample.keySet()) {
            String lsid = BSPSampleSearchServiceStub.LSID_PREFIX + id.substring(3);
            lsids.add(lsid);
            mapSmidToQueriedLsid.put(id, lsid);
        }

        // Make list of samples for which to query BSP
        List<String> bspLsids = new ArrayList<>();
        Map<String, String> mapSmidToFpLsid =
                FingerprintResource.getSmidToFpLsidMap(mapSmidToQueriedLsid, mapSmidToMercurySample, bspLsids);

        //Query BSP for FP aliquots for given (sequencing) aliquots and map queried LSIDs to MercurySamples that may have fingerprints
        Multimap<String, MercurySample> mapLsidToFpSamples =
                FingerprintResource.getMercurySampleMultimap(lsids, mapSmidToMercurySample, bspLsids, mapSmidToFpLsid,
                        bspGetExportedSamplesFromAliquots, mercurySampleDao);

        Collection<MercurySample> mercurySamples = mapLsidToFpSamples.values();
        for (MercurySample mercurySample : mercurySamples) {
            fingerprints.addAll(mercurySample.getFingerprints());
        }

        Map<String, SampleData> mapIdToData =
                sampleDataFetcher.fetchSampleDataForSamples(mercurySamples,
                        BSPSampleSearchColumn.PARTICIPANT_ID, BSPSampleSearchColumn.ROOT_SAMPLE);
        for (MercurySample mercurySample : mercurySamples) {
            mercurySample.setSampleData(mapIdToData.get(mercurySample.getSampleKey()));
        }

        return fingerprints.stream()
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get mercury samples for PT-ID search
     *
     * @param mapSmidToMercurySample map sm-ids to mercury sample
     * @param participantId          PT-ID user search term
     * @param mercurySampleDao       access mercury sample using sm-id
     * @return sm-ids mapped with mercury samples
     */
    public Map<String, MercurySample> getPtIdMercurySamples(Map<String, MercurySample> mapSmidToMercurySample,
                                                            String participantId,
                                                            MercurySampleDao mercurySampleDao) {
        if (StringUtils.isNotBlank(participantId)) {
            SearchItem searchItem =
                    new SearchItem("Participant ID", "IN", Arrays.asList((participantId.toUpperCase().split("\\s+"))));
            List<Object> ptMercurySamples = LabMetricSearchDefinition.runBspSearch(searchItem);
            List<String> mercurySamples = ptMercurySamples.stream()
                    .map(object -> Objects.toString(object, null))
                    .collect(Collectors.toList());
            mapSmidToMercurySample =
                    mercurySampleDao.findMapIdToMercurySample(mercurySamples);
        }
        return mapSmidToMercurySample;
    }

}

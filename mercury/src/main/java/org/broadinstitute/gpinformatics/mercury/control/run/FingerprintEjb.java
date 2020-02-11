package org.broadinstitute.gpinformatics.mercury.control.run;

import com.google.common.collect.Multimap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Color;
import org.apache.poi.ss.usermodel.ComparisonOperator;
import org.apache.poi.ss.usermodel.ConditionalFormattingRule;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.PatternFormatting;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.SheetConditionalFormatting;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFColor;
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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Stateful
@RequestScoped
public class FingerprintEjb {
    private static final Log log = LogFactory.getLog(FingerprintEjb.class);

    @Inject
    private MercurySampleDao mercurySampleDao;

    @Inject
    private BSPGetExportedSamplesFromAliquots bspGetExportedSamplesFromAliquots;

    @Inject
    private SampleDataFetcher sampleDataFetcher;

    @Inject
    private ConcordanceCalculator concordanceCalculator;

    /**
     * Builds a pairwise matrix to display calculated lod scores for each combination of samples
     *
     * @param fingerprints list of fingerprints to compare in matrix
     * @param platforms    fingerprinting platforms used to compare
     * @return formatted workbook containing pairwise matrix of samples and lod scores
     */
    public Workbook makeMatrix(List<Fingerprint> fingerprints, Set<Fingerprint.Platform> platforms) {
        Map<String, Object[][]> sheets = new HashMap<>();

        if (platforms == null) {
            platforms = EnumSet.allOf(Fingerprint.Platform.class);
        }

        List<Fingerprint> filteredFingerprints = new ArrayList<>();
        for (Fingerprint fingerprint : fingerprints) {
            if (fingerprint.getGender() != null && (platforms.contains(fingerprint.getPlatform()))
                && fingerprint.getDisposition() == Fingerprint.Disposition.PASS) {
                filteredFingerprints.add(fingerprint);
            }
        }

        int numFingerprintCells = fingerprints.size() + 2;
        int rowIndex = 2;
        int colIndex = 2;

        String[][] fluiLodCells = new String[numFingerprintCells][numFingerprintCells];

        for (Fingerprint fingerprint : filteredFingerprints) {
            fluiLodCells[0][colIndex] = fingerprint.getMercurySample().getSampleData().getPatientId();
            fluiLodCells[1][colIndex] = fingerprint.getMercurySample().getSampleKey();
            ++colIndex;
        }

        ConcordanceCalculator concordanceCalculator = new ConcordanceCalculator();
        List<Triple<String, String, Double>> scores = concordanceCalculator.calculateLodScores(filteredFingerprints,
                filteredFingerprints, ConcordanceCalculator.Comparison.MATRIX);
        DecimalFormat df = new DecimalFormat("##.##");
        df.setRoundingMode(RoundingMode.HALF_UP);

        int lodScoreIndex = 0;
        for (Fingerprint fingerprint : filteredFingerprints) {
            colIndex = 2;
            fluiLodCells[rowIndex][0] = fingerprint.getMercurySample().getSampleData().getPatientId();
            fluiLodCells[rowIndex][1] = fingerprint.getMercurySample().getSampleKey();
            for (Fingerprint fingerprint1 : filteredFingerprints) {
                String colSmId = fluiLodCells[1][colIndex];
                String rowSmID = fluiLodCells[rowIndex][1];
                boolean isColSample = scores.get(lodScoreIndex).getLeft().equals(colSmId);
                boolean isRowSample = scores.get(lodScoreIndex).getMiddle().equals(rowSmID);
                if (isColSample && isRowSample) {
                    double lodScore = scores.get(lodScoreIndex).getRight();
                    fluiLodCells[rowIndex][colIndex] = String.valueOf(df.format(lodScore));
                    ++colIndex;
                    ++lodScoreIndex;
                } else {
                    throw new RuntimeException(
                            "Couldn't calculate lod score for " + scores.get(lodScoreIndex).getMiddle()
                            + " , " + scores.get(lodScoreIndex).getRight());
                }
            }
            ++rowIndex;
        }

        String[] sheetNames = {"Fluidigm Matrix"};
        sheets.put(sheetNames[0], fluiLodCells);
        Workbook workbook = SpreadsheetCreator.createSpreadsheet(sheets, SpreadsheetCreator.Type.XLSX);
        Sheet sheet = workbook.getSheet("Fluidigm Matrix");

        Row row;
        Cell cell;

        for (int i = 2; i < numFingerprintCells; i++) {
            row = sheet.createRow(i);
            for (int j = 0; j < numFingerprintCells; j++) {
                cell = row.createCell(j);
                if (fluiLodCells[i][j] != null) {
                    if (j == 0 || j == 1) {
                        cell.setCellValue(fluiLodCells[i][j]);
                    } else {
                        cell.setCellValue(Double.parseDouble(fluiLodCells[i][j]));
                    }
                }
            }
        }

        SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();

        ConditionalFormattingRule positiveRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.GT, "3");
        PatternFormatting positiveFill = positiveRule.createPatternFormatting();
        positiveFill.setFillBackgroundColor(IndexedColors.LIGHT_GREEN.index);
        positiveFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        ConditionalFormattingRule negativeRule = sheetCF.createConditionalFormattingRule(ComparisonOperator.LT, "-3");
        PatternFormatting negativeFill = negativeRule.createPatternFormatting();
        Color roseQuartz = new XSSFColor(java.awt.Color.decode("#FABEC0"));
        negativeFill.setFillBackgroundColor(roseQuartz);
        negativeFill.setFillPattern(PatternFormatting.SOLID_FOREGROUND);

        ConditionalFormattingRule[] cfRules = new ConditionalFormattingRule[]{positiveRule, negativeRule};

        CellRangeAddress[] regions = new CellRangeAddress[]{
                new CellRangeAddress(2, numFingerprintCells, 2, numFingerprintCells)};

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
            List<String> partIds = Arrays.asList((participantId.toUpperCase().split("\\s+")));
            List<String> ptIds = partIds.stream()
                    .filter(partId -> partId.startsWith("PT-") )
                    .collect(Collectors.toList());
            if (!ptIds.isEmpty()) {
                SearchItem searchItem =
                        new SearchItem("Participant ID", "IN", ptIds);
                List<Object> ptMercurySamples = LabMetricSearchDefinition.runBspSearch(searchItem);
                List<String> mercurySamples = ptMercurySamples.stream()
                        .map(object -> Objects.toString(object, null))
                        .collect(Collectors.toList());
                mapSmidToMercurySample =
                        mercurySampleDao.findMapIdToMercurySample(mercurySamples);
            }
        }
        return mapSmidToMercurySample;
    }

    /**
     * Calculate lod score using oldest fluidigm fingerprint if available
     *
     * @param fingerprints used to determine anchor fp and compare against to find lod score
     * @return map of fingerprints and their lod scores "N/A" if fail, "Anchor FP" if oldest fluidigm fingerprint(general array if no fluidigm),
     * calculated lod score compared to Anchor FP
     */
    public Map<Fingerprint, String> findLodScore(List<Fingerprint> fingerprints) {
        Map<Fingerprint, String> lodScoreMap = new HashMap<>();
        List<Fingerprint> expected = new ArrayList<>();
        List<Fingerprint> observed = new ArrayList<>();

        findAnchor(fingerprints, lodScoreMap, expected, observed);

        DecimalFormat df = new DecimalFormat("##.####");
        df.setRoundingMode(RoundingMode.HALF_UP);

        List<Triple<String, String, Double>> scores = concordanceCalculator
                .calculateLodScores(expected, observed, ConcordanceCalculator.Comparison.ONE_TO_ONE);

        for (Fingerprint fingerprint : fingerprints) {
            for (Triple<String, String, Double> triple : scores) {
                if (fingerprint.getMercurySample().getSampleKey().equals(triple.getLeft())) {
                    if (!lodScoreMap.containsKey(fingerprint)) {
                        lodScoreMap.put(fingerprint, df.format(triple.getRight()));
                    }
                }
            }
        }
        return lodScoreMap;
    }

    public void findAnchor(List<Fingerprint> fingerprints, Map<Fingerprint, String> lodScoreMap,
                            List<Fingerprint> expected, List<Fingerprint> observed) {
        String lodScoreStr;
        for (Fingerprint fingerprint : fingerprints) {
            Optional<Fingerprint> oldFluidigmFp = fingerprints.stream()
                    .filter(fp -> fp.getMercurySample().getSampleData().getPatientId()
                            .equals(fingerprint.getMercurySample().getSampleData().getPatientId()))
                    .filter(fp -> fp.getPlatform() == Fingerprint.Platform.FLUIDIGM
                                  && fp.getDisposition() == Fingerprint.Disposition.PASS)
                    .min(Comparator.comparing(Fingerprint::getDateGenerated));

            if (!oldFluidigmFp.isPresent()) {
                oldFluidigmFp = fingerprints.stream()
                        .filter(fp -> fp.getMercurySample().getSampleData().getPatientId()
                                .equals(fingerprint.getMercurySample().getSampleData().getPatientId()))
                        .filter(fp -> fp.getPlatform() == Fingerprint.Platform.GENERAL_ARRAY
                                      && fp.getDisposition() == Fingerprint.Disposition.PASS)
                        .min(Comparator.comparing(Fingerprint::getDateGenerated));
            }

            Fingerprint oldFingerprint;
            if (oldFluidigmFp.isPresent()) {
                oldFingerprint = oldFluidigmFp.get();
                if (isAnchor(fingerprint, oldFingerprint)
                    ) {
                    lodScoreStr = "Anchor FP";
                    lodScoreMap.put(fingerprint, lodScoreStr);
                } else if (oldFingerprint.getGender() != null
                           && oldFingerprint.getDisposition() == Fingerprint.Disposition.PASS
                           && fingerprint.getDisposition() == Fingerprint.Disposition.PASS
                           && fingerprint.getGender() != null) {
                    expected.add(fingerprint);
                    observed.add(oldFingerprint);
                } else {
                    lodScoreStr = "N/A";
                    lodScoreMap.put(fingerprint, lodScoreStr);
                }
            }
        }
    }

    private boolean isAnchor(Fingerprint fingerprint, Fingerprint oldFingerprint) {
        boolean anchor = false;
        boolean isBefore = (fingerprint.getDateGenerated().before(oldFingerprint.getDateGenerated()) ||
                            oldFingerprint.getDateGenerated().equals(fingerprint.getDateGenerated()));
        boolean samePassingSample = oldFingerprint.getMercurySample().getSampleKey()
                            .equals(fingerprint.getMercurySample().getSampleKey()) &&
                                    fingerprint.getDisposition() == Fingerprint.Disposition.PASS;

        boolean isFluigidm = isBefore && samePassingSample && fingerprint.getPlatform() == Fingerprint.Platform.FLUIDIGM;
        boolean isGenArray = samePassingSample && isBefore;

        if (oldFingerprint.getPlatform() == Fingerprint.Platform.GENERAL_ARRAY){
            anchor = isGenArray;
        }else if (oldFingerprint.getPlatform() == Fingerprint.Platform.FLUIDIGM){
            anchor = isFluigidm;
        }
        return anchor;
    }
}

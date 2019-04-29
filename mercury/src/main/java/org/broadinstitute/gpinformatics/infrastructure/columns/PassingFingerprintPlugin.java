package org.broadinstitute.gpinformatics.infrastructure.columns;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.broadinstitute.gpinformatics.athena.presentation.Displayable;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPGetExportedSamplesFromAliquots;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.exports.IsExported;
import org.broadinstitute.gpinformatics.infrastructure.common.ServiceAccessUtility;
import org.broadinstitute.gpinformatics.infrastructure.search.SearchContext;
import org.broadinstitute.gpinformatics.mercury.boundary.run.FingerprintResource;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.entity.run.Fingerprint;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Configurable List plugin that adds columns for passing fingerprints.
 */
public class PassingFingerprintPlugin implements ListPlugin {

    private enum PluginColumn implements Displayable {
        PASSING("Passing FP"),
        DATE("FP Date"),
        PLATFORM("FP Platform"),
        ALIQUOT_ID("FP Aliquot");

        private final String displayName;
        private final ConfigurableList.Header header;

        PluginColumn(String displayName) {
            this.displayName = displayName;
            header = new ConfigurableList.Header(displayName, displayName, null);
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        public ConfigurableList.Header getHeader() {
            return header;
        }
    }

    @Override
    public List<ConfigurableList.Row> getData(
            List<?> entityList,
            ConfigurableList.HeaderGroup headerGroup,
            @Nonnull SearchContext context) {
        for (PluginColumn value : PluginColumn.values()) {
            headerGroup.addHeader(value.getHeader());
        }
        EnumSet<Fingerprint.Platform> initialPlatforms = EnumSet.of(
                Fingerprint.Platform.FLUIDIGM, Fingerprint.Platform.GENERAL_ARRAY, Fingerprint.Platform.FAT_PANDA);

        @SuppressWarnings("unchecked")
        List<MercurySample> sampleList = (List<MercurySample>) entityList;
        List<ConfigurableList.Row> fingerprintRows = new ArrayList<>();
        BSPGetExportedSamplesFromAliquots samplesFromAliquots = ServiceAccessUtility.getBean(
                BSPGetExportedSamplesFromAliquots.class);
        MercurySampleDao mercurySampleDao = ServiceAccessUtility.getBean(
                MercurySampleDao.class);
        BSPSampleDataFetcher bspSampleDataFetcher = ServiceAccessUtility.getBean(BSPSampleDataFetcher.class);
        Map<String, BspSampleData> mapIdToSampleData = bspSampleDataFetcher.fetchSampleData(
                sampleList.stream().map(MercurySample::getSampleKey).collect(Collectors.toList()),
                BSPSampleSearchColumn.LSID);

        Comparator<Fingerprint> comparator = (o1, o2) -> new CompareToBuilder().
                append(o1.getPlatform().getPrecedenceForInitial(), o2.getPlatform().getPrecedenceForInitial()).
                append(o2.getDateGenerated(), o1.getDateGenerated()).
                build();
        for( MercurySample sample : sampleList ) {
            Set<Fingerprint> rowFingerprints = sample.getFingerprints();
            Optional<Fingerprint> optionalFingerprint;
            if (rowFingerprints.isEmpty()) {
                // Get exports from BSP to GAP
                BspSampleData bspSampleData = mapIdToSampleData.get(sample.getSampleKey());
                if (bspSampleData == null) {
                    continue;
                }
                List<BSPGetExportedSamplesFromAliquots.ExportedSample> exportedSamples =
                        samplesFromAliquots.getExportedSamplesFromAliquots(Collections.singleton(bspSampleData.getSampleLsid()),
                                IsExported.ExternalSystem.GAP);
                List<String> sampleKeys = new ArrayList<>();
                for (BSPGetExportedSamplesFromAliquots.ExportedSample exportedSample : exportedSamples) {
                    sampleKeys.add(FingerprintResource.getSmIdFromLsid(exportedSample.getExportedLsid()));
                }

                // Get most recent passing fingerprint from each export
                Map<String, MercurySample> mapIdToMercurySample = mercurySampleDao.findMapIdToMercurySample(sampleKeys);
                List<Fingerprint> fingerprints = new ArrayList<>();
                for (Map.Entry<String, MercurySample> idMercurySampleEntry : mapIdToMercurySample.entrySet()) {
                    MercurySample mercurySample = idMercurySampleEntry.getValue();
                    if (mercurySample != null) {
                        mercurySample.getFingerprints().stream().
                                filter(fingerprint -> fingerprint.getDisposition() == Fingerprint.Disposition.PASS &&
                                        initialPlatforms.contains(fingerprint.getPlatform())).
                                max(comparator).
                                ifPresent(fingerprints::add);
                    }
                }
                optionalFingerprint = fingerprints.stream().max(comparator);
            } else {
                optionalFingerprint = rowFingerprints.stream().
                        filter(fingerprint -> fingerprint.getDisposition() == Fingerprint.Disposition.PASS &&
                                initialPlatforms.contains(fingerprint.getPlatform())).
                        max(comparator);
            }

            // Add row to table
            ConfigurableList.Row row = new ConfigurableList.Row(sample.getSampleKey());
            if (optionalFingerprint.isPresent()) {
                Fingerprint fingerprint = optionalFingerprint.get();
                row.addCell(new ConfigurableList.Cell(PluginColumn.PASSING.getHeader(), "Y", "Y"));
                row.addCell(new ConfigurableList.Cell(PluginColumn.DATE.getHeader(),
                        fingerprint.getDateGenerated(), ColumnValueType.DATE.format(fingerprint.getDateGenerated(), "")));
                row.addCell(new ConfigurableList.Cell(PluginColumn.PLATFORM.getHeader(),
                        fingerprint.getPlatform(), fingerprint.getPlatform().name()));
                row.addCell(new ConfigurableList.Cell(PluginColumn.ALIQUOT_ID.getHeader(),
                        fingerprint.getMercurySample().getSampleKey(), fingerprint.getMercurySample().getSampleKey()));
            } else {
                row.addCell(new ConfigurableList.Cell(PluginColumn.PASSING.getHeader(), "N", "N"));
            }
            fingerprintRows.add(row);
        }
        return fingerprintRows;
    }

    @Override
    public ConfigurableList.ResultList getNestedTableData(
            Object entity,
            ColumnTabulation columnTabulation,
            @Nonnull SearchContext context) {
        throw new UnsupportedOperationException("Method getNestedTableData not implemented in "
                + getClass().getSimpleName() );
    }
}

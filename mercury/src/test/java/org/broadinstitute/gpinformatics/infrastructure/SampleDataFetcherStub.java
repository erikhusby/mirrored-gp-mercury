package org.broadinstitute.gpinformatics.infrastructure;

import com.google.common.collect.ImmutableMap;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BspSampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.GetSampleDetails;

import javax.annotation.Nonnull;
import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SampleDataFetcherStub {

    @Alternative
    public static class EverythingYouAskForYouGetAndItsHuman extends SampleDataFetcher {

        @Override
        public SampleData fetchSampleData(String sampleName) {
            SampleData sampleData = new BspSampleData(ImmutableMap.of(
                    BSPSampleSearchColumn.SAMPLE_ID, sampleName,
                    BSPSampleSearchColumn.PARTICIPANT_ID, "2",
                    BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID, "2",
                    BSPSampleSearchColumn.MATERIAL_TYPE, "DNA:DNA Genomic",
                    BSPSampleSearchColumn.SPECIES, "Homo : Homo sapiens"));
            return sampleData;
        }

        @Override
        public Map<String, SampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {
            Map<String, SampleData> result = new HashMap<>();
            for (String sampleName : sampleNames) {
                result.put(sampleName, fetchSampleData(sampleName));
            }
            return result;
        }

        @Override
        public void fetchFFPEDerived(@Nonnull Collection<SampleData> sampleDataCollection) {
            super.fetchFFPEDerived(sampleDataCollection);
        }

        @Override
        public String getStockIdForAliquotId(@Nonnull String aliquotId) {
            return super.getStockIdForAliquotId(aliquotId);
        }

        @Override
        public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
            return super.getStockIdByAliquotId(aliquotIds);
        }

        @Override
        public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {
            return super.fetchSampleDetailsByBarcode(barcodes);
        }
    }
}

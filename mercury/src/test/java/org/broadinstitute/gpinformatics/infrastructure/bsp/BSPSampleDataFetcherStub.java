package org.broadinstitute.gpinformatics.infrastructure.bsp;

import javax.annotation.Nonnull;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Alternative
@RequestScoped
public class BSPSampleDataFetcherStub extends BSPSampleDataFetcher {

    public BSPSampleDataFetcherStub(){}

    private Map<String, BspSampleData> sampleDataBySampleId = new HashMap<>();
    private Map<String, String> samplePlasticBySampleId = new HashMap<>();


    /**
     * Clears any cached test sample data and replaces with new testing data <br/>
     * Typically managed in TestNg's @BeforeMethod event
     */
    public void stubFetchSampleData(String sampleId, BspSampleData sampleData, String samplePlastic) {
        sampleDataBySampleId.put(sampleId, sampleData);
        samplePlasticBySampleId.put(sampleId, samplePlastic);
    }

    public void clearStubFetchSampleData() {
        sampleDataBySampleId.clear();
        samplePlasticBySampleId.clear();
    }

    @Override
    public BspSampleData fetchSingleSampleFromBSP(String sampleName) {
        return super.fetchSingleSampleFromBSP(sampleName);
    }

    @Override
    public Map<String, BspSampleData> fetchSampleData(@Nonnull Collection<String> sampleNames,
                                                      BSPSampleSearchColumn... bspSampleSearchColumns) {
        return super.fetchSampleData(sampleNames, bspSampleSearchColumns);
    }

    @Override
    public Map<String, BspSampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {
        Map<String, BspSampleData> result = new HashMap<>();
        for (String sampleName : sampleNames) {
            BspSampleData sampleData = sampleDataBySampleId.get(sampleName);
            if (sampleData != null) {
                result.put(sampleName, sampleData);
            }
        }
        return result;
    }

    @Override
    public void fetchFFPEDerived(@Nonnull Collection<BspSampleData> bspSampleDatas) {
        super.fetchFFPEDerived(bspSampleDatas);
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

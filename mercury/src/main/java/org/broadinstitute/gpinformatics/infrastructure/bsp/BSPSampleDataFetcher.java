package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Wrapper around {@link BSPSampleSearchService} that
 * does a bit more object-ifying and type-safety.
 */
public class BSPSampleDataFetcher extends BSPJerseyClient implements Serializable {
    static final long serialVersionUID = -1432207534876411738L;
// Many versions of this service written only for tests are considered as options by IntelliJ.
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    BSPSampleSearchService service;
    static final String WS_FFPE_DERIVED = "sample/ffpeDerived";
    static final String WS_DETAILS = "sample/getdetails";
// Used for mapping Matrix barcodes to Sample short barcodes, forces xml output format.
    static final String WS_SAMPLE_DETAILS = "sample/getsampledetails?format=xml";

    public BSPSampleDataFetcher() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param service The sample search service to use.
     * @param bspConfig The config object - Thi sis only nullable for tests that don't need to deal with BSP directly.
     */
    public BSPSampleDataFetcher(@Nonnull BSPSampleSearchService service, @Nullable BSPConfig bspConfig) {
        super(bspConfig);
        this.service = service;
    }

    /**
     * New one up using the given service.
     *
     * @param service The search service object.
     */
    public BSPSampleDataFetcher(@Nonnull BSPSampleSearchService service) {
        this(service, null);
    }

    /**
     * Fetch the data from BSP for the given sample.
     *
     * @param sampleName The sample name.
     *
     * @return The sample DTO that was fetched.
     */
    public BspSampleData fetchSingleSampleFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        } else {
            Map<String, BspSampleData> sampleNameToDTO = fetchSampleData(Collections.singleton(sampleName));

            if (sampleNameToDTO.isEmpty()) {
                return null;
            } else if (sampleNameToDTO.size() > 1) {
                throw new RuntimeException("Found " + sampleNameToDTO.size() + " possible matches in bsp.");
            } else {
                return sampleNameToDTO.entrySet().iterator().next().getValue();
            }
        }
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames            The sample names, which can be short barcodes such as SM-4FHTK,
     *                               or bare ids such as 4FHTK.
     * @param bspSampleSearchColumns Array of columns to return from BSP. The SAMPLE_ID column is always returned
     *                               no matter what values is provided for bspSampleSearchColumns as it is the key
     *                               to the map returned.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BspSampleData> fetchSampleData(@Nonnull Collection<String> sampleNames,
                                                     BSPSampleSearchColumn... bspSampleSearchColumns) {
        Collection<String> filteredSampleNames = new HashSet<>();
        for (String sampleName : sampleNames) {
            if (BSPUtil.isInBspFormat(sampleName)) {
                filteredSampleNames.add(sampleName);
            }
        }

        if (filteredSampleNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Set<BSPSampleSearchColumn> searchColumns =
                new HashSet<>(Arrays.asList(bspSampleSearchColumns));
        searchColumns.add(BSPSampleSearchColumn.SAMPLE_ID);
        Map<String, BspSampleData> sampleNameToDTO = new HashMap<>();
        List<Map<BSPSampleSearchColumn, String>> results = service.runSampleSearch(filteredSampleNames,
                searchColumns.toArray(new BSPSampleSearchColumn[searchColumns.size()]));
        for (Map<BSPSampleSearchColumn, String> result : results) {
            BspSampleData bspSampleData = new BspSampleData(result);
            sampleNameToDTO.put(bspSampleData.getSampleId(), bspSampleData);
        }

        return sampleNameToDTO;
    }

    /**
     * Fetch the data from bsp for multiple samples.
     *
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BspSampleData> fetchSampleData(@Nonnull Collection<String> sampleNames) {
        return fetchSampleData(sampleNames, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
    }

    public static Collection<BspSampleData> convertToBspSampleDataCollection(
            Collection<? extends SampleData> sampleDatas) {
        Collection<BspSampleData> result = new ArrayList<>();
        for (SampleData sampleData : sampleDatas) {
            result.add((BspSampleData) sampleData);
        }
        return result;
    }

    /**
     * There is much copying and pasting of code from BSPSampleSearchServiceImpl into here -- a refactoring is needed.
     *
     * @param bspSampleDatas BSP DTOs whose sampleID field will be referenced for the barcode value, and which will
     *                      be filled with the ffpeDerived value returned by the FFPE webservice
     */
    public void fetchFFPEDerived(@Nonnull Collection<BspSampleData> bspSampleDatas) {
        if (bspSampleDatas.isEmpty()) {
            return;
        }

        final Map<String, BspSampleData> barcodeToDTOMap = new HashMap<>();
        for (BspSampleData bspSampleData : bspSampleDatas) {
            barcodeToDTOMap.put(bspSampleData.getSampleId(), bspSampleData);
        }

        // Check to see if BSP is supported before trying to get data.
        if (AbstractConfig.isSupported(getBspConfig())) {
            String urlString = getUrl(WS_FFPE_DERIVED);
            String queryString = makeQueryString("barcodes", barcodeToDTOMap.keySet());
            final int SAMPLE_BARCODE = 0;
            final int FFPE = 1;

            post(urlString, queryString, ExtraTab.FALSE, new AbstractJerseyClientService.PostCallback() {
                @Override
                public void callback(String[] bspOutput) {
                    BspSampleData bspSampleData = barcodeToDTOMap.get(bspOutput[SAMPLE_BARCODE]);
                    if (bspSampleData == null) {
                        throw new RuntimeException("Unrecognized return barcode: " + bspOutput[SAMPLE_BARCODE]);
                    }

                    bspSampleData.setFfpeStatus(Boolean.valueOf(bspOutput[FFPE]));
                }
            });
        }
    }

    public void fetchSamplePlastic(@Nonnull Collection<BspSampleData> bspSampleDatas) {
        if (bspSampleDatas.isEmpty()) {
            return;
        }

        final Map<String, BspSampleData> lsidToDTOMap = new HashMap<>();
        for (BspSampleData bspSampleData : bspSampleDatas) {
            lsidToDTOMap.put(bspSampleData.getSampleLsid(), bspSampleData);
        }

        String urlString = getUrl(WS_DETAILS);
        String queryString = makeQueryString("sample_lsid", lsidToDTOMap.keySet());
        final int LSID = 1;
        final int PLASTIC_BARCODE = 16;
        post(urlString, queryString, ExtraTab.FALSE, new AbstractJerseyClientService.PostCallback() {
            @Override
            public void callback(String[] bspOutput) {
                BspSampleData bspSampleData = lsidToDTOMap.get(bspOutput[LSID]);
                if (bspSampleData == null) {
                    throw new RuntimeException("Unrecognized return lsid: " + bspOutput[LSID]);
                }
                bspSampleData.addPlastic(bspOutput[PLASTIC_BARCODE]);
            }
        });

    }

    /**
     * Given an aliquot ID, return its stock sample ID. The aliquot ID may, but is not required to, have a "SM-" prefix.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        Map<String, String> stockIdByAliquotId = getStockIdByAliquotId(Collections.singletonList(aliquotId));
        if (!BSPUtil.isInBspFormat(aliquotId)) {
            aliquotId = "SM-" + aliquotId;
        }
        return stockIdByAliquotId.get(aliquotId);
    }

    /**
     * Given a list of aliquot IDs, return a map of aliquot IDs to stock IDs.
     */
    public Map<String, String> getStockIdByAliquotId(Collection<String> aliquotIds) {
        Map<String, String> stockIdByAliquotId = new HashMap<>();
        if (!aliquotIds.isEmpty()) {
            List<Map<BSPSampleSearchColumn, String>> results = service.runSampleSearch(aliquotIds,
                    BSPSampleSearchColumn.SAMPLE_ID, BSPSampleSearchColumn.STOCK_SAMPLE);
            for (Map<BSPSampleSearchColumn, String> result : results) {
                stockIdByAliquotId.put(result.get(BSPSampleSearchColumn.SAMPLE_ID),
                        result.get(BSPSampleSearchColumn.STOCK_SAMPLE));
            }
        }
        return stockIdByAliquotId;
    }

    /**
     * Returns a map of barcode to SampleDetails object for each input barcode, which can be a manufacturer barcode
     * or SM-id barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByBarcode(@Nonnull Collection<String> barcodes) {
        String urlString = getUrl(WS_SAMPLE_DETAILS);

        Map<String, GetSampleDetails.SampleInfo> map = new HashMap<>();
        WebResource resource = getJerseyClient().resource(urlString);
        // Use POST, rather than GET, to allow large number of barcodes without hitting 8K limit on URL.
        MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("barcodes", StringUtils.join(barcodes, ","));
        GetSampleDetails.Details details = resource.accept(MediaType.TEXT_XML).post(
                new GenericType<GetSampleDetails.Details>() {}, formData);

        // Fills in the map values using SampleDetails that were found in BSP.
        if (details.getSampleDetails().getSampleInfo() != null) {
            for (GetSampleDetails.SampleInfo sampleInfo : details.getSampleDetails().getSampleInfo()) {
                // BarcodedTube such as cryovial will not have mfg barcode but will have an SM-id barcode.
                if (StringUtils.isNotBlank(sampleInfo.getManufacturerBarcode()) &&
                    barcodes.contains(sampleInfo.getManufacturerBarcode())) {
                    map.put(sampleInfo.getManufacturerBarcode(), sampleInfo);
                } else if (StringUtils.isNotBlank(sampleInfo.getSampleId()) &&
                           barcodes.contains(sampleInfo.getSampleId())) {
                    map.put(sampleInfo.getSampleId(), sampleInfo);
                }
            }
        }

        return map;
    }
}

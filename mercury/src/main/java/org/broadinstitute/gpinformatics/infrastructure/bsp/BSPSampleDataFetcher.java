package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import org.broadinstitute.gpinformatics.infrastructure.deployment.AbstractConfig;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Wrapper around {@link BSPSampleSearchService} that
 * does a bit more object-ifying and type-safety.
 */
public class BSPSampleDataFetcher extends BSPJerseyClient {

    private static final long serialVersionUID = -1432207534876411738L;

    // Many versions of this service written only for tests are considered as options by IntelliJ.
    @SuppressWarnings("CdiInjectionPointsInspection")
    @Inject
    BSPSampleSearchService service;

    private static final String WS_FFPE_DERIVED = "sample/ffpeDerived";
    private static final String WS_DETAILS = "sample/getdetails";
    // Used for mapping Matrix barcodes to Sample short barcodes, forces xml output format.
    private static final String WS_SAMPLE_DETAILS = "sample/getsampledetails?format=xml";

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
    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        } else {
            Map<String, BSPSampleDTO> sampleNameToDTO = fetchSamplesFromBSP(Collections.singleton(sampleName));

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
     * @param sampleNames The sample names, which can be short barcodes such as SM-4FHTK,
     *                    or bare ids such as 4FHTK.
     *
     * @return Mapping of sample id to its bsp data
     */
    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
        if (sampleNames.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, BSPSampleDTO> sampleNameToDTO = new HashMap<>();
        List<Map<BSPSampleSearchColumn, String>> results =
                service.runSampleSearch(sampleNames, BSPSampleSearchColumn.PDO_SEARCH_COLUMNS);
        for (Map<BSPSampleSearchColumn, String> result : results) {
            BSPSampleDTO bspDTO = new BSPSampleDTO(result);
            sampleNameToDTO.put(bspDTO.getSampleId(), bspDTO);
        }

        return sampleNameToDTO;
    }

    /**
     * There is much copying and pasting of code from BSPSampleSearchServiceImpl into here -- a refactoring is needed.
     *
     * @param bspSampleDTOs BSP DTOs whose sampleID field will be referenced for the barcode value, and which will
     *                      be filled with the ffpeDerived value returned by the FFPE webservice
     */
    public void fetchFFPEDerived(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {
        if (bspSampleDTOs.isEmpty()) {
            return;
        }

        final Map<String, BSPSampleDTO> barcodeToDTOMap = new HashMap<>();
        for (BSPSampleDTO bspSampleDTO : bspSampleDTOs) {
            barcodeToDTOMap.put(bspSampleDTO.getSampleId(), bspSampleDTO);
        }

        // Check to see if BSP is supported before trying to get data.
        if (AbstractConfig.isSupported(getBspConfig())) {
            String urlString = getUrl(WS_FFPE_DERIVED);
            String queryString = makeQueryString("barcodes", barcodeToDTOMap.keySet());
            final int SAMPLE_BARCODE = 0;
            final int FFPE = 1;

            post(urlString, queryString, ExtraTab.FALSE, new PostCallback() {
                @Override
                public void callback(String[] bspOutput) {
                    BSPSampleDTO bspSampleDTO = barcodeToDTOMap.get(bspOutput[SAMPLE_BARCODE]);
                    if (bspSampleDTO == null) {
                        throw new RuntimeException("Unrecognized return barcode: " + bspOutput[SAMPLE_BARCODE]);
                    }

                    bspSampleDTO.setFfpeStatus(Boolean.valueOf(bspOutput[FFPE]));
                }
            });
        }
    }

    public void fetchSamplePlastic(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {
        if (bspSampleDTOs.isEmpty()) {
            return;
        }

        final Map<String, BSPSampleDTO> lsidToDTOMap = new HashMap<>();
        for (BSPSampleDTO bspSampleDTO : bspSampleDTOs) {
            lsidToDTOMap.put(bspSampleDTO.getSampleLsid(), bspSampleDTO);
        }

        String urlString = getUrl(WS_DETAILS);
        String queryString = makeQueryString("sample_lsid", lsidToDTOMap.keySet());
        final int LSID = 1;
        final int PLASTIC_BARCODE = 16;
        post(urlString, queryString, ExtraTab.FALSE, new PostCallback() {
            @Override
            public void callback(String[] bspOutput) {
                BSPSampleDTO bspSampleDTO = lsidToDTOMap.get(bspOutput[LSID]);
                if (bspSampleDTO == null) {
                    throw new RuntimeException("Unrecognized return lsid: " + bspOutput[LSID]);
                }
                bspSampleDTO.addPlastic(bspOutput[PLASTIC_BARCODE]);
            }
        });

    }

    /**
     * Given an aliquot ID, return its stock sample ID.
     */
    public String getStockIdForAliquotId(@Nonnull String aliquotId) {
        List<Map<BSPSampleSearchColumn, String>> results =
                service.runSampleSearch(Collections.singletonList(aliquotId), BSPSampleSearchColumn.STOCK_SAMPLE);
        if (results.isEmpty()) {
            return null;
        }

        return results.get(0).get(BSPSampleSearchColumn.STOCK_SAMPLE);
    }

    /**
     * Return a Map of manufacturer barcodes to the SampleDetails object for each input barcode.
     */
    public Map<String, GetSampleDetails.SampleInfo> fetchSampleDetailsByMatrixBarcodes(@Nonnull Collection<String> matrixBarcodes) {
        String queryString = makeQueryString("barcodes", matrixBarcodes);
        String urlString = getUrl(WS_SAMPLE_DETAILS);

        Map<String, GetSampleDetails.SampleInfo> map = new HashMap<>();
        WebResource resource = getJerseyClient().resource(urlString + "&" + queryString);
        GetSampleDetails.Details
                details = resource.accept(MediaType.TEXT_XML).get(new GenericType<GetSampleDetails.Details>() {});

        // Overwrite the map values that were found in BSP with the SampleDetails objects.
        if (details.getSampleDetails().getSampleInfo() != null) {
            for (GetSampleDetails.SampleInfo sampleInfo : details.getSampleDetails().getSampleInfo()) {
                map.put(sampleInfo.getManufacturerBarcode(), sampleInfo);
            }
        }

        return map;
    }
}

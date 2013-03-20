package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.*;

/**
 * Wrapper around {@link BSPSampleSearchService} that
 * does a bit more object-ifying and type-safety.
 */
public class BSPSampleDataFetcher extends AbstractJerseyClientService {

    @Inject
    BSPSampleSearchService service;

    @Inject
    BSPConfig bspConfig;

    private final static Log logger = LogFactory.getLog(BSPSampleDataFetcher.class);

    private static final String WS_FFPE_DERIVED = "sample/ffpeDerived";

    private static final String WS_SAMPLE_DETAILS = "sample/getdetails";

    public BSPSampleDataFetcher() {
    }

    /**
     * New one up using the given service.
     *
     * @param service
     */
    public BSPSampleDataFetcher(@Nonnull BSPSampleSearchService service) {
        if (service == null) {
            throw new NullPointerException("service cannot be null.");
        }
        this.service = service;
    }

    /**
     * Fetch the data from BSP for the given sample.
     *
     * @param sampleName
     * @return
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
     * @param sampleNames
     * @return
     */
    public Map<String, BSPSampleDTO> fetchSamplesFromBSP(@Nonnull Collection<String> sampleNames) {
        if (sampleNames == null) {
            throw new NullPointerException("sampleNames cannot be null.");
        }
        if (sampleNames.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, BSPSampleDTO> sampleNameToDTO = new HashMap<String, BSPSampleDTO>();
        List<String[]> results = getBSPResponse(sampleNames);

        for (String[] result : results) {
            BSPSampleDTO bspDTO = toDTO(result);
            sampleNameToDTO.put(bspDTO.getSampleId(), bspDTO);
        }
        return sampleNameToDTO;
    }

    /**
     * Create a BSPSampleDTO based off the BSP Sample Search results.
     *
     * @param bspColumns The columns returned by BSP Sample Search web service
     * @return A populated BSPSampleDTO object
     */
    private static BSPSampleDTO toDTO(String[] bspColumns) {
        /** beware of DBFreeBSPSampleTest: if you add columns here, you'll need to add them to the mock **/
        return new BSPSampleDTO(bspColumns);
    }

    private static String getIfNotNullAndNotEmpty(String value) {
        if (value != null) {
            if (value.trim().length() > 0) {
                return value.trim();
            }
        }
        return null;
    }

    /**
     * Get the list of column names that need to be supplied to BSP for the sample search web service.
     *
     * @param sampleNames The sample names
     * @return List of the column names for the web service
     */
    private List<String[]> getBSPResponse(Collection<String> sampleNames) {
        return service.runSampleSearch(sampleNames,
            BSPSampleSearchColumn.PARTICIPANT_ID,
            BSPSampleSearchColumn.ROOT_SAMPLE,
            BSPSampleSearchColumn.STOCK_SAMPLE,
            BSPSampleSearchColumn.COLLABORATOR_SAMPLE_ID,
            BSPSampleSearchColumn.COLLECTION,
            BSPSampleSearchColumn.VOLUME,
            BSPSampleSearchColumn.CONCENTRATION,
            BSPSampleSearchColumn.SPECIES,
            BSPSampleSearchColumn.LSID,
            BSPSampleSearchColumn.COLLABORATOR_PARTICIPANT_ID,
            BSPSampleSearchColumn.MATERIAL_TYPE,
            BSPSampleSearchColumn.TOTAL_DNA,
            BSPSampleSearchColumn.SAMPLE_TYPE,
            BSPSampleSearchColumn.PRIMARY_DISEASE,
            BSPSampleSearchColumn.GENDER,
            BSPSampleSearchColumn.STOCK_TYPE,
            BSPSampleSearchColumn.FINGERPRINT,
            BSPSampleSearchColumn.CONTAINER_ID,
            BSPSampleSearchColumn.SAMPLE_ID,
            BSPSampleSearchColumn.COLLABORATOR_NAME,
            BSPSampleSearchColumn.ETHNICITY,
            BSPSampleSearchColumn.RACE,
            BSPSampleSearchColumn.RACKSCAN_MISMATCH);
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

    /**
     * There is much copying and pasting of code from BSPSampleSearchServiceImpl into here, a refactoring is needed
     *
     * @param bspSampleDTOs BSP DTOs whose sampleID field will be referenced for the barcode value, and which will
     *                      be filled with the ffpeDerived value returned by the FFPE webservice
     */
    public void fetchFFPEDerived(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {
        if (bspSampleDTOs.isEmpty()) {
            return;
        }

        final Map<String, BSPSampleDTO> barcodeToDTOMap = new HashMap<String, BSPSampleDTO>();
        for (BSPSampleDTO bspSampleDTO : bspSampleDTOs) {
            barcodeToDTOMap.put(bspSampleDTO.getSampleId(), bspSampleDTO);
        }

        String urlString = bspConfig.getWSUrl(WS_FFPE_DERIVED);
        String queryString = "barcodes=" + StringUtils.join(barcodeToDTOMap.keySet(), "&barcodes=");
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

    public void fetchSamplePlastic(@Nonnull Collection<BSPSampleDTO> bspSampleDTOs) {
        if (bspSampleDTOs.isEmpty()) {
            return;
        }

        final Map<String, BSPSampleDTO> lsidToDTOMap = new HashMap<String, BSPSampleDTO>();
        for (BSPSampleDTO bspSampleDTO : bspSampleDTOs) {
            lsidToDTOMap.put(bspSampleDTO.getSampleLsid(), bspSampleDTO);
        }

        String urlString = bspConfig.getWSUrl(WS_SAMPLE_DETAILS);
        String queryString = "sample_lsid=" + StringUtils.join(lsidToDTOMap.keySet(), "&sample_lsid=");
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
        List<String[]> results =
                service.runSampleSearch(Collections.singletonList(aliquotId), BSPSampleSearchColumn.STOCK_SAMPLE);
        if (results.isEmpty() || results.get(0).length == 0) {
            return null;
        }
        return results.get(0)[0];
    }
}

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
    
    @Inject BSPSampleSearchService service;

    @Inject BSPConfig bspConfig;

    private final static Log logger = LogFactory.getLog(BSPSampleDataFetcher.class);

    private static final String WS_FFPE_DERIVED = "sample/ffpeDerived";

    public BSPSampleDataFetcher() {}


    /**
     * New one up using the given service.
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
     * @param sampleName
     * @return
     */
    public BSPSampleDTO fetchSingleSampleFromBSP(String sampleName) {
        if (service == null) {
            throw new RuntimeException("No BSP service has been declared.");
        } else {
            Map<String, BSPSampleDTO> sampleNameToDTO = fetchSamplesFromBSP(Collections.singleton(sampleName));

            if (sampleNameToDTO.isEmpty()) {
                throw new RuntimeException("Could not find " + sampleName + " in bsp.");
            } else if (sampleNameToDTO.size() > 1) {
                throw new RuntimeException("Found " + sampleNameToDTO.size() + " possible matches in bsp.");
            } else {
                return sampleNameToDTO.entrySet().iterator().next().getValue();
            }
        }
    }

    /**
     * Fetch the data from bsp for multiple samples.
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

    private static BSPSampleDTO toDTO(String[] bspColumns) {
        String patientId = null;
        String rootSample = null;
        String stockSample = null;
        String collaboratorSampleId = null;
        String collection = null;
        String volume = null;
        String concentration = null;
        String organism = null;
        String sampleLsid = null;
        String gender = null;
        String collaboratorParticipantId = null;
        String materialType = null;
        String total = null;
        String sampleType = null;
        String primaryDisease = null;
        String stockType = null;
        String fingerprint = null;
        String containerId = null;
        String sampleId = null;

        if (bspColumns.length > 0) {
            patientId = bspColumns[0];
        }
        if (bspColumns.length > 1) {
            rootSample = bspColumns[1];
        }
        if (bspColumns.length > 2) {
            stockSample = bspColumns[2];
        }
        if (bspColumns.length > 3) {
            collaboratorSampleId = bspColumns[3];
        }
        if (bspColumns.length > 4) {
            collection = bspColumns[4];
        }
        if (bspColumns.length > 5) {
            volume = bspColumns[5];
        }
        if (bspColumns.length > 6) {
            concentration = bspColumns[6];
        }
        if (bspColumns.length > 7) {
            organism = bspColumns[7];
        }
        if (bspColumns.length > 8) {
            sampleLsid = bspColumns[8];
        }
        if (bspColumns.length > 9) {
            collaboratorParticipantId = bspColumns[9];
        }
        if (bspColumns.length > 10) {
            materialType = bspColumns[10];
        }
        if (bspColumns.length > 11) {
            total = bspColumns[11];
        }
        if (bspColumns.length > 12) {
            sampleType = bspColumns[12];
        }
        if (bspColumns.length > 13) {
            primaryDisease = bspColumns[13];
        }
        if (bspColumns.length > 14) {
            gender = bspColumns[14];
        }
        if (bspColumns.length > 15) {
            stockType = bspColumns[15];
        }
        if (bspColumns.length > 16) {
            fingerprint = bspColumns[16];
        }

        if (bspColumns.length > 17) {
            containerId = bspColumns[17];
        }

        if (bspColumns.length > 18) {
            sampleId = bspColumns[18];
        }
        /** beware of DBFreeBSPSampleTest: if you add columns here, you'll need to add them to the mock **/

        return new BSPSampleDTO(containerId,stockSample,rootSample,null,patientId,organism,collaboratorSampleId,collection,
                                volume,concentration, sampleLsid, collaboratorParticipantId, materialType, total,
                                sampleType, primaryDisease,gender, stockType, fingerprint, sampleId);

    }

    private List<String[]> getBSPResponse(Collection<String> sampleNames) {
        return  service.runSampleSearch(sampleNames,
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
                BSPSampleSearchColumn.SAMPLE_ID);
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

                bspSampleDTO.setFfpeDerived(Boolean.valueOf(bspOutput[FFPE]));
            }
        });
   }

}
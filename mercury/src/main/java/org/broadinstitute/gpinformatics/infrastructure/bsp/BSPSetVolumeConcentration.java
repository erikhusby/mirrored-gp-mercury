package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

/**
 * This class handles grabbing collections from BSP for use in Mercury project set up.
 */
@Impl
public class BSPSetVolumeConcentration extends BSPJerseyClient {

    private static final long serialVersionUID = -2649024856161379565L;

    private String[] result;

    @SuppressWarnings("unused")
    public BSPSetVolumeConcentration() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPSetVolumeConcentration(BSPConfig bspConfig) {
        super(bspConfig);
    }

    public void setVolumeAndConcentration(String barcode, float volume, float concentration) {

        String SET_VOLUME_CONCENTRATION = "sample/setVolumeConcentration";
        String urlString = getUrl(SET_VOLUME_CONCENTRATION);

        String queryString = String.format("barcode='%s'&volume='%f'&concentration='%f'", barcode, volume, concentration);
        post(urlString, queryString, ExtraTab.FALSE, new PostCallback() {
            @Override
            public void callback(String[] bspOutput) {
                result = bspOutput;
                if (result == null) {
                    throw new RuntimeException("Did not get anything back from set volume and concentration service");
                }
            }
        });
    }

    public String[] getResult() {
        return result;
    }
}

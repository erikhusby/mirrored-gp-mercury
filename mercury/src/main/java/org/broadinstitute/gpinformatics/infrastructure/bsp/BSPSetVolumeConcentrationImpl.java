package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class provides Mercury with a way to send volume and concentration to BSP.
 */
@Impl
public class BSPSetVolumeConcentrationImpl extends BSPJerseyClient implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -2649024856161379565L;

    private static final String VOLUME_CONCENTRATION_URL =
            "sample/setVolumeConcentration?barcode=%s&volume=%f&concentration=%f";

    private final String[] result = new String[] { "No result calculated" };

    /**
     * Required for @Impl class.
     */
    @SuppressWarnings("unused")
    public BSPSetVolumeConcentrationImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPSetVolumeConcentrationImpl(BSPConfig bspConfig) {
        super(bspConfig);
    }

    @Override
    public void setVolumeAndConcentration(String barcode, double volume, double concentration) {

        String queryString = String.format(VOLUME_CONCENTRATION_URL, barcode, volume, concentration);
        String urlString = getUrl(queryString);

        BufferedReader rdr = null;

        try {
            WebResource webResource = getJerseyClient().resource(urlString);
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, urlString);

            InputStream is = clientResponse.getEntityInputStream();
            rdr = new BufferedReader(new InputStreamReader(is));

            // Check for OK status.
            if (clientResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                result[0] = "Cannot set volume and concentration: " + clientResponse.getStatus();
            } else {
                result[0] = rdr.readLine();
            }
        } catch (Exception exp) {
            result[0] = "Cannot set volume and concentration: " + exp.getMessage();
        } finally {
            // Close the reader, which will close the underlying input stream.
            IOUtils.closeQuietly(rdr);
        }
    }

    @Override
    public String[] getResult() {
        return result;
    }

    @Override
    public boolean isValidResult() {
        return result[0].startsWith(VALID_COMMUNICATION_PREFIX);
    }
}

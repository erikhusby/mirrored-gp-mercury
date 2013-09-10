package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;

/**
 * This class provides Mercury with a way to send volume and concentration to BSP.
 */
@Impl
public class BSPSetVolumeConcentrationImpl extends BSPJerseyClient implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -2649024856161379565L;

    private static final String VOLUME_CONCENTRATION_URL = "sample/setVolumeConcentration?barcode=%s";

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

    /**
     * Create URL String for the web service call that ignores ignoring null values
     *
     * @return queryString to pass to the web service.
     */
    private static final String getQueryString(String barcode, BigDecimal volume, BigDecimal concentration) {
        String queryString = String.format(VOLUME_CONCENTRATION_URL, barcode);
        if (volume != null) {
            queryString = queryString + String.format("&volume=%f", volume);
        }
        if (concentration != null) {
            queryString = queryString + String.format("&concentration=%f", concentration);
        }
        return queryString;
    }


    /**
     * Call BSP WebService which sets the volume and or the concentration of the thing barcoded.
     * At lease one of must be Nonnull.
     *
     * @param barcode       The thing having its quant updated. In BSP this currently can be a SM id or a manufacturer
     *                      barcode.
     * @param volume        new volume of the sample. Can be null.
     * @param concentration the new concentration of the sample. Can be null.
     */
    @Override
    public void setVolumeAndConcentration(@Nonnull String barcode, @Nullable BigDecimal volume, @Nullable BigDecimal concentration) {
        String queryString = getQueryString(barcode, volume, concentration);
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

package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides Mercury with a way to send volume and concentration to BSP.
 */
@Impl
public class BSPSetVolumeConcentrationImpl extends BSPJerseyClient implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -2649024856161379565L;

    private static final String VOLUME_CONCENTRATION_URL = "sample/setVolumeConcentration";

    private String result;

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
    private static String getQueryString(@Nonnull String barcode, @Nullable BigDecimal volume,
                                         @Nullable BigDecimal concentration)
            throws ValidationException {
        List<NameValuePair> parameters = new ArrayList<>();

        if (volume != null) {
            parameters.add(new BasicNameValuePair("volume", String.valueOf(volume)));
        }

        if (concentration != null) {
            parameters.add(new BasicNameValuePair("concentration", String.valueOf(concentration)));
        }

        if (parameters.isEmpty()) {
            throw new ValidationException("A value for volume or concentration is required.");
        }

        parameters.add(new BasicNameValuePair("barcode", barcode));
        return VOLUME_CONCENTRATION_URL + "?" + URLEncodedUtils.format(parameters, CharEncoding.UTF_8);
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
    public String setVolumeAndConcentration(@Nonnull String barcode, @Nullable BigDecimal volume,
                                            @Nullable BigDecimal concentration) {
        BufferedReader rdr = null;
        try {
            String queryString = getQueryString(barcode, volume, concentration);
            String urlString = getUrl(queryString);

            WebResource webResource = getJerseyClient().resource(urlString);
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, urlString);

            InputStream is = clientResponse.getEntityInputStream();
            rdr = new BufferedReader(new InputStreamReader(is));

            // Check for OK status.
            if (clientResponse.getStatus() == ClientResponse.Status.OK.getStatusCode() &&
                rdr.readLine().startsWith(VALID_COMMUNICATION_PREFIX)) {
                result = RESULT_OK;
            } else {
                result = "Cannot set volume and concentration: " + clientResponse.getStatus();
            }
        } catch (Exception exp) {
            result = "Cannot set volume and concentration: " + exp.getMessage();
        } finally {
            // Close the reader, which will close the underlying input stream.
            IOUtils.closeQuietly(rdr);
        }
        return result;
    }
}

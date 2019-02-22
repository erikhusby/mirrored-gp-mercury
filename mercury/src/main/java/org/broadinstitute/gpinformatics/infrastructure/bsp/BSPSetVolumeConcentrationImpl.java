package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.message.BasicNameValuePair;
import org.broadinstitute.gpinformatics.infrastructure.ValidationException;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;
import org.glassfish.jersey.client.ClientResponse;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * This class provides Mercury with a way to send volume and concentration to BSP.
 */
@Dependent
@Default
public class BSPSetVolumeConcentrationImpl extends BSPJerseyClient implements BSPSetVolumeConcentration {

    private static final long serialVersionUID = -2649024856161379565L;

    private static final String VOLUME_CONCENTRATION_URL = "sample/setVolumeConcentration";

    /**
     * Required for CDI
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
                                         @Nullable BigDecimal concentration, @Nullable BigDecimal receptacleWeight,
                                         @Nullable Boolean terminateDepleted)
            throws ValidationException {
        List<NameValuePair> parameters = new ArrayList<>();

        if (volume != null) {
            parameters.add(new BasicNameValuePair("volume", String.valueOf(volume)));
        }

        if (concentration != null) {
            parameters.add(new BasicNameValuePair("concentration", String.valueOf(concentration)));
        }

        if (receptacleWeight != null) {
            parameters.add(new BasicNameValuePair("receptacle_weight", String.valueOf(receptacleWeight)));
        }

        if (terminateDepleted != null && terminateDepleted) {
            parameters.add(new BasicNameValuePair("terminate_depleted", "true"));
        }

        if (parameters.isEmpty()) {
            throw new ValidationException("A value for volume, concentration or receptacleWeight is required.");
        }

        parameters.add(new BasicNameValuePair("barcode", barcode));
        return VOLUME_CONCENTRATION_URL + "?" + URLEncodedUtils.format(parameters, CharEncoding.UTF_8);
    }


    /**
     * Call BSP WebService which sets the volume and or the concentration of the thing barcoded.
     * At lease one of must be Nonnull.
     *
     * @param barcode         The thing having its quant updated. In BSP this currently can be a SM id or a manufacturer
     *                        barcode.
     * @param volume          New volume of the sample. Can be null.
     * @param concentration   The new concentration of the sample. Can be null.
     * @param terminateAction Whether to terminate the sample if it is depleted or leave the sample in the same state.
     */
    @Override
    public String setVolumeAndConcentration(@Nonnull String barcode, @Nullable BigDecimal volume,
                                            @Nullable BigDecimal concentration, @Nullable BigDecimal receptacleWeight,
                                            @Nullable TerminateAction terminateAction) {
        BufferedReader rdr = null;
        String result;
        try {
            String queryString = getQueryString(barcode, volume, concentration, receptacleWeight, terminateAction.getTerminateDepleted());
            String urlString = getUrl(queryString);

            WebTarget webTarget = getJerseyClient().target(urlString);
            ClientResponse clientResponse =
                    webTarget.request(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(null, ClientResponse.class); // todo jmt is this right?

            InputStream is = clientResponse.getEntityStream();
            rdr = new BufferedReader(new InputStreamReader(is));

            // Check for OK status.
            String firstLine = rdr.readLine();
            if (clientResponse.getStatus() == Response.Status.OK.getStatusCode() &&
                    firstLine.startsWith(VALID_COMMUNICATION_PREFIX)) {
                result = RESULT_OK;
            } else {
                result = "Cannot set volume and concentration: " + firstLine + "(" + clientResponse.getStatus() + ")";
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

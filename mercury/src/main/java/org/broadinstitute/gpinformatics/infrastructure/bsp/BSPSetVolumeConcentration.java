package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * This class handles grabbing collections from BSP for use in Mercury project set up.
 */
@Impl
public class BSPSetVolumeConcentration extends BSPJerseyClient {

    private static final long serialVersionUID = -2649024856161379565L;

    private static Log logger = LogFactory.getLog(BSPCohortSearchServiceImpl.class);

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

    public void setVolumeAndConcentration(String barcode, double volume, double concentration) {

        String SET_VOLUME_CONCENTRATION = "sample/setVolumeConcentration";
        String queryString = String.format("?barcode=%s&volume=%f&concentration=%f", barcode, volume, concentration);
        String urlString = getUrl(SET_VOLUME_CONCENTRATION + queryString);

        BufferedReader rdr = null;

        try {
            WebResource webResource = getJerseyClient().resource(urlString);
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, urlString);

            InputStream is = clientResponse.getEntityInputStream();
            rdr = new BufferedReader(new InputStreamReader(is));

            // Check for OK status.
            if (clientResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                String errMsg = "Cannot set volume and concentration : " + clientResponse.getStatus();
                logger.error(errMsg + " : " + rdr.readLine());
                throw new RuntimeException(errMsg);
            }

            // Skip the header line.
            result = new String[1];
            result[0] = rdr.readLine();
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with BSP platform to get all cohorts";
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error(exp.getMessage(), exp);
            throw new RuntimeException(exp);
        } finally {
            // Close the reader, which will close the underlying input stream.
            IOUtils.closeQuietly(rdr);
        }
    }

    public String[] getResult() {
        return result;
    }
}

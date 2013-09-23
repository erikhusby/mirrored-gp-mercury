package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.BSPJerseyClient;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the Bsp sample receipt service.
 */
@Impl
public class BSPSampleReceiptServiceImpl extends BSPJerseyClient implements BSPSampleReceiptService {

    private static final String WEB_SERVICE_URL = "sample/receivesamples";
    private static final XStream XSTREAM = new XStream();

    /**
     * Required for @Impl class.
     */
    @SuppressWarnings("unused")
    public BSPSampleReceiptServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPSampleReceiptServiceImpl(BSPConfig bspConfig) {
        super(bspConfig);
    }

    @Override
    public SampleKitReceiptResponse receiveSamples(List<String> barcodes, String username)
            throws UnsupportedEncodingException {

        // Prepare the parameters.
        List<String> parameters = new ArrayList<>();
        parameters.add("username=" + username);
        parameters.add("barcodes=" + StringUtils.join(barcodes, ","));
        parameters.add("format=xml");

        String parameterString = StringUtils.join(parameters, "&");
        final String ENCODING = "UTF-8";
        // Change to the URL string and fire off the web service.
        String urlString = getUrl(WEB_SERVICE_URL + "?" + parameterString);

        WebResource webResource = getJerseyClient().resource(urlString);

        ClientResponse clientResponse = webResource.accept(MediaType.TEXT_PLAIN).post(ClientResponse.class);

        InputStream inputStream = clientResponse.getEntityInputStream();
        Reader reader = null;

        Object resultObject = null;
        try {

            reader = new InputStreamReader(inputStream, Charset.forName(ENCODING));

            resultObject = XSTREAM.fromXML(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {

            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(reader);
        }
        return (SampleKitReceiptResponse) resultObject;
    }
}

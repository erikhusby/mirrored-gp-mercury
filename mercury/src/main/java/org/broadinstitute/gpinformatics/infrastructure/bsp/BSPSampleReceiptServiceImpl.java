package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.thoughtworks.xstream.XStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.bsp.client.response.SampleKitReceiptResponse;
import org.broadinstitute.gpinformatics.mercury.BSPJaxRsClient;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
@Dependent
@Default
public class BSPSampleReceiptServiceImpl extends BSPJaxRsClient implements BSPSampleReceiptService {

    private static final String WEB_SERVICE_URL = "sample/receivesamples";
    private static final XStream XSTREAM = new XStream();

    /**
     * Required for CDI
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

        WebTarget webResource = getJerseyClient().target(urlString);

        Response clientResponse = webResource.request(MediaType.TEXT_PLAIN).post(null);

        InputStream inputStream = clientResponse.readEntity(InputStream.class);
        Reader reader = null;

        Object resultObject = null;
        try {

            reader = new InputStreamReader(inputStream, Charset.forName(ENCODING));

            resultObject = XSTREAM.fromXML(reader);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            clientResponse.close();
            IOUtils.closeQuietly(inputStream);
            IOUtils.closeQuietly(reader);
        }
        return (SampleKitReceiptResponse) resultObject;
    }
}

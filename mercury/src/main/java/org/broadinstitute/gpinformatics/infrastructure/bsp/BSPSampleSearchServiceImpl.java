package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Impl
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements
                                                BSPSampleSearchService {

    private static Log logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);

    public static final String SEARCH_RUN_SAMPLE_SEARCH = "search/runSampleSearch";

    @Inject
    private BSPConfig bspConfig;

    enum Endpoint {

        SAMPLE_SEARCH("search/runSampleSearch");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }
    }


    /**
     * No arg constructor for CDI
     */
    public BSPSampleSearchServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly
     *
     * @param bspConfig
     */
    public BSPSampleSearchServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
        logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);
    }


    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }


    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... queryColumns) {

        if (queryColumns == null || queryColumns.length == 0)
            throw new RuntimeException("No query columns supplied!");

        if (sampleIDs == null)
            return null;
        
        if (sampleIDs.size() == 0)
            return new ArrayList<String[]>();

        List<String[]> ret = new ArrayList<String[]>();

        String urlString = bspConfig.getWSUrl(SEARCH_RUN_SAMPLE_SEARCH);
        logger.debug(String.format("URL string is '%s'", urlString));
        WebResource webResource = getJerseyClient().resource(urlString);

        List<String> queryParameters = new ArrayList<String>();

        try {

            for (BSPSampleSearchColumn column : queryColumns)
                queryParameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));

            for (String sampleID : sampleIDs)
                queryParameters.add("sample_ids=" + sampleID);

            String queryString = "";
            if (queryParameters.size() > 0)
                queryString = StringUtils.join(queryParameters, "&");

            logger.trace("query string to be POSTed is '" + queryString + "'");
            
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, queryString);
            
            InputStream is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));     
            
            if (clientResponse.getStatus() / 100 != 2) {
                logger.error("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
                return ret;
                // throw new RuntimeException("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
            }

            // skip header line
            rdr.readLine();
            
            // what should be the first real data line
            String readLine = rdr.readLine();
            
            while (readLine != null) {
                
                String[] rawBSPData = readLine.split("\t", -1);
                
                // BSP always seems to return 1 more field than we asked for?
                String[] truncatedData = new String[rawBSPData.length - 1];
                System.arraycopy(rawBSPData, 0, truncatedData, 0, truncatedData.length);
                
                ret.add(truncatedData);
                
                readLine = rdr.readLine();
                
            }

            is.close();

        }
        catch (ClientHandlerException clientException) {
            throw new RuntimeException("Error connecting to BSP",clientException);
        }
        catch (UnsupportedEncodingException uex) {
            throw new RuntimeException(uex);
        }
        catch (MalformedURLException mux) {
            throw new RuntimeException(mux);
        }
        catch (IOException iox) {
            throw new RuntimeException(iox);
        }

        return ret;

    }

    @Override
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
        BSPSampleSearchColumn [] dummy = new BSPSampleSearchColumn[resultColumns.size()];
        return runSampleSearch(sampleIDs, resultColumns.toArray(dummy));
    }

}

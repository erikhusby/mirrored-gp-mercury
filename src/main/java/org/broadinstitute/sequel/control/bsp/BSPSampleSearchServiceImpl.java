package org.broadinstitute.sequel.control.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.*;


public class BSPSampleSearchServiceImpl implements BSPSampleSearchService {

    private static Log _logger = LogFactory
            .getLog(BSPSampleSearchServiceImpl.class);
    

    private BSPConnectionParameters connParams;

    private Client jerseyClient;

    public BSPSampleSearchServiceImpl(BSPConnectionParameters params) {
        if (params == null) {
             throw new NullPointerException("params cannot be null.");
        }
        this.connParams = params;
    }

    private Client getClient() {

        if (jerseyClient == null) {
            ClientConfig clientConfiguration = new DefaultClientConfig();
            jerseyClient = Client.create(clientConfiguration);

            jerseyClient.addFilter(new HTTPBasicAuthFilter(connParams.getSuperuserLogin(), connParams.getSuperuserPassword()));
        }
        return jerseyClient;
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
        

        String urlString = "http://%s:%d/ws/bsp/search/runSampleSearch";
        urlString = String.format(urlString, connParams.getHostname(), connParams.getPort());
        
        _logger.info(String.format("url string is '%s'", urlString));
        
        WebResource webResource = getClient().resource(urlString);


        List<String> queryParameters = new ArrayList<String>();
       
        
        try {

            for (BSPSampleSearchColumn column : queryColumns)
                queryParameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));

            for (String sampleID : sampleIDs)
                queryParameters.add("sample_ids=" + sampleID);

            String queryString = "";
            if (queryParameters.size() > 0)
                queryString = StringUtils.join(queryParameters, "&");

            _logger.info("query string to be POSTed is '" + queryString + "'");
            
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, queryString);
            
            InputStream is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));     
            
            if (clientResponse.getStatus() / 100 != 2) {
                _logger.error("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
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


    @Override
    public Map<String, String> lsidsToBareIds(Collection<String> lsids) {
        Map<String, String> ret = new HashMap<String, String>();
        
        for (String lsid : lsids) {
            String [] chunks = lsid.split(":");
            ret.put(lsid, chunks[chunks.length-1]);
        }
        
        return ret;

    }

}

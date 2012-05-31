package org.broadinstitute.pmbridge.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.pmbridge.control.AbstractJerseyClientService;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollection;
import org.broadinstitute.pmbridge.entity.bsp.BSPCollectionID;
import org.broadinstitute.pmbridge.entity.person.Person;
import org.omg.CosNaming.NamingContextPackage.CannotProceed;

import javax.enterprise.inject.Default;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.*;

@Default
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements BSPSampleSearchService {


    private static Log _logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);

    @Inject
    private BSPConnectionParameters connParams;


    public BSPSampleSearchServiceImpl() {
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, connParams);
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

        String urlString = "http://%s:%d%s";
        urlString = String.format(urlString, connParams.getHostname(), connParams.getPort(),
                BSPConnectionParameters.BSP_SAMPLE_SEARCH_URL);
        
        _logger.info(String.format("url string is '%s'", urlString));
        
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


    //TODO hmc Fake up a retrieved collection.
    private Collection<BSPCollection> getFakeCollections() {

        HashSet<BSPCollection> fakeCohorts = new HashSet<BSPCollection>();
        fakeCohorts.add( new BSPCollection(new BSPCollectionID("12345"), "AlxCollection1"));
        return fakeCohorts;

    }


    private Set<BSPCollection> runCollectionSearch(Person bspUser ) {

        if ((bspUser == null) || (StringUtils.isBlank(bspUser.getUsername())) )  {
            throw new IllegalArgumentException( "Cannot lookup cohorts for user without a valid username." );
        }

        HashSet<BSPCollection> usersCohorts = new HashSet<BSPCollection>();

        String urlString = "http://%s:%d%s";
        urlString = String.format(urlString, connParams.getHostname(), connParams.getPort(),
                BSPConnectionParameters.BSP_USERS_COHORT_URL + bspUser.getUsername().trim() );
        _logger.info(String.format("url string is '%s'", urlString));

        WebResource webResource = getJerseyClient().resource(urlString);

        try {
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, urlString);

            InputStream is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

            if (clientResponse.getStatus() / 100 != 2) {
                _logger.error("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
                return usersCohorts;
                // throw new RuntimeException("response code " + clientResponse.getStatus() + ": " + rdr.readLine());
            }

            // skip the header line
            rdr.readLine();
            // what should be the first real data line
            String readLine = rdr.readLine();
            while (readLine != null) {
                String[] rawBSPData = readLine.split("\t", -1);

                //TODO - For now only interested in the first two fields
                if ( (rawBSPData.length >= 2 ) &&
                        StringUtils.isNotBlank( rawBSPData[0] ) &&
                        StringUtils.isNotBlank( rawBSPData[1]) ) {

                    BSPCollectionID bspCollectionID = new BSPCollectionID(rawBSPData[0]);
                    BSPCollection bspCollection = new BSPCollection ( bspCollectionID, rawBSPData[1] );
                    usersCohorts.add( bspCollection );
                } else {
                    _logger.error( "Found a line from BSP Cohort for user " + bspUser.getUsername() + " which had less than two fields of real data <" + readLine  + ">");
                }
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

        return usersCohorts;
    }



    @Override
    public Set<BSPCollection> getCohortsByUser(Person bspUser) {

        if ((bspUser == null ) || (StringUtils.isBlank(bspUser.getUsername()))) {
            throw new IllegalArgumentException("Bsp Username is not valid. Canot retrieve list of cohorts from BSP.");
        }

        Set<BSPCollection> bspCollections = runCollectionSearch(bspUser);

        return bspCollections;

    }

    @Override
    public List<String> runSampleSearchByCohort(BSPCollection cohort) {

        if ((cohort == null) ) {
            throw new IllegalArgumentException("Cohort param was null. Cannot retrieve list of samples from BSP.");
        }

        List<String> results = new ArrayList<String>();

        // find samples in cohort

        // TODO hmc Replace this with new BSP API call to get samples per cohort.
        // Faked out here as API is not yet available. Add 4 samples to results.
        List<String> tempIds = new ArrayList<String>();
        tempIds.add("SM-11K1"); tempIds.add("SM-11K2");tempIds.add("SM-11K4");tempIds.add("SM-11K4");

        List<String[]> tempResults = runSampleSearch(tempIds, BSPSampleSearchColumn.SAMPLE_ID );
        for ( String[] sampleResult : tempResults ) {
            results.add(sampleResult[0]);
        }
        //TODO end

        return results;
    }

}

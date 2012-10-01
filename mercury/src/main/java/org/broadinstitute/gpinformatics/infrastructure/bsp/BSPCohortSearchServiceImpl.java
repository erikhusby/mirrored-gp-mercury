package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.athena.entity.project.CohortID;
import org.broadinstitute.gpinformatics.mercury.entity.person.Person;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.*;

@Impl
public class BSPCohortSearchServiceImpl extends AbstractJerseyClientService implements BSPCohortSearchService {


    private static Log logger = LogFactory.getLog(BSPCohortSearchServiceImpl.class);

    @Inject
    private BSPConfig bspConfig;

    enum Endpoint {

        SAMPLE_SEARCH("search/runSampleSearch"),
        USERS_COHORT("collection/getSampleCollectionsByPM?username=");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }

    }



    private String url( Endpoint endpoint ) {

        return String.format("%s:%d/ws/bsp/%s", bspConfig.getHost(), bspConfig.getPort(), endpoint.getSuffixUrl());

    }


    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }


    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

// TODO remove
    public List<String[]> runSampleSearch(Collection<String> sampleIDs, BSPSampleSearchColumn... queryColumns) {

        if (queryColumns == null || queryColumns.length == 0)
            throw new RuntimeException("No query columns supplied!");

        if (sampleIDs == null)
            return null;

        if (sampleIDs.size() == 0)
            return new ArrayList<String[]>();

        List<String[]> ret = new ArrayList<String[]>();

        String urlString = url(Endpoint.SAMPLE_SEARCH);

        logger.info(String.format("url string is '%s'", urlString));

        WebResource webResource = getJerseyClient().resource(urlString);


        List<String> queryParameters = new ArrayList<String>();
        InputStream is = null;

        try {
            for (BSPSampleSearchColumn column : queryColumns)
                queryParameters.add("columns=" + URLEncoder.encode(column.columnName(), "UTF-8"));

            for (String sampleID : sampleIDs)
                queryParameters.add("sample_ids=" + sampleID);

            String queryString = "";
            if (queryParameters.size() > 0)
                queryString = StringUtils.join(queryParameters, "&");

            logger.info("query string to be POSTed is '" + queryString + "'");

            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, queryString);

            is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));

            if (clientResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                String errMsg = "Cannot retrieve sample data from BSP platform. Received response code : " + clientResponse.getStatus();
                logger.error(errMsg + " : " + rdr.readLine());
                throw new RuntimeException(errMsg);
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
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with BSP platform to retrieve sample data.";
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error("Exception occurred trying to retrieve BSP sample data  : " + exp.getMessage(), exp);
            throw new RuntimeException(exp);
        }  finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) { }
            }
        }

        return ret;

    }

//    @Override
//    public List<String[]> runSampleSearch(Collection<String> sampleIDs, List<BSPSampleSearchColumn> resultColumns) {
//        BSPSampleSearchColumn [] dummy = new BSPSampleSearchColumn[resultColumns.size()];
//        return runSampleSearch(sampleIDs, resultColumns.toArray(dummy));
//    }


    private Set<Cohort> runCollectionSearch(Person bspUser ) {

        String urlString = "http://%s:%d%s";

        if ((bspUser == null) || (StringUtils.isBlank(bspUser.getLogin())) )  {
            throw new IllegalArgumentException( "Cannot lookup cohorts for user without a valid username." );
        }

        HashSet<Cohort> usersCohorts = new HashSet<Cohort>();
        urlString = url(Endpoint.USERS_COHORT)  + bspUser.getLogin().trim();
        logger.info(String.format("url string is '%s'", urlString));
        WebResource webResource = getJerseyClient().resource(urlString);

        InputStream is = null;
        try {
            ClientResponse clientResponse =
                    webResource.type(MediaType.APPLICATION_FORM_URLENCODED_TYPE).post(ClientResponse.class, urlString);

            is = clientResponse.getEntityInputStream();
            BufferedReader rdr = new BufferedReader(new InputStreamReader(is));


            // Check for 200
            if (clientResponse.getStatus() != ClientResponse.Status.OK.getStatusCode()) {
                String errMsg = "Cannot retrieve cohorts from BSP platform for user " + bspUser.getLogin() + ". Received response code : " + clientResponse.getStatus();
                logger.error(errMsg + " : " + rdr.readLine());
                throw new RuntimeException(errMsg);
            }

            // Skip the header line.
            rdr.readLine();
            // what should be the first real data line
            String readLine = rdr.readLine();
            while (readLine != null) {
                String[] rawBSPData = readLine.split("\t", -1);

                // For now only interested in the first two fields
                if ( (rawBSPData.length >= 2 ) &&
                        StringUtils.isNotBlank( rawBSPData[0] ) &&
                        StringUtils.isNotBlank( rawBSPData[1]) ) {
                    /* Example record:
                     * Collection ID Collection Name	          Collection Category Group Name   PI Lastname	PI Firstname Collaborator Lastname	                Collaborator Firstname
                     * SC-912	      Cell Line Samples - Coriell		              1000 Genomes Gabriel	    Stacey	     Coriell Institute for Medical Research	Institute:
                     */
                    CohortID bspCollectionID = new CohortID(rawBSPData[0]);
                    Cohort bspCollection = new Cohort( bspCollectionID, rawBSPData[1] );
                    usersCohorts.add( bspCollection );
                } else {
                    logger.error("Found a line from BSP Cohort for user " + bspUser.getLogin() + " which had less than two fields of real data. Ignoring this line  <" + readLine + ">");
                }
                readLine = rdr.readLine();
            }
            is.close();
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with BSP platform for user " + bspUser.getLogin();
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error(exp.getMessage(), exp);
            throw new RuntimeException(exp);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) { }
            }
        }
        return usersCohorts;
    }



    /* Method to retrieve all of the cohorts associated with a particular BSP user. The available columns of data are
     * Collection ID
     * Collection Name
     * Collection Category
     * Group Name
     * PI Lastname
     * PI Firstname
     * Collaborator Lastname
     * Collaborator Firstname
     */
    @Override
    public Set<Cohort> getCohortsByUser(Person bspUser) {

        if ((bspUser == null ) || (StringUtils.isBlank(bspUser.getLogin()))) {
            throw new IllegalArgumentException("Bsp Username is not valid. Canot retrieve list of cohorts from BSP.");
        }

        Set<Cohort> bspCollections = runCollectionSearch(bspUser);

        return bspCollections;

    }

    @Override
    public List<String> runSampleSearchByCohort(Cohort cohort) {

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

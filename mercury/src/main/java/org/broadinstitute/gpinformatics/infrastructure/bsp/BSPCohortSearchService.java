package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.jetbrains.annotations.NotNull;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.util.*;

public class BSPCohortSearchService extends AbstractJerseyClientService {

    private static Log logger = LogFactory.getLog(BSPCohortSearchService.class);

    @Inject
    private BSPConfig bspConfig;

    enum Endpoint {

        SAMPLE_SEARCH("search/runSampleSearch"),
        USERS_COHORT("collection/getSampleCollectionsByPM?username="),
        ALL_COHORTS("collection/get_all_collections");

        String suffixUrl;

        Endpoint(String suffixUrl) {
            this.suffixUrl = suffixUrl;
        }

        public String getSuffixUrl() {
            return suffixUrl;
        }

    }

    private String url( Endpoint endpoint ) {
        return String.format("http://%s:%d/ws/bsp/%s", bspConfig.getHost(), bspConfig.getPort(), endpoint.getSuffixUrl());
    }

    @Override
    protected void customizeConfig(ClientConfig clientConfig) {
        // noop
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }

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
            IOUtils.closeQuietly(is);
        }

        return ret;
    }

    private Set<Cohort> runCollectionSearch() {

        SortedSet<Cohort> usersCohorts = new TreeSet<Cohort>(Cohort.COHORT_BY_ID);
        String urlString = url(Endpoint.ALL_COHORTS);
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
                String errMsg = "Cannot retrieve all cohorts from BSP platform. Received response code : " + clientResponse.getStatus();
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
                if ( (rawBSPData.length >= 5 ) &&
                        StringUtils.isNotBlank( rawBSPData[0] ) &&
                        StringUtils.isNotBlank( rawBSPData[1]) ) {

                    boolean archived = rawBSPData[4].equals("1");
                    Cohort bspCollection = new Cohort( rawBSPData[0], rawBSPData[1], rawBSPData[2], rawBSPData[3], archived);
                    usersCohorts.add( bspCollection );
                } else {
                    logger.error("Found a line from BSP Cohort which had less than the required five fields of real data. Ignoring this line  <" + readLine + ">");
                }
                readLine = rdr.readLine();
            }
            is.close();
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with BSP platform to get all cohorts";
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error(exp.getMessage(), exp);
            throw new RuntimeException(exp);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return usersCohorts;
    }

    private Set<Cohort> runCollectionSearch(@NotNull String bspUsername) {

        if (StringUtils.isBlank(bspUsername))  {
            throw new IllegalArgumentException( "Cannot lookup cohorts for user without a valid username." );
        }

        HashSet<Cohort> usersCohorts = new HashSet<Cohort>();
        String urlString = url(Endpoint.USERS_COHORT)  + bspUsername.trim();
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
                String errMsg = "Cannot retrieve cohorts from BSP platform for user " + bspUsername + ". Received response code : " + clientResponse.getStatus();
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
                    Cohort bspCollection = new Cohort( rawBSPData[0], rawBSPData[1], rawBSPData[2], rawBSPData[3], false);
                    usersCohorts.add( bspCollection );
                } else {
                    logger.error("Found a line from BSP Cohort for user " + bspUsername + " which had less than two fields of real data. Ignoring this line  <" + readLine + ">");
                }
                readLine = rdr.readLine();
            }
            is.close();
        } catch(ClientHandlerException e) {
            String errMsg = "Could not communicate with BSP platform for user " + bspUsername;
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error(exp.getMessage(), exp);
            throw new RuntimeException(exp);
        } finally {
            IOUtils.closeQuietly(is);
        }
        return usersCohorts;
    }

    /* Method to retrieve all of the cohorts associated with a particular BSP user. The available columns of data are
     * Collection ID
     * Collection Name
     * Collection Category
     * Group Name
     * Archived
     */
    public Set<Cohort> getAllCohorts() {
        return runCollectionSearch();
    }

    /* Method to retrieve all of the cohorts associated with a particular BSP user. The available columns of data are
     * Collection ID
     * Collection Name
     * Collection Category
     * Group Name
     * Archived
     */
    public Set<Cohort> getCohortsByUser(@NotNull String bspUser) {
        if (StringUtils.isBlank(bspUser)) {
            throw new IllegalArgumentException("Bsp Username is not valid. Canot retrieve list of cohorts from BSP.");
        }

        return runCollectionSearch(bspUser);
    }

    public List<String> runSampleSearchByCohort(Cohort cohort) {

        if ((cohort == null) ) {
            throw new IllegalArgumentException("Cohort param was null. Cannot retrieve list of samples from BSP.");
        }

        List<String> results = new ArrayList<String>();

        // Faked out here as API is not yet available. Add 4 samples to results.
        List<String> tempIds = new ArrayList<String>();
        tempIds.add("SM-11K1"); tempIds.add("SM-11K2");tempIds.add("SM-11K4");tempIds.add("SM-11K4");

        List<String[]> tempResults = runSampleSearch(tempIds, BSPSampleSearchColumn.SAMPLE_ID );
        for ( String[] sampleResult : tempResults ) {
            results.add(sampleResult[0]);
        }

        return results;
    }

}

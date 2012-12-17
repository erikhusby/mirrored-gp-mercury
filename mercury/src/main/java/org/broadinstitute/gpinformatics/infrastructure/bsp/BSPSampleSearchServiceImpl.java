package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.ClientConfig;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.util.*;

@Impl
public class BSPSampleSearchServiceImpl extends AbstractJerseyClientService implements
                                                BSPSampleSearchService, BSPCohortSearchService, BSPMaterialTypeService {

    private static Log logger = LogFactory.getLog(BSPSampleSearchServiceImpl.class);

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

        String urlString = getUrlForEndPoint( Endpoint.SAMPLE_SEARCH );
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

            logger.info("query string to be POSTed is '" + queryString + "'");
            
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


    @Override
    public Set<Cohort> getAllCohorts() {

        SortedSet<Cohort> usersCohorts = new TreeSet<Cohort>(Cohort.COHORT_BY_ID);

        String urlString = getUrlForEndPoint( Endpoint.ALL_COHORTS );
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


    //TODO not tested.
    private Set<Cohort> getCohortsForUser(@Nonnull String bspUsername) {

        if (StringUtils.isBlank(bspUsername))  {
            throw new IllegalArgumentException( "Cannot lookup cohorts for user without a valid username." );
        }

        HashSet<Cohort> usersCohorts = new HashSet<Cohort>();
        String urlString = getUrlForEndPoint( Endpoint.USERS_COHORT ) + bspUsername;
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




    private String getUrlForEndPoint(Endpoint endpoint) {
        String urlString = bspConfig.getWSUrl(endpoint.getSuffixUrl());
        logger.debug(String.format("URL string is '%s'", urlString));
        return urlString;
    }

}

package org.broadinstitute.gpinformatics.infrastructure.bsp;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
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
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 12/17/12
 * Time: 3:34 PM
 */
@Impl
public class BSPCohortSearchServiceImpl extends AbstractJerseyClientService implements BSPCohortSearchService {

    private static Log logger = LogFactory.getLog(BSPCohortSearchServiceImpl.class);

    @Inject
    protected BSPConfig bspConfig;

    enum Endpoint {

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

    public BSPCohortSearchServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly
     *
     * @param bspConfig The config object
     */
    public BSPCohortSearchServiceImpl(BSPConfig bspConfig) {
        this.bspConfig = bspConfig;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
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
                        StringUtils.isNotBlank(rawBSPData[0]) &&
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

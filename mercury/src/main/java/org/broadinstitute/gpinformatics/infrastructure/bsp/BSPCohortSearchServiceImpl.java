package org.broadinstitute.gpinformatics.infrastructure.bsp;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.mercury.BSPJaxRsClient;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This class handles grabbing collections from BSP for use in Mercury project set up.
 */
@Dependent
@Default
public class BSPCohortSearchServiceImpl extends BSPJaxRsClient implements BSPCohortSearchService {

    private static final long serialVersionUID = -1765914773249771569L;

    private static Log logger = LogFactory.getLog(BSPCohortSearchServiceImpl.class);

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

    @SuppressWarnings("unused")
    public BSPCohortSearchServiceImpl() {
    }

    /**
     * Container free constructor, need to initialize all dependencies explicitly.
     *
     * @param bspConfig The config object
     */
    public BSPCohortSearchServiceImpl(BSPConfig bspConfig) {
        super(bspConfig);
    }

    /**
     * @return Get all the cohorts from BSP so that they can be cached and displayed in Mercury UIs.
     */
    @Override
    public Set<Cohort> getAllCohorts() {

        // These are needed outside the try for errors, cleanup, and return values.
        BufferedReader rdr = null;
        String urlString = getUrl(Endpoint.ALL_COHORTS.getSuffixUrl());
        SortedSet<Cohort> usersCohorts = new TreeSet<>(Cohort.COHORT_BY_ID);

        Response clientResponse = null;
        try {
            WebTarget webResource = getJerseyClient().target(urlString);
            clientResponse = webResource.request().get();

            InputStream is = clientResponse.readEntity(InputStream.class);
            rdr = new BufferedReader(new InputStreamReader(is));

            // Check for OK status.
            if (clientResponse.getStatus() != Response.Status.OK.getStatusCode()) {
                String errMsg = "Cannot retrieve all cohorts from BSP platform. Received response code : " + clientResponse.getStatus();
                logger.error(errMsg + " : " + rdr.readLine());
                throw new RuntimeException(errMsg);
            }

            // Skip the header line.
            rdr.readLine();

            // What should be the first real data line.
            String readLine = rdr.readLine();
            while (readLine != null) {
                String[] rawBSPData = readLine.split("\t", -1);

                // For now only interested in the first two fields.
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
        } catch(WebApplicationException e) {
            String errMsg = "Could not communicate with BSP platform to get all cohorts";
            logger.error(errMsg + " at " + urlString, e);
            throw e;
        } catch (Exception exp) {
            logger.error(exp.getMessage(), exp);
            throw new RuntimeException(exp);
        } finally {
            // Close the reader, which will close the underlying input stream.
            IOUtils.closeQuietly(rdr);
            if (clientResponse != null) {
                clientResponse.close();
            }
        }

        return usersCohorts;
    }
}

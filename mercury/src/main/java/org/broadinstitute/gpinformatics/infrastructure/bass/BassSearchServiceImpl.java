/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.bass;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import org.apache.commons.io.IOUtils;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Impl;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Impl
public class BassSearchServiceImpl extends AbstractJerseyClientService implements BassSearchService {
    private final BassConfig bassConfig;

    public static String LIST = "list";

    @Inject
    public BassSearchServiceImpl(BassConfig bassConfig) {
        this.bassConfig = bassConfig;
    }

    @Override
    public List<BassDTO> runSearch(Map<BassDTO.BassResultColumn, List<String>> parameters) {
        String url = bassConfig.getWSUrl(LIST);
        MultivaluedMap<String, String> params = new MultivaluedMapImpl();

        for (Map.Entry<BassDTO.BassResultColumn, List<String>> queryPair : parameters.entrySet()) {
            params.put(queryPair.getKey().name(), queryPair.getValue());
        }
        WebResource resource = getJerseyClient().resource(url);
        resource.accept(MediaType.TEXT_PLAIN);
        resource.queryParams(params);
        ClientResponse response = resource.queryParams(params).get(ClientResponse.class);
        List<BassDTO> results;
        if (!response.getClientResponseStatus().equals(ClientResponse.Status.OK)) {
            throw new InformaticsServiceException("Error getting data from Bass " + response.getClientResponseStatus());
        } else {
            InputStream is = response.getEntityInputStream();
            StringWriter writer = new StringWriter();
            try {
                IOUtils.copy(is, writer);
                String theString = writer.toString();
                BassResultsParser parser = new BassResultsParser();
                results = parser.parse(theString);
            } catch (IOException e) {
                throw new RuntimeException("Error reading data from server." + e.getMessage(), e);
            }
        }
        return results;
    }

    @Override
    public List<BassDTO> searchByResearchProject(String researchProjectId) {
        Map<BassDTO.BassResultColumn, List<String>> parameters = new HashMap<>();
        parameters.put(BassDTO.BassResultColumn.rpid, Arrays.asList(researchProjectId));
        return runSearch(parameters);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bassConfig);
    }
}

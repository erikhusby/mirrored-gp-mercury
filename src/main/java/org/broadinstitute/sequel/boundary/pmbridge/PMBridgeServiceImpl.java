package org.broadinstitute.sequel.boundary.pmbridge;


import com.sun.jersey.api.client.*;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProject;
import org.broadinstitute.sequel.boundary.pmbridge.data.ResearchProjectsResult;
import org.broadinstitute.sequel.infrastructure.pmbridge.PMBridgeConnectionParameters;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.List;

public class PMBridgeServiceImpl implements PMBridgeService {

    @Inject
    private PMBridgeConnectionParameters connectionParameters;

    private Client client;

    private Client getClient() {

        if(client == null)
        {
            ClientConfig config = new DefaultClientConfig();

            client = Client.create(config);
            // borrow mime type override logic from QuoteService; this should be available in a common place since this
            // is unfortunately becoming a common problem
            client.addFilter(new ClientFilter() {
                @Override
                public ClientResponse handle(ClientRequest cr) throws ClientHandlerException {
                    ClientResponse resp = getNext().handle(cr);
                    MultivaluedMap<String, String> map = resp.getHeaders();
                    List<String> mimeTypes = new ArrayList<String>();
                    mimeTypes.add(MediaType.APPLICATION_XML);
                    map.put("Content-Type", mimeTypes);
                    return resp;
                }
            });
        }

        return client;
    }


    @Override
    public ResearchProject getResearchProjectByID(String id) {
        if (id == null)
            throw new ResearchProjectNotFoundException("RPID is null!");

        try {

            Client client = getClient();

            WebResource resource = client.resource(connectionParameters.getUrl() + "/ResearchProjectsDataServlet");

            ResearchProjectsResult result = resource.accept(MediaType.APPLICATION_XML).get(ResearchProjectsResult.class);

            if (result == null)
                throw new ResearchProjectNotFoundException("No results!");

            if (result.getResearchProjects() == null)
                throw new ResearchProjectNotFoundException("No results!");

            for (ResearchProject rp : result.getResearchProjects())
                if (id.equals(rp.getId()))
                    return rp;

        } catch (UniformInterfaceException e) {
            throw new ResearchProjectNotFoundException("Could not find ResearchProject for RPID " + id, e);
        } catch (ClientHandlerException e) {
            throw new PMBridgeException(e);
        }

        throw new ResearchProjectNotFoundException("Could not find ResearchProject for RPID " + id);
    }

    @Override
    public String getNameForResearchProjectID(String id) {

        ResearchProject researchProject = getResearchProjectByID(id);

        return researchProject.getName();

    }
}

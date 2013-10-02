package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.sun.jersey.api.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiDTO;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiInfo;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TODO scottmat fill in javadoc!!!
 */
@Path("/crspinfo")
@Stateful
@RequestScoped
public class CrspInfoResource extends AbstractJerseyClientService {

    public static final String SEARCH_CRSP_PHI = "sample/getcrspphenotypes";

    @Inject
    private BSPConfig bspConfig;

    @GET
    @Path("/getCrspPhiInfo")
    @RolesAllowed("CRSP-Mercury-ProjectManagers, CRSP-Mercury-Administrators, CRSP-Mercury-Developers")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCrspPhiInfo(@QueryParam("sampleIds") List<String> sampleIds) {

        if (sampleIds == null || sampleIds.isEmpty()) {
            throw new ResourceException("At least one sample ID is needed", Response.Status.BAD_REQUEST);
        }

        Map<String, CrspPhiDTO> resourceResults = new HashMap<>();

        String queryResponse = getJerseyClient().resource(bspConfig.getWSUrl(SEARCH_CRSP_PHI))
                .queryParam("sample_ids", StringUtils.join(sampleIds, ","))
                .get(String.class);

        Response foundResponse;
        try {
            JAXBContext queryContext = JAXBContext.newInstance(CrspPhiInfo.class, CrspPhiDTO.class);
            Unmarshaller unmarshaller = queryContext.createUnmarshaller();
            XMLReader reader = XMLReaderFactory.createXMLReader();

            //Prepare the input
            InputSource is = new InputSource(new StringReader(queryResponse));

            //Create a SAXSource specifying the filter
            SAXSource source = new SAXSource(is);

            CrspPhiInfo queryInfo = (CrspPhiInfo)unmarshaller.unmarshal(source);

            if (queryInfo == null || queryInfo.getPhiData().isEmpty()) {
                throw new ResourceException("No results were found for the given samples", Response.Status.NOT_FOUND);
            }

            for (CrspPhiDTO phiDTO : queryInfo.getPhiData()) {
                resourceResults.put(phiDTO.getSampleID(), phiDTO);
            }
            foundResponse = Response.ok().entity(resourceResults).build();
        } catch (JAXBException | SAXException e) {
            foundResponse = Response.status(Response.Status.BAD_REQUEST).build();
            e.printStackTrace();
        }

        return foundResponse;
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, bspConfig);
    }
}

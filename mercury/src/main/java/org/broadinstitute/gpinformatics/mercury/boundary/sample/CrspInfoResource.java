package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import com.sun.jersey.api.client.Client;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.athena.entity.orders.ProductOrder;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.ResourceException;
import org.broadinstitute.gpinformatics.mercury.control.AbstractJerseyClientService;
import org.broadinstitute.gpinformatics.mercury.control.LoginAndPassword;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiDTO;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.CrspPhiInfo;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.servlet.ServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.security.AccessController;
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

    private static final String SEARCH_CRSP_PHI = "sample/getcrspphenotypes";
    private static final String SEARCH_CRSP_CLINICIAN_INFO = "getclinicianinfo";

    private static final Log logger = LogFactory.getLog(CrspInfoResource.class);

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private PortalConfig portalConfig;

    @Inject
    private ProductOrderDao productOrderDao;

    @Context
    SecurityContext sc;

    private LoginAndPassword currentConfig;

    @GET
    @Path("/getCrspPhiInfo")
//    @RolesAllowed({"CRSP-Mercury-ProjectManagers", "CRSP-Mercury-Developers"})
//    @PermitAll
    @Produces({MediaType.APPLICATION_JSON})
    public Response getCrspPhiInfo(@QueryParam("sampleIds") List<String> sampleIds,
                                   @QueryParam("reqId") String reqId) {

        if(!sc.isUserInRole("CRSP-Mercury-Developers") &&
           !sc.isUserInRole("CRSP-Mercury-WebServiceUser")) {
            throw new ResourceException(String.format("Unauthorized Access: user %s does not have access to PHI information",sc.getUserPrincipal().getName()),
                    Response.Status.FORBIDDEN);
        }

        currentConfig = portalConfig;
        if (sampleIds == null || sampleIds.isEmpty()) {
            throw new ResourceException("At least one sample ID is needed", Response.Status.BAD_REQUEST);
        }

        if(StringUtils.isBlank(reqId)) {
            throw new ResourceException("A Requisition identifier is needed to find Clinician information",
                    Response.Status.BAD_REQUEST);
        }

        ProductOrder order = productOrderDao.findByBusinessKey(reqId);

        String clinicianQueryResponse =
                getJerseyClient().resource(portalConfig.getWsUrl(PortalConfig.CRSP_PORTAL_NAME)+SEARCH_CRSP_CLINICIAN_INFO)
                .queryParam("requisition_identifier", order.getRequisitionKey()).get(String.class);

        CrspPhiInfo clinicianInfo = null;
        try {
            clinicianInfo = parseCrspPhiResponse(clinicianQueryResponse);
        } catch (JAXBException | SAXException e) {
            logger.error("Error parsing information retrieved for clinician information");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        CrspPhiDTO clinicianData = clinicianInfo.getPhiData().iterator().next();

        Map<String, CrspPhiDTO> resourceResults = new HashMap<>();

        clearClient();
        currentConfig = bspConfig;

        String queryResponse = getJerseyClient().resource(bspConfig.getWSUrl(SEARCH_CRSP_PHI))
                .queryParam("sample_ids", StringUtils.join(sampleIds, ","))
                .get(String.class);

        Response foundResponse;
        try {
            CrspPhiInfo queryInfo = parseCrspPhiResponse(queryResponse);

            if (queryInfo != null) {
                for (CrspPhiDTO phiDTO : queryInfo.getPhiData()) {

                    phiDTO.setClinicianName(clinicianData.getClinicianName());

                    phiDTO.setClinicianPhone(clinicianData.getClinicianPhone());
                    phiDTO.setClinicianFax(clinicianData.getClinicianFax());
                    phiDTO.setClinicianEmail(clinicianData.getClinicianEmail());

                    phiDTO.setClinicianAddress1(clinicianData.getClinicianAddress1());
                    phiDTO.setClinicianAddress2(clinicianData.getClinicianAddress2());
                    phiDTO.setClinicianCity(clinicianData.getClinicianCity());
                    phiDTO.setClinicianState(clinicianData.getClinicianState());
                    phiDTO.setClinicianZipCode(clinicianData.getClinicianZipCode());
                    phiDTO.setClinicianCountry(clinicianData.getClinicianCountry());

                    resourceResults.put(phiDTO.getSampleID(), phiDTO);
                }
            }
        } catch (JAXBException | SAXException e) {
            e.printStackTrace();
            logger.error("Error getting data from BSP call",e);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        foundResponse = Response.ok().entity(resourceResults).build();

        return foundResponse;
    }

    private CrspPhiInfo parseCrspPhiResponse(String queryResponse) throws JAXBException, SAXException {
        JAXBContext queryContext = JAXBContext.newInstance(CrspPhiInfo.class, CrspPhiDTO.class);
        Unmarshaller unmarshaller = queryContext.createUnmarshaller();
        XMLReader reader = XMLReaderFactory.createXMLReader();

        //Prepare the input
        InputSource is = new InputSource(new StringReader(queryResponse));

        //Create a SAXSource specifying the filter
        SAXSource source = new SAXSource(is);

        return (CrspPhiInfo) unmarshaller.unmarshal(source);
    }

    @Override
    protected void customizeClient(Client client) {
        specifyHttpAuthCredentials(client, currentConfig);
    }
}

package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.InformaticsServiceException;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;

/**
 * ClinicalResource provides web services related to accessioning samples into Mercury along with sample data used for
 * lab processing, diagnostics, and analysis of the data.
 */
@Path(ClinicalResource.CLINICAL_RESOURCE_PATH)
@Stateful
@RequestScoped
public class ClinicalResource {

    public static final String CLINICAL_RESOURCE_PATH = "clinical";

    private static final Log log = LogFactory.getLog(ClinicalResource.class);
    public static final String USERNAME = "username";
    public static final String MANIFEST_ID = "manifestId";
    public static final String CREATE_MANIFEST = "createManifestWithSamples";

    @Inject
    private UserBean userBean;

    @Inject
    private BSPConfig bspConfig;

    @Inject
    private PortalConfig portalConfig;

    @Inject
    private ProductOrderDao productOrderDao;

    @Context
    SecurityContext sc;

    @Inject
    private ManifestSessionEjb manifestSessionEjb;

    @SuppressWarnings("unused")
    public ClinicalResource() {}

    public ClinicalResource(UserBean userBean, ManifestSessionEjb manifestSessionEjb) {
        this.userBean = userBean;
        this.manifestSessionEjb = manifestSessionEjb;
    }

    /**
     * Creates an accessioning session for receiving sample tubes along with their sample data.
     *
     * Note that object wrappers are used instead of their primitive types so that we get null values instead of
     * default values.
     *
     * @param username              the user who is sending the sample data to Mercury
     * @param manifestName          a name for the session, which should indicate the source of the data (spreadsheet name, shipment ID, etc.)
     * @param researchProjectKey    the research project that the samples belong to
     * @param isFromSampleKit       true if the samples are in Broad-provided tubes; false if the samples are in collaborator tubes
     * @return a unique session ID to use as input to other web service calls
     */
    public long createAccessioningSession(String username, String manifestName, String researchProjectKey,
                                          Boolean isFromSampleKit) {
        validateManifestName(manifestName);
        validateIsFromSampleKit(isFromSampleKit);
        login(username);

        ManifestSession manifestSession = manifestSessionEjb.createManifestSession(researchProjectKey, manifestName,
                isFromSampleKit);

        return manifestSession.getManifestSessionId();
    }

    public void addSamplesToManifest(String username, Long manifestId, Collection<Sample> samples) {
        login(username);
        String requiredParameterMissing = "Required parameter \'%s\' is missing.";
        if (manifestId == null) {
            throw new InformaticsServiceException(String.format(requiredParameterMissing, "manifestId"));
        }
        if (CollectionUtils.isEmpty(samples)) {
            throw new InformaticsServiceException(String.format(requiredParameterMissing, "samples"));
        }
        manifestSessionEjb.addSamplesToManifest(manifestId, samples);
    }

    @POST
    @Path(CREATE_MANIFEST)
    @Produces(MediaType.APPLICATION_XML)
    public Response createManifestWithSamples(ClinicalResourceBean clinicalResourceBean) {
        validateManifestName(clinicalResourceBean.getManifestName());
        validateIsFromSampleKit(clinicalResourceBean.isFromSampleKit());
        login(clinicalResourceBean.getUsername());

        ManifestSession manifestSession = manifestSessionEjb
                .createManifestSession(clinicalResourceBean.getResearchProjectKey(),
                        clinicalResourceBean.getManifestName(), clinicalResourceBean.isFromSampleKit());
        manifestSessionEjb
                .addSamplesToManifest(manifestSession.getManifestSessionId(), clinicalResourceBean.getSamples());

        String resultString = String.format("Manifest Created with %s samples", clinicalResourceBean.getSamples().size() > 0 ?
                String.valueOf(clinicalResourceBean.getSamples().size()) : "no");

        return Response.status(Response.Status.OK).entity(clinicalResourceBean).type(MediaType.APPLICATION_XML_TYPE).build();
    }

    private void validateManifestName(String manifestName) {
        if (StringUtils.isBlank(manifestName)) {
            throw new IllegalArgumentException("manifestName is required.");
        }
    }

    private void validateIsFromSampleKit(Boolean isFromSampleKit) {
        if (isFromSampleKit == null) {
            throw new IllegalArgumentException("isFromSampleKit is required.");
        }
        if (!isFromSampleKit) {
            throw new UnsupportedOperationException(
                    "Samples in tubes other than from Broad sample kits are currently not supported.");
        }
    }

    private void login(String username) {
        userBean.login(username);
        if (!userBean.isValidBspUser()) {
            throw new UnknownUserException(username);
        }
    }
}

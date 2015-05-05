package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.presentation.UserBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;
import java.util.Collection;
import java.util.Set;

/**
 * ClinicalResource provides web services related to accessioning samples into Mercury along with sample data used for
 * lab processing, diagnostics, and analysis of the data.
 */
@Path(ClinicalResource.CLINICAL_RESOURCE_PATH)
@RequestScoped
public class ClinicalResource {

    public static final String CLINICAL_RESOURCE_PATH = "clinical";
    public static final String CREATE_MANIFEST = "createManifestWithSamples";
    public static final String SAMPLE_CONTAINS_NO_METADATA = "Sample contains no metadata.";
    public static final String SAMPLE_IS_NULL = "Sample is null.";
    public static final String EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED = "Empty list of samples not allowed.";
    public static final String REQUIRED_FIELD_MISSING = "Missing required sample metadata";

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
     * @param clinicalResourceBean    data bean containing request parameters for creating the manifest
     * @return a unique manifest ID
     */
    @POST
    @Path(CREATE_MANIFEST)
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces(MediaType.APPLICATION_JSON)
    public long createManifestWithSamples(ClinicalResourceBean clinicalResourceBean) {
        validateManifestName(clinicalResourceBean.getManifestName());
        validateIsFromSampleKit(clinicalResourceBean.isFromSampleKit());
        login(clinicalResourceBean.getUsername());

        validateSamples(clinicalResourceBean.getSamples());

        ManifestSession manifestSession = manifestSessionEjb
                .createManifestSession(clinicalResourceBean.getResearchProjectKey(),
                        clinicalResourceBean.getManifestName(), clinicalResourceBean.isFromSampleKit(),
                        clinicalResourceBean.getSamples());

        return manifestSession.getManifestSessionId();
    }

    private void validateSamples(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED);
        }

        if (samples.contains(null)) {
            throw new IllegalArgumentException(SAMPLE_IS_NULL);
        }
        for (Sample sample : samples) {
            int metadataCount=0;
            if (sample.getSampleData().isEmpty()) {
                throw new IllegalArgumentException(SAMPLE_CONTAINS_NO_METADATA);
            }
            for (SampleData sampleData : sample.getSampleData()) {
                if (StringUtils.isNotEmpty(sampleData.getValue())) {
                    metadataCount++;
                }
            }
            if (metadataCount==0) {
                throw new IllegalArgumentException(SAMPLE_CONTAINS_NO_METADATA);
            }
            if (!ClinicalSampleFactory.hasRequiredMetadata(sample.getSampleData())){
                Set<Metadata.Key> requiredFields = Metadata.Key.getRequiredFields();
                Collection<Object> missingFields = CollectionUtils.subtract(requiredFields, sample.getSampleData());
                throw new IllegalArgumentException(REQUIRED_FIELD_MISSING + ": " + missingFields);
            }
        }
    }

    private void validateManifestName(String manifestName) {
        if (StringUtils.isBlank(manifestName)) {
            throw new IllegalArgumentException("manifestName is required.");
        }
    }

    private void validateIsFromSampleKit(Boolean isFromSampleKit) {
        if (isFromSampleKit == null) {
            throw new IllegalArgumentException("fromSampleKit is required.");
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

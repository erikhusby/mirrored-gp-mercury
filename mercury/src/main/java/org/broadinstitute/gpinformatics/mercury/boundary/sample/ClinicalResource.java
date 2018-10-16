package org.broadinstitute.gpinformatics.mercury.boundary.sample;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.control.dao.orders.ProductOrderDao;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPConfig;
import org.broadinstitute.gpinformatics.infrastructure.portal.PortalConfig;
import org.broadinstitute.gpinformatics.mercury.boundary.UnknownUserException;
import org.broadinstitute.gpinformatics.mercury.boundary.manifest.ManifestSessionEjb;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.ClinicalResourceBean;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.Sample;
import org.broadinstitute.gpinformatics.mercury.crsp.generated.SampleData;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;
import org.broadinstitute.gpinformatics.mercury.entity.sample.ManifestSession;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.MaterialType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
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
    public static final String UNRECOGNIZED_MATERIAL_TYPE = "An unrecognized material type was entered";
    public static final Set<Metadata.Key> REQUIRED_METADATA_KEYS = EnumSet.of(Metadata.Key.MATERIAL_TYPE);

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

    @Inject
    private MercurySampleDao mercurySampleDao;

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

        List<MercurySample> mercurySamples =
                mercurySampleDao.findBySampleKeys(extractSampleIdsFromSamples(clinicalResourceBean.getSamples()));

        updateMetadataSource(mercurySamples);

        return manifestSession.getManifestSessionId();
    }

    private void updateMetadataSource(List<MercurySample> mercurySamples) {
        for (MercurySample mercurySample : mercurySamples) {
            mercurySample.changeMetadataSourceToCrspPortal();
        }
    }

    private List<String> extractSampleIdsFromSamples(List<Sample> samples) {
        List<String> sampleIds = new ArrayList<>(samples.size());
        for (Sample sample : samples) {
            Metadata.Key metadataKey = Metadata.Key.BROAD_SAMPLE_ID;
            sampleIds.add(metadataKey.getValueFor(sample));
        }

        return sampleIds;
    }

    private void validateSamples(Collection<Sample> samples) {
        if (samples.isEmpty()) {
            throw new IllegalArgumentException(EMPTY_LIST_OF_SAMPLES_NOT_ALLOWED);
        }

        if (samples.contains(null)) {
            throw new IllegalArgumentException(SAMPLE_IS_NULL);
        }
        List<String> invalidMaterialTypes = new ArrayList<>();
        for (Sample sample : samples) {
            int metadataCount=0;
            if (sample.getSampleData().isEmpty()) {
                throw new IllegalArgumentException(SAMPLE_CONTAINS_NO_METADATA);
            }
            for (SampleData sampleData : sample.getSampleData()) {
                if (StringUtils.isNotEmpty(sampleData.getValue())) {
                    metadataCount++;
                }
                if (Metadata.Key.valueOf(sampleData.getName()) == Metadata.Key.MATERIAL_TYPE
                                && !MaterialType.isValid(sampleData.getValue())) {
                    invalidMaterialTypes.add(sampleData.getValue());
                }
            }
            if (metadataCount==0) {
                throw new IllegalArgumentException(SAMPLE_CONTAINS_NO_METADATA);
            }
            if (!hasRequiredMetadata(sample.getSampleData())) {
                Collection<String> missingFields=getMissingFields(sample.getSampleData());
                throw new IllegalArgumentException(REQUIRED_FIELD_MISSING + ": " + missingFields);
            }
        }
        if(!invalidMaterialTypes.isEmpty()) {
            throw new IllegalArgumentException(UNRECOGNIZED_MATERIAL_TYPE + ": " + StringUtils.join(invalidMaterialTypes, ", "));
        }
    }

    /**
      * Test if provided sampleData includes all required fields.
      *
      * @return True if provided sampleData includes all required fields.<br/>
      *         False if any required fields are missing.
      */
    public static boolean hasRequiredMetadata(List<SampleData> sampleData) {
        return getMissingFields(sampleData).isEmpty();
    }

    /**
     * Find all required fields that are missing from sampleData
     */
    private static Collection<String> getMissingFields(List<SampleData> sampleData) {
        Set<String> includedRequiredFields = new HashSet<>();
        for (SampleData data : sampleData) {
            if (REQUIRED_METADATA_KEYS.contains(Metadata.Key.valueOf(data.getName()))) {
                if (StringUtils.isNotBlank(data.getValue())) {
                    includedRequiredFields.add(data.getName());
                }
            }
        }
        return CollectionUtils.subtract(getRequiredFieldNames(), includedRequiredFields);
    }

    private static Set<String> getRequiredFieldNames() {
        Set<String> requiredFieldNames = new HashSet<>(REQUIRED_METADATA_KEYS.size());
        for (Metadata.Key requiredMetadataKey : REQUIRED_METADATA_KEYS) {
            requiredFieldNames.add(requiredMetadataKey.name());
        }
        return requiredFieldNames;
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

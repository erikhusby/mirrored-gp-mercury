package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Deployment;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.util.Set;

public class CrspPipelineUtils {

    private Deployment deployment;

    @Inject
    public CrspPipelineUtils(@Nonnull Deployment deployment) {
        this.deployment = deployment;
    }

    /**
     * Returns true if all samples are considered
     * crsp samples.  Throws an exception if there's a
     * mix of crps and non-crsp samples.
     */
    public  boolean areAllSamplesForCrsp(Set<SampleInstanceV2> sampleInstances) {
        boolean hasAtLeastOneCrspSample = false;
        boolean hasNonCrspSamples = false;

        for (SampleInstanceV2 sampleInstance : sampleInstances) {
            if (sampleInstance.getSingleBucketEntry() != null) {
                ResearchProject.RegulatoryDesignation regulatoryDesignation = sampleInstance.getSingleBucketEntry().
                        getProductOrder().getResearchProject().getRegulatoryDesignation();
                if (regulatoryDesignation == ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP ||
                        regulatoryDesignation == ResearchProject.RegulatoryDesignation.CLINICAL_DIAGNOSTICS) {
                    hasAtLeastOneCrspSample = true;
                }
                else {
                    hasNonCrspSamples = true;
                }
            }
        }
        if (hasAtLeastOneCrspSample && hasNonCrspSamples) {
            throw new RuntimeException("Samples contain a mix of CRSP and non-CRSP samples.");
        }
        else {
            return hasAtLeastOneCrspSample;
        }
    }

    /**
     * Overrides various fields with CRSP-specific values
     */
    public void setFieldsForCrsp(
            LibraryBean libraryBean,
            SampleData sampleData) {
        throwExceptionIfInProductionAndSampleIsNotABSPSample(sampleData.getSampleId());
        setBuickVisitAndCollectionDate(libraryBean, sampleData);

        libraryBean.setLsid(getCrspLSIDForBSPSampleId(sampleData.getSampleId()));
        libraryBean.setRootSample(libraryBean.getSampleId());
        libraryBean.setTestType(LibraryBean.CRSP_SOMATIC_TEST_TYPE);
    }

    private void setBuickVisitAndCollectionDate(LibraryBean libraryBean, SampleData sampleData) {
        if (sampleData instanceof MercurySampleData) {
            MercurySampleData mercurySampleData = (MercurySampleData)sampleData;
            libraryBean.setBuickVisit(mercurySampleData.getVisit());
            libraryBean.setBuickCollectionDate(mercurySampleData.getCollectionDate());
        }
        // else don't throw an exception because it's unclear whether these
        // fields are actually required for pipeline processing and we
        // don't want to make the pipeline explode for no good reason
    }

    private void throwExceptionIfInProductionAndSampleIsNotABSPSample(@Nonnull String sampleId) {
        if (deployment == Deployment.PROD) {
            if (!BSPUtil.isInBspFormat(sampleId)) {
                throw new RuntimeException("Sample " + sampleId + " does not appear to be a BSP sample.  " +
                        "The pipeline's fingerprint validation can only handle BSP samples.");
            }
        }
    }

    /**
     * Generates a synthetic CRSP lsid because the CRSP pipeline
     * needs the LSID in this format.  No explicit BSP format check
     * is done here to allow for flexibility in test data.
     */
    public String getCrspLSIDForBSPSampleId(@Nonnull String bspSampleId) {
        return bspSampleId.replaceFirst("S[MP]-", "org.broadinstitute:crsp:");
    }

}

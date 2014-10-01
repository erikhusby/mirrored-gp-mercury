package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;

import javax.annotation.Nonnull;
import java.util.Set;

public class CrspPipelineUtils {

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
                if (sampleInstance.getSingleBucketEntry().getProductOrder().getResearchProject().getRegulatoryDesignation() == ResearchProject.RegulatoryDesignation.GENERAL_CLIA_CAP) {
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
    public void setFieldsForCrsp(LibraryBean libraryBean,
                                 SampleData sampleData,
                                 ResearchProject positiveControlsProject,
                                 String lcSet) {
        libraryBean.setLsid(getCrspLSIDForBSPSampleId(sampleData.getSampleId()));
        libraryBean.setRootSample(libraryBean.getSampleId());

        if (Boolean.TRUE.equals(libraryBean.isPositiveControl())) {
            String mungedSampleName = libraryBean.getCollaboratorSampleId() + "_" + lcSet;
            libraryBean.setCollaboratorSampleId(mungedSampleName);
            libraryBean.setCollaboratorParticipantId(mungedSampleName);

            libraryBean.setResearchProjectId(positiveControlsProject.getBusinessKey());
            libraryBean.setResearchProjectName(positiveControlsProject.getTitle());
            libraryBean.setRegulatoryDesignation(positiveControlsProject.getRegulatoryDesignationCodeForPipeline());
        }
    }

    /**
     * Generates a synthetic CRSP lsid because the CRSP pipeline
     * needs the LSID in this format.  No explicit BSP format check
     * is done here to allow for flexibility in test data.
     */
    public String getCrspLSIDForBSPSampleId(@Nonnull String bspSampleId) {
        if (bspSampleId.length() < 3) {
            throw new RuntimeException("Cannot transform non-BSP sample id " + bspSampleId + " into CRSP lsid format");
        }
        return "org.broadinstitute:crsp:" + bspSampleId.substring(3);
    }

    /**
     * Returns the hardcoded research project id (RP-XYZ)
     * used for aggregating positive controls for CRSP
     */
    public String getResearchProjectForCrspPositiveControls() {
        // have michael or cassie make an RP, then hardcode it here
        return "RP-1205";
    }
}

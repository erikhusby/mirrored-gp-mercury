package org.broadinstitute.gpinformatics.mercury.boundary.zims;

import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.infrastructure.SampleData;
import org.broadinstitute.gpinformatics.mercury.control.dao.run.AttributeArchetypeDao;
import org.broadinstitute.gpinformatics.mercury.entity.infrastructure.KeyValueMapping;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstanceV2;
import org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean;
import org.broadinstitute.gpinformatics.mercury.samples.MercurySampleData;

import javax.annotation.Nonnull;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

import static org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean.CRSP_LSID_PREFIX;
import static org.broadinstitute.gpinformatics.mercury.entity.zims.LibraryBean.MERCURY_LSID_PREFIX;

@Dependent
public class CrspPipelineUtils {

    @Inject
    private AttributeArchetypeDao attributeArchetypeDao;

    /**
     * Returns true if all samples are considered
     * crsp samples.  Throws an exception if there's a
     * mix of crps and non-crsp samples.
     * @param sampleInstances the sample instances for a lane
     * @param mixedLaneOk whether the pipeline accepts mixtures of clinical and research samples; true for genomes
     */
    public boolean areAllSamplesForCrsp(Set<SampleInstanceV2> sampleInstances, boolean mixedLaneOk) {
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
        if (!mixedLaneOk && hasAtLeastOneCrspSample && hasNonCrspSamples) {
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
            SampleData sampleData,
            String bait) {
        if (sampleData.getMetadataSource() != MercurySample.MetadataSource.BSP) {
            setBuickVisitAndCollectionDate(libraryBean, sampleData);
            libraryBean.setLsid(getCrspLSIDForBSPSampleId(sampleData.getSampleId()));
            libraryBean.setRootSample(libraryBean.getSampleId());
            libraryBean.setTestType(LibraryBean.CRSP_SOMATIC_TEST_TYPE);
        }

        if (Boolean.TRUE.equals(libraryBean.isPositiveControl())) {
            if (bait != null) {
                /*
                 * The clinical pipeline requires a product part number, but controls are
                 * not associated with PDOs, so derive the product from the bait.
                 */
                // todo required by clinical Exome pipeline, not sure about genomes
                Map<String, String> mapBaitToProductPartNumber =
                        attributeArchetypeDao.findKeyValueMap(KeyValueMapping.BAIT_PRODUCT_MAPPING);
                libraryBean.setProductPartNumber(mapBaitToProductPartNumber.get(bait));
            }
        }
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

    /**
     * Generates a synthetic CRSP lsid because the CRSP pipeline
     * needs the LSID in this format.  No explicit BSP format check
     * is done here to allow for flexibility in test data.
     */
    public static String getCrspLSIDForBSPSampleId(@Nonnull String bspSampleId) {
        if (bspSampleId.startsWith("SM-") || bspSampleId.startsWith("SP-")) {
            return bspSampleId.replaceFirst("S[MP]-", CRSP_LSID_PREFIX);
        } else {
            // other options are "GS-" and All of Us 10 digit barcodes
            return MERCURY_LSID_PREFIX + bspSampleId;
        }
    }

    /** Setter used for dbfree testing. */
    public void setAttributeArchetypeDao(AttributeArchetypeDao attributeArchetypeDao) {
        this.attributeArchetypeDao = attributeArchetypeDao;
    }
}

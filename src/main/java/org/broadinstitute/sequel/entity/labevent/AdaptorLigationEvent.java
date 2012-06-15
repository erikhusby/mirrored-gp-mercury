package org.broadinstitute.sequel.entity.labevent;

import org.broadinstitute.sequel.entity.project.BasicProjectPlan;
import org.broadinstitute.sequel.entity.project.ProjectPlan;
import org.broadinstitute.sequel.entity.vessel.LabMetric;
import org.broadinstitute.sequel.entity.vessel.LabMetricRange;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.vessel.MolecularEnvelope;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.JiraCommentUtil;

/**
 * A very early thought about how one might
 * write a {@link LabEvent}.
 * 
 * This is outdated.  Look at the {@link LabEvent} in
 * {@link GenericLabEvent} to
 * get a different (perhaps better) take on this.
 */
public class AdaptorLigationEvent extends LabEvent  {

    private LabEventConfiguration eventConfiguration;

    private MolecularEnvelope adaptor;


    public AdaptorLigationEvent(LabEventConfiguration eventConfig,MolecularEnvelope adaptor) {
        this.eventConfiguration = eventConfig;
        this.adaptor = adaptor;
    }

    @Override
    public LabEventName getEventName() {
       return LabEventName.ADAPTOR_LIGATION;
    }

    /**
     * Sources ar expected to have sample information
     * but no adaptor.
     * @throws InvalidMolecularStateException
     */
    @Override
    public void validateSourceMolecularState() throws InvalidMolecularStateException {
        for (LabVessel tangible: getSourceLabVessels()) {
                if (tangible.getSampleInstances().isEmpty()) {
                    throw new InvalidMolecularStateException("No sample sheet");
                }
                for (SampleInstance sampleInstance: tangible.getSampleInstances()) {
                    if (sampleInstance.getStartingSample() == null) {
                        throw new InvalidMolecularStateException("No source sample");
                    }
                    MolecularEnvelope molEnvelope = sampleInstance.getMolecularState().getMolecularEnvelope();
                    // if we have pooling, we expect indexes
                    if (tangible.getSampleInstances().size() > 1) {
                        if (molEnvelope == null) {
                            for (ProjectPlan projectPlan : sampleInstance.getAllProjectPlans()) {
                                Project p = projectPlan.getProject();
                                // is this a fatal error?  or just an alert?
                                // do we throw the exception?  or alert?  or both?
                                p.sendAlert("No index for " + sampleInstance.getStartingSample().getSampleName() + " in " + getEventName());
                            }
                        }
                    }

                    float concentration  = sampleInstance.getMolecularState().getConcentration().floatValue();
                    if (concentration < eventConfiguration.getExpectedMolecularState().getMinConcentration()) {
                        JiraCommentUtil.postUpdate("Concentration Update", "Concentration " + concentration + " is out of range for " + tangible.getLabCentricName(), tangible);
                    }

                    if (!sampleInstance.getMolecularState().getMolecularEnvelope().equals(eventConfiguration.getExpectedMolecularState().getMolecularEnvelope())) {
                        throw new InvalidMolecularStateException("Molecular envelope is wrong!");
                    }

                    for (LabMetricRange thresholds: eventConfiguration.getExpectedMolecularState().getDisastrousMetricRanges()) {
                        LabMetric someMetric = tangible.getMetric(thresholds.getMetricName(),
                                LabVessel.MetricSearchMode.NEAREST,
                                sampleInstance);
                        if (!someMetric.isInRange(thresholds)) {
                            JiraCommentUtil.postUpdate("Concentration update", thresholds.getMetricName() + " disaster for " + tangible.getLabCentricName(), tangible);
                        }
                    }
                }
        }
    }

    @Override
    public void validateTargetMolecularState() throws InvalidMolecularStateException {
        for (LabVessel tangible: getTargetLabVessels()) {
             if (eventConfiguration.getOutputMode() == LabEventConfiguration.OutputMaterialMode.NEW_LIBRARY) {
                throw new InvalidMolecularStateException("There's already a sample sheet; I expected empty destinations");
            }
        }
    }

    @Override
    /**
     * Dead example. See EndToEndTest.
     */
    public void applyMolecularStateChanges() throws InvalidMolecularStateException {
        
    }

}

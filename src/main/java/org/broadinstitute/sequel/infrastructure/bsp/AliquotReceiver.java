package org.broadinstitute.sequel.infrastructure.bsp;

import org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt;
import org.broadinstitute.sequel.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.sequel.entity.bsp.BSPStartingSample;
import org.broadinstitute.sequel.entity.labevent.LabEventName;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.project.Starter;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.StartingSample;
import org.broadinstitute.sequel.entity.vessel.BSPSampleAuthorityTwoDTube;
import org.broadinstitute.sequel.entity.vessel.LabVessel;
import org.broadinstitute.sequel.entity.project.Project;

/**
 * Service called when a third party service
 * wants to register an aliquot with us.
 * 
 * Possible implementations: 
 * 1. BSP push of samples.
 * 2. UI that lets collaborators register
 * aliquots for any action
 * 3. Another UI where our dev lab can
 * register non-BSP "aliquots", for dev aliquots
 */
public class AliquotReceiver {

    /**
     * Registers the aliquot with GSP for sequencing.
     * GSP has to know what the sourceSampleLabel is to
     * ensure we have enough metadata for sequence
     * analysis, submission, billing, etc.
     *
     * We assume at this point that a prior operation
     * has register the source sample with GSP, although
     * it's possible that an implementation might immediately
     * turn around and query some other caller-provided
     * service to get more metadata about source sample.
     *
     * In other words, it's possible that when this method
     * is called from BSP, we have no clue what sourceSampleLabel
     * is, in which case we'd immediately call a BSP
     * API to lookup the metadata we need.  Probably this
     * approach is very inefficient, but the point is that
     * we can change the implementation over time.
     */
    public void receiveAliquot(String sourceBarcode,
                               String aliquotBarcode,
                               String project,
                               Float volume,
                               Float concentration) {

        /**
         * injected DAO translates strings to objects,
         * then delegates to {@link #receiveAliquot(Goop, org.broadinstitute.sequel.entity.bsp.BSPPlatingReceipt, float, float)}
         */
        throw new RuntimeException("Method not yet implemented.");
    }

    //TODO .. aliquot should be Starter rather than BSPSampleAuthorityTwoDTube
    public BSPPlatingRequest receiveAliquot(StartingSample source,
                                            LabVessel aliquot,
                                            BSPPlatingReceipt receipt) {
        BSPPlatingRequest platingRequest = resolveAliquotToPlatingRequest(source,aliquot,receipt);

        for (Project project : aliquot.getAllProjects()) {
            project.addJiraComment("Aliquot " + aliquot.getLabel() + " derived from " + source.getLabel() + " has been received.");
        }

        aliquot.logNote(new StatusNote(LabEventName.ALIQUOT_RECEIVED));
        return platingRequest;
    }

    public BSPPlatingRequest receiveAliquot(LabVessel source,
                                            LabVessel aliquot,
                                            BSPPlatingReceipt receipt) {
        BSPPlatingRequest platingRequest = resolveAliquotToPlatingRequest(source,aliquot,receipt);

        for (Project project : aliquot.getAllProjects()) {
            project.addJiraComment("Aliquot " + aliquot.getLabCentricName() + " derived from " + source.getLabCentricName() + " has been received.");
        }

        aliquot.logNote(new StatusNote(LabEventName.ALIQUOT_RECEIVED));
        return platingRequest;
    }

    /**
     * At the moment we get one {@link BSPPlatingReceipt receipt} per batch
     * of aliquots.  In other words, we get a {@link BSPPlatingReceipt receipt} for
     * each plate, not for each {@link Goop}.  Ideally this will
     * change so we'll be able to map more reliably between an {@link BaseGoop} and
     * a {@link BSPPlatingRequest}, and thereby know more accurately
     * what the {@link org.broadinstitute.sequel.entity.project.Project} the {@link BaseGoop} is for.
     * 
     * In the meantime, we guess a bit with volumes and concentration.
     * @param aliquot
     * @param platingReceipt
     * @return
     */
    private BSPPlatingRequest resolveAliquotToPlatingRequest(LabVessel source,
                                                             LabVessel aliquot,
                                                             BSPPlatingReceipt platingReceipt) {
        if (aliquot == null) {
             throw new IllegalArgumentException("aliquot must be non-null in AliquotReceiver.resolveAliquotToPlatingRequest");
        }
        if (platingReceipt == null) {
             throw new IllegalArgumentException("platingReceipt must be non-null in AliquotReceiver.resolveAliquotToPlatingRequest");
        }
        BSPPlatingRequest platingRequest = null;

        for (BSPPlatingRequest possibleRequest : platingReceipt.getPlatingRequests()) {
            if (!possibleRequest.isFulfilled()) {
                String requestedSource = possibleRequest.getSampleName();
                if (source.getLabel().equalsIgnoreCase(requestedSource)) {
                    // one could argue that we should also do a "best match"
                    // across concentration and volume, but I don't think that
                    // matters here because we're not linking any LC or sequencing
                    // instructions with the aliquot yet.  We're just requesting
                    // particular aliquots.
                    possibleRequest.setFulfilled(true);
                    for (SampleInstance sampleInstance: aliquot.getSampleInstances()) {
                        sampleInstance.getStartingSample().setRootProjectPlan(possibleRequest.getAliquotParameters().getProjectPlan());
                    }

                    platingRequest = possibleRequest;

                    break;
                }
            }
        }
        return platingRequest;
    }

    /**
     *
     * @param source
     * @param aliquot
     * @param platingReceipt
     * @return
     */
    private BSPPlatingRequest resolveAliquotToPlatingRequest(StartingSample source,
                                                             Starter aliquot,
                                                             BSPPlatingReceipt platingReceipt) {
        if (aliquot == null) {
            throw new IllegalArgumentException("aliquot must be non-null in AliquotReceiver.resolveAliquotToPlatingRequest");
        }
        if (platingReceipt == null) {
            throw new IllegalArgumentException("platingReceipt must be non-null in AliquotReceiver.resolveAliquotToPlatingRequest");
        }
        BSPPlatingRequest platingRequest = null;

        for (BSPPlatingRequest possibleRequest : platingReceipt.getPlatingRequests()) {
            if (!possibleRequest.isFulfilled()) {
                String requestedSource = possibleRequest.getSampleName();
                if (source.getLabel().equalsIgnoreCase(requestedSource)) {
                    // one could argue that we should also do a "best match"
                    // across concentration and volume, but I don't think that
                    // matters here because we're not linking any LC or sequencing
                    // instructions with the aliquot yet.  We're just requesting
                    // particular aliquots.
                    possibleRequest.setFulfilled(true);
                    for (SampleInstance sampleInstance: aliquot.getSampleInstances()) {
                        sampleInstance.getStartingSample().setRootProjectPlan(possibleRequest.getAliquotParameters().getProjectPlan());
                    }

                    platingRequest = possibleRequest;

                    break;
                }
            }
        }
        return platingRequest;
    }

}

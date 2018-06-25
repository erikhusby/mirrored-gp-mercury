package org.broadinstitute.gpinformatics.infrastructure.bsp.plating;

import org.broadinstitute.gpinformatics.mercury.entity.bsp.BSPPlatingRequest;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;
import java.util.List;

@Stub
@Alternative
@Dependent
public class BSPPlatingRequestServiceStub implements BSPPlatingRequestService {

    public BSPPlatingRequestServiceStub(){}

    @Override
    public BSPPlatingRequestResult createPlatingRequest(BSPPlatingRequestOptions options, String login, String platingRequestName, List<SeqWorkRequestAliquot> aliquots, List<ControlWell> controlWells, String comments, String seqTechnology, String humanReadableBarcode) {
        BSPPlatingRequestResult result = new BSPPlatingRequestResult();
        result.setPlatingRequestReceipt("WR-1234");
        result.setPlatingRequestSubmitted(false);
        return result;
    }

    @Override
    public BSPPlatingRequestResult updatePlatingRequest(String platingRequestReceipt, BSPPlatingRequestOptions options, String login, List<SeqWorkRequestAliquot> aliquots, List<ControlWell> controlWells) {
        BSPPlatingRequestResult result = new BSPPlatingRequestResult();
        result.setPlatingRequestReceipt("WR-1234");
        result.setPlatingRequestSubmitted(false);
        return result;
    }

    @Override
    public BSPPlatingRequestOptions getBSPPlatingRequestDefaultOptions() {
        BSPPlatingRequestOptions defaultPlatingRequestOptions = new BSPPlatingRequestOptions(
                BSPPlatingRequestOptions.HighConcentrationOption.VOLUME_FIRST,
                BSPPlatingRequestOptions.PlatformAndProcess.ILLUMINA_HYBRID_SELECTION_WGS_FRAGMENT_180BP,
                BSPPlatingRequestOptions.PlateType.Matrix96SlotRackSC05,
                BSPPlatingRequestOptions.TubeType.MatrixTubeSC05,
                BSPPlatingRequestOptions.AllowLessThanOne.NO,
                BSPPlatingRequestOptions.CancerProject.NO);

        return defaultPlatingRequestOptions;
    }

    @Override
    public BSPPlatingRequestResult issueBSPPlatingRequest(BSPPlatingRequestOptions options, List<BSPPlatingRequest> requests, List<ControlWell> controlWells, String login, String platingRequestName, String comments, String seqTechnology, String humanReadableBarcode) throws Exception {
        BSPPlatingRequestResult result = new BSPPlatingRequestResult();
        result.setPlatingRequestReceipt("WR-1234");
        result.setPlatingRequestSubmitted(false);
        return result;
    }

}

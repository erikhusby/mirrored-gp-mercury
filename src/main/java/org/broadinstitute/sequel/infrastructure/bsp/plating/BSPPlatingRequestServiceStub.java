package org.broadinstitute.sequel.infrastructure.bsp.plating;

import org.broadinstitute.sequel.entity.project.JiraTicket;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchColumn;
import org.broadinstitute.sequel.infrastructure.bsp.BSPSampleSearchService;
import org.broadinstitute.sequel.infrastructure.deployment.Stub;

import java.util.*;

@Stub
public class BSPPlatingRequestServiceStub implements BSPPlatingRequestService {
    @Override
    public BSPPlatingRequestResult createPlatingRequest(BSPPlatingRequestOptions options, String login, String platingRequestName, List<SeqWorkRequestAliquot> aliquots, List<ControlWell> controlWells, String comments, String seqTechnology, String humanReadableBarcode) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BSPPlatingRequestResult updatePlatingRequest(String platingRequestReceipt, BSPPlatingRequestOptions options, String login, List<SeqWorkRequestAliquot> aliquots, List<ControlWell> controlWells) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setSquidWorkRequestId(String platingRequestReceipt, long squidWorkRequestId) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void setHumanReadableBarcode(String platingRequestReceipt, int plateIndex, String humanReadableText) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BSPPlatingRequestResult tryAgain(String platingRequestReceipt) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String generateLinkToBSPPlatingRequestPage(String platingRequestReceipt) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public String getLcSetJiraKey(JiraTicket jiraTicket) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BSPPlatingRequestOptions getBSPPlatingRequestDefaultOptions() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}

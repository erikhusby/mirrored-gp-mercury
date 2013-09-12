package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.bsp.client.util.MessageCollection;
import org.broadinstitute.gpinformatics.mercury.boundary.sample.ReceiveSamplesEjb;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;

@UrlBinding(ReceiveSamplesActionBean.URL)
public class ReceiveSamplesActionBean extends CoreActionBean {

    public static final String URL = "/sample/receiveSamples.action";
    public static final String SHOW_RECEIPT_JSP = "/sample/sample_receipt.jsp";
    public static final String SHOW_RECEIPT_ACTION = "showRecept";
    public static final String RECEIVE_SAMPLES_ACTION = "receiveSamples";

    private List<String> samplesToReceive;

    @Inject
    private ReceiveSamplesEjb receiveSamplesEjb;

    @DefaultHandler
    @HandlesEvent(SHOW_RECEIPT_ACTION)
    public Resolution showReceipt() {
        return new ForwardResolution(SHOW_RECEIPT_JSP);
    }

    @HandlesEvent(RECEIVE_SAMPLES_ACTION)
    public Resolution receiveSamples() {

        MessageCollection messageCollection = receiveSamplesEjb.receiveSamples(samplesToReceive, getUserBean().getBspUser().getUsername());

        addMessages(messageCollection);

        return showReceipt();
    }

    public List<String> getSamplesToReceive() {
        return samplesToReceive;
    }

    public void setSamplesToReceive(List<String> samplesToReceive) {
        this.samplesToReceive = samplesToReceive;
    }
}

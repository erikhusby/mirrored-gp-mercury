package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

@UrlBinding("/workflow/LinkDenatureTubeToFlowcell.action")
public class LinkDenatureTubeToFlowcellActionBean extends CoreActionBean {
    private static final String VIEW_PAGE = "/resources/workflow/link_dtube_to_fc.jsp";
    private String denatureTubeBarcode;
    private String flowcellBarcode;

    public String getFlowcellBarcode() {
        return flowcellBarcode;
    }

    public void setFlowcellBarcode(String flowcellBarcode) {
        this.flowcellBarcode = flowcellBarcode;
    }

    public String getDenatureTubeBarcode() {
        return denatureTubeBarcode;
    }

    public void setDenatureTubeBarcode(String denatureTubeBarcode) {
        this.denatureTubeBarcode = denatureTubeBarcode;
    }

    @DefaultHandler
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public ForwardResolution save() {
        return new ForwardResolution(VIEW_PAGE);
    }
}

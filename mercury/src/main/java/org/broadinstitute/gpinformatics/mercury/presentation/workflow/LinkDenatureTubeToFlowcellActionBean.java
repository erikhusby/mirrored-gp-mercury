package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;

@UrlBinding("/workflow/LinkDenatureTubeToFlowcell.action")
public class LinkDenatureTubeToFlowcellActionBean extends CoreActionBean {
    private static String VIEW_PAGE = "/resources/workflow/link_dtube_to_fc.jsp";
    private static String DTUBE_INFO_PAGE = "/resources/workflow/denature_tube_info.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    private String denatureTubeBarcode;
    private String flowcellBarcode;
    private LabVessel denatureTube;

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

    public LabVessel getDenatureTube() {
        return denatureTube;
    }

    public void setDenatureTube(LabVessel denatureTube) {
        this.denatureTube = denatureTube;
    }

    @DefaultHandler
    public ForwardResolution showPage() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SAVE_ACTION)
    public ForwardResolution save() {
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent("denatureInfo")
    public ForwardResolution denatureTubeInfo() {
        denatureTube = labVesselDao.findByIdentifier(denatureTubeBarcode);
        return new ForwardResolution(DTUBE_INFO_PAGE);
    }
}

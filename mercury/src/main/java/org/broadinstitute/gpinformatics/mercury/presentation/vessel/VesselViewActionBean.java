package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;

@UrlBinding(value = "/view/vesselView.action")
public class VesselViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/container/vesselView.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    private String vesselLabel;

    private LabVessel vessel;

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    @DefaultHandler
    public Resolution view() {
        if (vesselLabel != null) {
            this.vessel = labVesselDao.findByIdentifier(vesselLabel);
        }
        return new ForwardResolution(VIEW_PAGE);
    }
}

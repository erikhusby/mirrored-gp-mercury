package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.Validate;
import net.sourceforge.stripes.validation.ValidationMethod;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.inject.Inject;


@UrlBinding(value = "/view/metricsView.action")
public class MetricsViewActionBean extends HeatMapActionBean {
    private static final String VIEW_PAGE = "/vessel/vessel_metrics_view.jsp";

    private static final String SEARCH_ACTION = "search";

    @Inject
    private LabVesselDao labVesselDao;

    @Validate(required = true, on = {SEARCH_ACTION})
    private String labVesselIdentifier;

    private LabVessel labVessel;

    @DefaultHandler
    @HandlesEvent(VIEW_ACTION)
    public Resolution view() {
        if (labVesselIdentifier != null) {
            labVessel = labVesselDao.findByIdentifier(labVesselIdentifier);
            buildHeatMap();
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    @HandlesEvent(SEARCH_ACTION)
    public Resolution search() {
        buildHeatMap();
        return new ForwardResolution(VIEW_PAGE);
    }

    @ValidationMethod(on = SEARCH_ACTION)
    public void validateData() {
        setLabVessel(labVesselDao.findByIdentifier(labVesselIdentifier));
        if (getLabVessel() == null) {
            addValidationError("labVesselIdentifier", "Could not find lab vessel " + labVesselIdentifier);
        }
    }

    private void buildHeatMap() {
        setHeatMapFieldString("TestHeatMapFields");
    }

    public String getLabVesselIdentifier() {
        return labVesselIdentifier;
    }

    public void setLabVesselIdentifier(String labVesselIdentifier) {
        this.labVesselIdentifier = labVesselIdentifier;
    }

    public LabVessel getLabVessel() {
        return labVessel;
    }

    public void setLabVessel(LabVessel labVessel) {
        this.labVessel = labVessel;
    }
}

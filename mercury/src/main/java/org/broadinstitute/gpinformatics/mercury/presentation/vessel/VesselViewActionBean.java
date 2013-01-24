package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.List;
import java.util.Set;

@UrlBinding(value = "/view/vesselView.action")
public class VesselViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/resources/container/vesselView.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    private String vesselLabel;

    private LabVessel vessel;
    private SampleInstance selectedCells;

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

    public List<SampleInstance> samplesAtPosition(String rowName, String columnName) {
        List<SampleInstance> sampleInstances;
        VesselPosition position = VesselPosition.getByName(rowName + columnName);
        VesselContainer<?> vesselContainer = vessel.getContainerRole();
        if (vesselContainer != null) {
            sampleInstances = vesselContainer.getSampleInstancesAtPositionList(position);
        } else {
            sampleInstances = vessel.getSampleInstancesList();
        }
        return sampleInstances;
    }

    public Set<SampleInstance> getSelectedCells() {
       return this.vessel.getAllSamples();
    }
}

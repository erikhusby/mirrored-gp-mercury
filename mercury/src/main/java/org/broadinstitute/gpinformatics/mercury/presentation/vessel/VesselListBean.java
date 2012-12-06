package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@ViewScoped
public class VesselListBean implements Serializable {

    @Inject
    private BSPUserList bspUserList;
    @Inject
    private LabVesselDao labVesselDao;

    private Boolean showPlateLayout = false;
    private Boolean showSampleList = false;
    private LabVessel selectedVessel;
    private List<LabVessel> foundVessels;

    public void updateVessels(List<LabVessel> vessels) {
        showPlateLayout = false;
        showSampleList = false;
        foundVessels = new ArrayList<LabVessel>();
        for (LabVessel vessel : vessels) {
            foundVessels.add(labVesselDao.findByIdentifier(vessel.getLabel()));
        }
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }

    public Boolean getShowPlateLayout() {
        return showPlateLayout;
    }

    public void setShowPlateLayout(Boolean showPlateLayout) {
        this.showPlateLayout = showPlateLayout;
    }

    public Boolean getShowSampleList() {
        return showSampleList;
    }

    public void setShowSampleList(Boolean showSampleList) {
        this.showSampleList = showSampleList;
    }

    public void togglePlateLayout(LabVessel vessel) {
        selectedVessel = vessel;
        showPlateLayout = !showPlateLayout;
    }

    public void toggleSampleList(LabVessel vessel) {
        selectedVessel = vessel;
        showSampleList = !showSampleList;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public String getOpenCloseValue(Boolean shown) {
        String value = "Open";
        if (shown) {
            value = "Close";
        }
        return value;
    }
}

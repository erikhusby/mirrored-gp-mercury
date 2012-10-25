package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import com.sun.jersey.core.util.StringIgnoreCaseKeyComparator;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ManagedBean
@RequestScoped
public class ContainerBean implements Serializable {
    @Inject
    private RackOfTubesDao rackOfTubesDao;

    private LabVessel container;

    public LabVessel getContainer() {
        return container;
    }

    public void setContainer(LabVessel container) {
        this.container = container;
    }

    public void loadVessel(LabVessel container){
        this.container = rackOfTubesDao.getByLabel(container.getLabel());
    }

    public int getColumns(){
        int columns = 1;
        if(container != null){
            columns = ((RackOfTubes) container).getRackType().getVesselGeometry().getColumnNames().length;
        }
        return columns;
    }

    public int getRows(){
        int rows = 1;
        if(container != null){
            rows = ((RackOfTubes) container).getRackType().getVesselGeometry().getRowNames().length;
        }
        return rows;
    }
}

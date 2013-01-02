package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * This is the bean class for the container view composite component
 */
@ManagedBean
@RequestScoped
public class ContainerBean {
    @Inject
    private LabVesselDao labVesselDao;

    private LabVessel vessel;
    private Integer index = -1;
    private String barcode;
    private List<String> geometry = new ArrayList<String>();

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void updateVessel(String barcode) {
        if (barcode != null) {
            this.vessel = labVesselDao.findByIdentifier(barcode);
            this.barcode = barcode;
        }
    }

    public int getColumns() {
        int columns = 1;
        if (vessel != null) {
            columns = vessel.getVesselGeometry().getColumnNames().length;
        }
        return columns;
    }

    public int getRows() {
        int rows = 1;
        if (vessel != null) {
            rows = vessel.getVesselGeometry().getRowNames().length;
        }
        return rows;
    }

    // This method keeps a running count of rows since the primefaces DataGrid does not do this.
    // Details on this issue can be found here: http://code.google.com/p/primefaces/issues/detail?id=4124
    public String getRowNumIndex() {
        index++;
        int columnNum = getColumnNumFromIndex();
        int rowIndex = getRowNumFromIndex();
        String columnName = vessel.getVesselGeometry().getColumnNames()[columnNum];
        String rowName = vessel.getVesselGeometry().getRowNames()[rowIndex];
        VesselPosition position = VesselPosition.getByName(rowName + columnName);
        String positionName = " ";
        if (position != null) {
            positionName = position.name();
        }
        return positionName;
    }

    private int getRowNumFromIndex() {
        return index / getColumns();
    }

    private int getColumnNumFromIndex() {
        return index % getColumns();
    }


    /**
     * This method finds all of the sample instances at a specific container position.
     *
     * @return a list of sample instances for all samples at the current container position (represented by index)
     */
    public List<SampleInstance> samplesAtPosition() {
        List<SampleInstance> sampleInstances = null;
        if (vessel != null && index != -1) {
            String columnName = vessel.getVesselGeometry().getColumnNames()[getColumnNumFromIndex()];
            String rowName = vessel.getVesselGeometry().getRowNames()[getRowNumFromIndex()];
            VesselPosition position = VesselPosition.getByName(rowName + columnName);
            VesselContainer<?> vesselContainer = vessel.getContainerRole();
            if (vesselContainer != null) {
                sampleInstances = vesselContainer.getSampleInstancesAtPositionList(position);
            } else {
                sampleInstances = vessel.getSampleInstancesList();
            }

        }
        return sampleInstances;
    }

    /**
     * This method returns a dummy list of strings that prime faces data grid iterates over in order to get the
     * geometry of the container correct.
     *
     * @return a list of dummy strings for dataGrid to iterate over.
     */
    public List<String> getGeometry() {
        if (vessel != null && geometry.size() == 0) {
            int capacity = vessel.getVesselGeometry().getCapacity();
            for (int i = 0; i < capacity; i++) {
                geometry.add("");
            }
        }

        return geometry;
    }
}

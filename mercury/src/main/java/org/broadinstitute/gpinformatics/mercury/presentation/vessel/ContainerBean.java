package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@ManagedBean
@RequestScoped
public class ContainerBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;

    private LabVessel vessel;
    private String selectedSample;
    private Integer index = -1;
    private String barcode;
    private List<String> geometry = new ArrayList<String>();

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String getSelectedSample() {
        return selectedSample;
    }

    public void setSelectedSample(String selectedSample) {
        this.selectedSample = selectedSample;
    }

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    public void updateVessel(String barcode) {
        if (barcode != null && this.vessel == null) {
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

    //we need to keep a running count of rows since the datagrid won't do this for us
    //http://code.google.com/p/primefaces/issues/detail?id=4124
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

    public List<SampleInstance> samplesAtPosition() {
        List<SampleInstance> instances = null;
        if (vessel != null && index != -1) {
            String columnName = vessel.getVesselGeometry().getColumnNames()[getColumnNumFromIndex()];
            String rowName = vessel.getVesselGeometry().getRowNames()[getRowNumFromIndex()];
            VesselPosition position = VesselPosition.getByName(rowName + columnName);
            Set<SampleInstance> sampleInstances;
            VesselContainer<?> vesselContainer = null;
            if(OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                vesselContainer = ((RackOfTubes)vessel).getVesselContainer();
            }
            if(OrmUtil.proxySafeIsInstance(vessel, StaticPlate.class)) {
                vesselContainer = ((StaticPlate)vessel).getVesselContainer();
            }
            if(OrmUtil.proxySafeIsInstance(vessel, StripTube.class)) {
                vesselContainer = ((StripTube)vessel).getVesselContainer();
            }
            if(OrmUtil.proxySafeIsInstance(vessel, IlluminaFlowcell.class)) {
                vesselContainer = ((IlluminaFlowcell)vessel).getVesselContainer();
            }
            if(vesselContainer != null){
                sampleInstances = vesselContainer.getSampleInstancesAtPosition(position);
            }
            else {
                sampleInstances = vessel.getSampleInstances();
            }

            if (sampleInstances != null && sampleInstances.size() > 0) {
                instances = new ArrayList<SampleInstance>(sampleInstances);
            }
        }
        return instances;
    }

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

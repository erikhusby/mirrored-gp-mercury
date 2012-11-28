package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@ManagedBean
@RequestScoped
public class SampleListBean {
    @Inject
    private LabVesselDao labVesselDao;

    private LabVessel vessel;
    private String barcode;
    private VesselContainer<?> vesselContainer;

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public VesselContainer<?> getVesselContainer() {
        return vesselContainer;
    }

    public void setVesselContainer(VesselContainer<?> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    public void updateVessel(String barcode) {
        if (barcode != null && this.vessel == null) {
            this.vessel = labVesselDao.findByIdentifier(barcode);
            this.barcode = barcode;
            if (OrmUtil.proxySafeIsInstance(vessel, RackOfTubes.class)) {
                vesselContainer = ((RackOfTubes) vessel).getContainerRole();
            }
            if (OrmUtil.proxySafeIsInstance(vessel, StaticPlate.class)) {
                vesselContainer = ((StaticPlate) vessel).getContainerRole();
            }
            if (OrmUtil.proxySafeIsInstance(vessel, StripTube.class)) {
                vesselContainer = ((StripTube) vessel).getContainerRole();
            }
            if (OrmUtil.proxySafeIsInstance(vessel, IlluminaFlowcell.class)) {
                vesselContainer = ((IlluminaFlowcell) vessel).getContainerRole();
            }
        }
    }

    public String indexValueForSample(SampleInstance sample) {
        StringBuilder indexInfo = new StringBuilder("");
        for (Reagent reagent : sample.getReagents()) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                indexInfo.append(" - ");
                for (MolecularIndexingScheme.IndexPosition hint : indexReagent.getMolecularIndexingScheme().getIndexes().keySet()) {
                    MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                    indexInfo.append(index.getSequence());
                    indexInfo.append("\n");
                }

            }
        }
        return indexInfo.toString();
    }

    public String reagentInfoForSample(SampleInstance sample) {
        StringBuilder reagentInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (!(OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class))) {
                reagentInfo.append(reagent.getReagentName());
                reagentInfo.append(" - ");
                reagentInfo.append(reagent.getLot());
                reagentInfo.append("\n");
            }
        }
        return reagentInfo.toString();
    }

    public List<VesselPosition> getPositionNameList() {
        List<VesselPosition> positions = null;
        if (vessel != null) {
            positions = new ArrayList<VesselPosition>();
            Iterator<String> iterator = vessel.getVesselGeometry().getPositionNames();
            while (iterator.hasNext()) {
                positions.add(VesselPosition.getByName(iterator.next()));
            }
        }
        return positions;
    }

    public List<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        if (vesselContainer != null) {
            return vesselContainer.getSampleInstancesAtPositionList(position);
        } else {
            return vessel.getSampleInstancesList();
        }
    }

    public LabVessel getVesselAtPosition(VesselPosition position) {
        LabVessel vesselAtPosition;
        if (vesselContainer != null && !vesselContainer.hasAnonymousVessels()) {
            vesselAtPosition = vesselContainer.getVesselAtPosition(position);
        } else {
            vesselAtPosition = vessel;
        }
        return vesselAtPosition;
    }
}

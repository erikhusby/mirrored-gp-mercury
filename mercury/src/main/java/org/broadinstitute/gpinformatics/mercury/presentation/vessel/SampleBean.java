package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.RequestScoped;
import javax.inject.Inject;
import java.io.Serializable;

@ManagedBean
@RequestScoped
public class SampleBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;

    private LabVessel vessel;
    private String barcode;

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

    public void updateVessel(String barcode) {
        if (barcode != null && this.vessel == null) {
            this.vessel = labVesselDao.findByIdentifier(barcode);
            this.barcode = barcode;
        }
    }

    public String indexValueForSample(SampleInstance sample) {
        StringBuilder indexInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (reagent instanceof MolecularIndexReagent) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                indexInfo.append(" - ");
                for (MolecularIndexingScheme.PositionHint hint : indexReagent.getMolecularIndexingScheme().getIndexes().keySet()) {
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
            if (!(reagent instanceof MolecularIndexReagent)) {
                reagentInfo.append(reagent.getReagentName());
                reagentInfo.append(" - ");
                reagentInfo.append(reagent.getLot());
                reagentInfo.append("\n");
            }
        }
        return reagentInfo.toString();
    }
}

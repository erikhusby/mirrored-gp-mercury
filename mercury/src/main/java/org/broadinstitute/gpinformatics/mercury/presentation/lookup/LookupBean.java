package org.broadinstitute.gpinformatics.mercury.presentation.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.primefaces.event.ToggleEvent;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

@ManagedBean(name = "lookupBean")
@ViewScoped
public class LookupBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;
    private String barcode;
    private LabVessel selectedVessel;
    private List<LabVessel> foundVessels;

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }


    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }

    public void barcodeSearch(String barcode) {
        this.barcode = barcode;
        barcodeSearch();
    }

    public void barcodeSearch() {
        List<String> barcodeList = Arrays.asList(barcode.trim().split(","));
        foundVessels = labVesselDao.findByListIdentifiers(barcodeList);
    }

    public boolean isSingleSampleVessel(LabVessel vessel) {
        return vessel != null && (vessel.getType().equals(LabVessel.CONTAINER_TYPE.TUBE)
                || vessel.getType().equals(LabVessel.CONTAINER_TYPE.PLATE_WELL)
                || vessel.getType().equals(LabVessel.CONTAINER_TYPE.STRIP_TUBE_WELL));
    }

    public String indexValueForSample(SampleInstance sample, LabVessel vessel) {
        String output = null;
        if (isSingleSampleVessel(vessel)) {
            StringBuilder indexInfo = new StringBuilder();
            for (Reagent reagent : sample.getReagents()) {
                if (reagent instanceof MolecularIndexReagent) {
                    MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                    indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                    indexInfo.append(" - ");
                    for (MolecularIndexingScheme.PositionHint hint : indexReagent.getMolecularIndexingScheme().getIndexes().keySet()) {
                        MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                        indexInfo.append(index.getSequence());
                    }

                }
            }
            output = indexInfo.toString();
        }
        return output;
    }

    public void onRowToggle(ToggleEvent event) {
        selectedVessel = labVesselDao.findByIdentifier(((LabVessel) event.getData()).getLabel());
    }
}

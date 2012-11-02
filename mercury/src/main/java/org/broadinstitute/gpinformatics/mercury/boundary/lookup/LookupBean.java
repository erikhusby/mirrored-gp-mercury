package org.broadinstitute.gpinformatics.mercury.boundary.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.*;

import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean(name = "lookupBean")
@ViewScoped
public class LookupBean implements Serializable{

    private String barcode;
    private String selectedBarcode;
    private List<LabVessel> foundVessels;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    @Inject
    private StripTubeDao stripTubeDao;

    public String getSelectedBarcode() {
        return selectedBarcode;
    }

    public void setSelectedBarcode(String selectedBarcode) {
        this.selectedBarcode = selectedBarcode;
    }

    public void barcodeSearch(String barcode){
        this.barcode = barcode;
        barcodeSearch();
    }

    public void barcodeSearch() {
        selectedBarcode = null;
        foundVessels = new ArrayList<LabVessel>();
        barcode = barcode.trim();
        TwoDBarcodedTube tube = twoDBarcodedTubeDAO.findByBarcode(barcode);
        IlluminaFlowcell flowCell = flowcellDao.findByBarcode(barcode);
        StaticPlate plate = staticPlateDAO.findByBarcode(barcode);
        RackOfTubes rackOfTubes = rackOfTubesDao.getByLabel(barcode);
        StripTube stripTube = stripTubeDao.findByBarcode(barcode);
        if(tube != null){
            foundVessels.add(tube);
        }
        if(plate != null){
            foundVessels.add(plate);
        }
        if(flowCell != null) {
            foundVessels.add(flowCell);
        }
        if(rackOfTubes != null) {
            foundVessels.add(rackOfTubes);
        }
        if(stripTube != null){
            foundVessels.add(stripTube);
        }
    }

    public boolean isSingleSampleVessel(LabVessel vessel) {
        return vessel != null && (vessel.getType().equals(LabVessel.CONTAINER_TYPE.TUBE)
                || vessel.getType().equals(LabVessel.CONTAINER_TYPE.PLATE_WELL)
                || vessel.getType().equals(LabVessel.CONTAINER_TYPE.STRIP_TUBE_WELL));
    }

    public String indexValueForSample(SampleInstance sample, LabVessel vessel){
        String output = null;
        if(isSingleSampleVessel(vessel)){
            StringBuilder indexInfo = new StringBuilder();
            for(Reagent reagent : sample.getReagents()) {
                if(reagent instanceof MolecularIndexReagent){
                    MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                    indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                    indexInfo.append(" - ");
                    for(MolecularIndexingScheme.PositionHint hint :indexReagent.getMolecularIndexingScheme().getIndexes().keySet()) {
                        MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                        indexInfo.append(index.getSequence());
                    }

                }
            }
            output = indexInfo.toString();
        }
        return output;
    }

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
}

package org.broadinstitute.gpinformatics.mercury.boundary.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.IlluminaFlowcellDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.RackOfTubesDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.StaticPlateDAO;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.TwoDBarcodedTubeDAO;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.RackOfTubes;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;

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
    private List<LabVessel> foundVessels;

    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO staticPlateDAO;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private RackOfTubesDao rackOfTubesDao;

    public void barcodeSearch(String barcode){
        this.barcode = barcode;
        barcodeSearch();
    }
    public void barcodeSearch() {
        foundVessels = new ArrayList<LabVessel>();
        TwoDBarcodedTube tube = twoDBarcodedTubeDAO.findByBarcode(barcode);
        IlluminaFlowcell flowCell = flowcellDao.findByBarcode(barcode);
        StaticPlate plate = staticPlateDAO.findByBarcode(barcode);
        RackOfTubes tubes = rackOfTubesDao.getByLabel(barcode);
        if(tube != null){
            foundVessels.add(tube);
        }
        if(plate != null){
            foundVessels.add(plate);
        }
        if(flowCell != null) {
            foundVessels.add(flowCell);
        }
        if(tubes != null) {
            foundVessels.add(tubes);
        }
    }

    public boolean isTube(LabVessel vessel) {
        return vessel != null && (vessel.getType().equals(LabVessel.CONTAINER_TYPE.TUBE) || vessel.getType().equals(LabVessel.CONTAINER_TYPE.STRIP_TUBE));
    }
    public String indexValueForSample(SampleInstance sample, LabVessel vessel){
        String output = null;
        if(isTube(vessel)){
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

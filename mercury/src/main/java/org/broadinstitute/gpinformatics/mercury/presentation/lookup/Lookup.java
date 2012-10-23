package org.broadinstitute.gpinformatics.mercury.presentation.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.run.IlluminaFlowcell;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.TwoDBarcodedTube;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@RequestScoped
public class Lookup extends AbstractJsfBean implements Serializable {
    private String barcode;
    private List<LabVessel> foundVessels;
    @Inject
    private TwoDBarcodedTubeDAO twoDBarcodedTubeDAO;

    @Inject
    private StaticPlateDAO  staticPlateDAO;

    @Inject
    private IlluminaFlowcellDao flowcellDao;

    @Inject
    private RackOfTubesDao  rackOfTubesDao;

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String barcodeSearch() {
        String targetPage = "/lookup/search";
        foundVessels = new ArrayList<LabVessel>();
        TwoDBarcodedTube tube = twoDBarcodedTubeDAO.findByBarcode(barcode);
        IlluminaFlowcell flowCell = flowcellDao.findByBarcode(barcode);
        StaticPlate plate = staticPlateDAO.findByBarcode(barcode);
        if(tube != null){
            foundVessels.add(tube);
        }
        if(plate != null){
            foundVessels.add(plate);
        }
        if(flowCell != null) {
            foundVessels.add(flowCell);
        }
        return targetPage;
    }

    public List<LabVessel> getFoundVessels() {
        return foundVessels;
    }

    public void setFoundVessels(List<LabVessel> foundVessels) {
        this.foundVessels = foundVessels;
    }
}

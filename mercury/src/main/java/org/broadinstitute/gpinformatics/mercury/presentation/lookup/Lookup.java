package org.broadinstitute.gpinformatics.mercury.presentation.lookup;

import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.*;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.StaticPlate;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Named
@ViewScoped
public class Lookup extends AbstractJsfBean implements Serializable {
    private String barcode;
    private List<StaticPlate> resultBeans;
    @Inject
    private LabVesselDao labVesselDao;

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public String barcodeSearch() {
        String targetPage = "/lookup/search";
        resultBeans = labVesselDao.findByBarcode(barcode);
        return targetPage;
    }

    public List<StaticPlate> getResultBeans() {
        return resultBeans;
    }

    public void setResultBeans(List<StaticPlate> resultBeans) {
        this.resultBeans = resultBeans;
    }
}

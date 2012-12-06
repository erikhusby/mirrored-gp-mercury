package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@ManagedBean
@ViewScoped
public class PlasticHistoryBean implements Serializable {
    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private LabVesselDao labVesselDao;

    private MercurySample sample;

    private String barcode;

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public void updateSample(String barcode) {
        sample = mercurySampleDao.findBySampleKey(barcode);
    }

    public List<LabVessel> getPlasticHistory() {
        List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getSampleKey());
        List<LabVessel> targetVessels = new ArrayList<LabVessel>();
        for (LabVessel vessel : vessels) {
            targetVessels.addAll(vessel.getDescendantVessels());
        }

        return targetVessels;
    }
}

package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

@UrlBinding(value = "/view/plasticHistoryView.action")
public class PlasticHistoryViewActionBean extends CoreActionBean {

    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private LabVesselDao labVesselDao;

    private static final String VIEW_PAGE = "/resources/sample/plasticHistoryView.jsp";

    private String sampleKey;

    public String getSampleKey() {
        return sampleKey;
    }

    public void setSampleKey(String sampleKey) {
        this.sampleKey = sampleKey;
    }


    @DefaultHandler
    public Resolution view() {
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method traverses the all descendant vessels for the selected sample.
     *
     * @return a list of vessels that represents the plastic history of the selected sample.
     */
    public List<LabVessel> getPlasticHistory() {
        List<LabVessel> targetVessels = new ArrayList<LabVessel>();
        MercurySample selectedSample = mercurySampleDao.findBySampleKey(sampleKey);
        if (selectedSample != null) {
            List<LabVessel> vessels = labVesselDao.findBySampleKey(selectedSample.getSampleKey());

            for (LabVessel vessel : vessels) {
                targetVessels.addAll(vessel.getDescendantVessels());
            }

        }
        return targetVessels;
    }

}

package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@UrlBinding(value = "/view/plasticHistoryView.action")
public class PlasticHistoryViewActionBean extends CoreActionBean {

    @Inject
    private MercurySampleDao mercurySampleDao;
    @Inject
    private LabVesselDao labVesselDao;
    @Inject
    private LabBatchDAO labBatchDAO;

    private static final String VIEW_PAGE = "/resources/sample/plasticHistoryView.jsp";

    private String sampleKey;

    private String batchKey;

    public String getBatchKey() {
        return batchKey;
    }

    public void setBatchKey(String batchKey) {
        this.batchKey = batchKey;
    }

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
    public Set<LabVessel> getPlasticHistory() {
        Set<LabVessel> targetVessels = new HashSet<LabVessel>();
        if (sampleKey != null) {
            MercurySample selectedSample = mercurySampleDao.findBySampleKey(sampleKey);
            if (selectedSample != null) {
                List<LabVessel> vessels = labVesselDao.findBySampleKey(selectedSample.getSampleKey());
                for (LabVessel vessel : vessels) {
                    targetVessels.addAll(vessel.getDescendantVessels());
                }
            }
        } else if (batchKey != null) {
            LabBatch batch = labBatchDAO.findByBusinessKey(batchKey);
            if (batch != null) {
                Set<LabVessel> vessels = batch.getStartingLabVessels();
                for (LabVessel vessel : vessels) {
                    targetVessels.addAll(vessel.getDescendantVessels());
                }
            }
        }
        return targetVessels;
    }

}

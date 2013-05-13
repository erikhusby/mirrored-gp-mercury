package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.sample.MercurySampleDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.run.RunCartridge;
import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingRun;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.Collection;
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

    private static final String VIEW_PAGE = "/sample/plastic_history.jsp";

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
     * @return a list of items that represents the plastic history of the selected sample.
     */
    public Set<PlasticHistoryListItem> getPlasticHistory() {
        Set<PlasticHistoryListItem> targetItems = new HashSet<PlasticHistoryListItem>();
        if (sampleKey != null) {
            List<MercurySample> selectedSamples = mercurySampleDao.findBySampleKey(sampleKey);
            if (selectedSamples != null) {
                for (MercurySample selectedSample : selectedSamples) {
                    List<LabVessel> vessels = labVesselDao.findBySampleKey(selectedSample.getSampleKey());
                    addVesselsToItemList(targetItems, vessels);
                }
            }
        } else if (batchKey != null) {
            LabBatch batch = labBatchDAO.findByBusinessKey(batchKey);
            if (batch != null) {
                Set<LabVessel> vessels = batch.getStartingLabVessels();
                addVesselsToItemList(targetItems, vessels);
            }
        }
        return targetItems;
    }

    private void addVesselsToItemList(Set<PlasticHistoryListItem> targetItems, Collection<LabVessel> vessels) {
        Set<LabVessel> vesselSet = new HashSet<LabVessel>();
        for (LabVessel vessel : vessels) {
            vesselSet.addAll(vessel.getDescendantVessels());
        }

        for (LabVessel descendentVessel : vesselSet) {
            targetItems.add(new PlasticHistoryListItem(descendentVessel));

            // Adds flowcell's sequencer runs to the list.
            if (descendentVessel.getType().equals(LabVessel.ContainerType.FLOWCELL)) {
                if (OrmUtil.proxySafeIsInstance(descendentVessel, RunCartridge.class)) {
                    RunCartridge runCartridge = (RunCartridge) descendentVessel;
                    for (SequencingRun seqRun : runCartridge.getSequencingRuns()) {
                        targetItems.add(new PlasticHistoryListItem(seqRun, descendentVessel));
                    }
                }
            }
        }

    }

}

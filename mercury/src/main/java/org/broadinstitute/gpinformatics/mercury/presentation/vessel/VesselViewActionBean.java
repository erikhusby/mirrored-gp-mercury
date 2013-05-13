package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabMetric;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UrlBinding(value = "/view/vesselView.action")
public class VesselViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/container/vessel_layout.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    private String vesselLabel;

    private LabVessel vessel;

    private Map<String, Set<LabMetric>> sampleToMetricsMap = new HashMap<String, Set<LabMetric>>();

    public String getVesselLabel() {
        return vesselLabel;
    }

    public void setVesselLabel(String vesselLabel) {
        this.vesselLabel = vesselLabel;
    }

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    public Map<String, Set<LabMetric>> getSampleToMetricsMap() {
        return sampleToMetricsMap;
    }

    public void setSampleToMetricsMap(Map<String, Set<LabMetric>> sampleToMetricsMap) {
        this.sampleToMetricsMap = sampleToMetricsMap;
    }

    @DefaultHandler
    public Resolution view() {
        if (vesselLabel != null) {
            this.vessel = labVesselDao.findByIdentifier(vesselLabel);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    public Set<SampleInstance> samplesAtPosition(String rowName, String columnName) {
        Set<SampleInstance> sampleInstances;
        VesselPosition position = VesselPosition.getByName(rowName + columnName);
        VesselContainer<?> vesselContainer = vessel.getContainerRole();
        if (vesselContainer != null) {
            sampleInstances = vesselContainer.getSampleInstancesAtPosition(position);
        } else {
            sampleInstances = vessel.getSampleInstances();
        }
        for (SampleInstance sample : sampleInstances) {
            List<LabVessel> vessels = labVesselDao.findBySampleKey(sample.getStartingSample().getSampleKey());
            for (LabVessel sampleVessel : vessels) {
                Set<LabMetric> metrics = sampleVessel.getMetrics();
                if (metrics.size() > 0) {
                    sampleToMetricsMap.put(sample.getStartingSample().getSampleKey(), metrics);
                }
            }
        }
        return sampleInstances;
    }
}

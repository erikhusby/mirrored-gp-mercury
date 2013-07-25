package org.broadinstitute.gpinformatics.mercury.presentation.sample;

import net.sourceforge.stripes.action.DefaultHandler;
import net.sourceforge.stripes.action.ForwardResolution;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;
import net.sourceforge.stripes.validation.LocalizableError;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDTO;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPSampleDataFetcher;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.broadinstitute.gpinformatics.mercury.presentation.CoreActionBean;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@UrlBinding(value = "/view/vesselSampleListView.action")
public class VesselSampleListViewActionBean extends CoreActionBean {

    private static final String VIEW_PAGE = "/sample/vessel_sample_list.jsp";

    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPSampleDataFetcher sampleDataFetcher;

    private String vesselLabel;

    private LabVessel vessel;

    private Map<String, BSPSampleDTO> bspInfoDetails = new HashMap<>();

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

    @DefaultHandler
    public Resolution view() {
        if (vesselLabel != null) {
            this.vessel = labVesselDao.findByIdentifier(vesselLabel);
        }
        return new ForwardResolution(VIEW_PAGE);
    }

    /**
     * This method gets the vessel at the a specific postion.  If this vessel is in a container it does the lookup
     * by the position of that container. Otherwise it returns itself.
     *
     * @param position the position to get the vessel from..
     *
     * @return the vessel at the position.
     */
    public LabVessel getVesselAtPosition(VesselPosition position) {
        LabVessel vesselAtPosition;
        VesselContainer vesselContainer = vessel.getContainerRole();
        if (vesselContainer != null && !vesselContainer.hasAnonymousVessels()) {
            vesselAtPosition = vesselContainer.getVesselAtPosition(position);
        } else {
            vesselAtPosition = vessel;
        }
        return vesselAtPosition;
    }

    /**
     * This method gets all of the samples instances at a vessel position. If there is not a vessel container
     * the samples are taken from the vessel directly.
     *
     * @param position the vessel position to get samples from
     *
     * @return a list of samples at the vessel position
     */
    public Set<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        if (vessel.getContainerRole() != null) {
            return vessel.getContainerRole().getSampleInstancesAtPosition(position);
        } else {
            return vessel.getSampleInstances();
        }
    }


    /**
     * This method get index information for a sample instance.
     *
     * @param sample the sample to get the indexes from.
     *
     * @return a string representing all indexes for this sample.
     */
    public String getIndexValueForSample(SampleInstance sample) {
        StringBuilder indexInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                indexInfo.append(" - ");
                for (MolecularIndexingScheme.IndexPosition hint : indexReagent.getMolecularIndexingScheme().getIndexes()
                        .keySet()) {
                    MolecularIndex index = indexReagent.getMolecularIndexingScheme().getIndexes().get(hint);
                    indexInfo.append(index.getSequence());
                    indexInfo.append("\n");
                }

            }
        }
        return indexInfo.toString();
    }

    /**
     * This method get all reagent information for a sample instance execept indexes.
     *
     * @param sample the sample to get the reagents from.
     *
     * @return a string representing all reagents (except indexes) for this sample.
     */
    public String getReagentInfoForSample(SampleInstance sample) {
        StringBuilder reagentInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (!(OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class))) {
                reagentInfo.append(reagent.getName());
                reagentInfo.append(" - ");
                reagentInfo.append(reagent.getLot());
                reagentInfo.append("\n");
            }
        }
        return reagentInfo.toString();
    }

    /**
     * This method looks up bsp sample information and stores it in the bsp info details.
     *
     * @param sample the sample instance to look up bsp info for.
     */
    public void lookupBspSampleInfo(SampleInstance sample) {
        if (sample != null) {
            BSPSampleDTO bspSampleDTO;
            if (sample.getStartingSample().getBspSampleDTO() != null) {
                bspSampleDTO = sample.getStartingSample().getBspSampleDTO();
            } else {
                try {
                    bspSampleDTO =
                            sampleDataFetcher.fetchSingleSampleFromBSP(sample.getStartingSample().getSampleKey());
                } catch (RuntimeException re) {
                    bspSampleDTO = null;
                    flashErrorMessage(new LocalizableError("BSP Warning: " + re.getLocalizedMessage()));
                }
            }
            bspInfoDetails.put(sample.getStartingSample().getSampleKey(), bspSampleDTO);
        }
    }
}

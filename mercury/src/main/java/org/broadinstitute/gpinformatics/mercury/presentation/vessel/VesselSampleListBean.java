package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

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
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.*;

/**
 * This is the bean class for the sample list per vessel composite component.
 */
@ManagedBean
@ViewScoped
public class VesselSampleListBean extends AbstractJsfBean implements Serializable {
    @Inject
    private LabVesselDao labVesselDao;

    @Inject
    private BSPSampleDataFetcher sampleDataFetcher;

    private LabVessel vessel;
    private String barcode;
    private Map<String, BSPSampleDTO> bspInfoDetails = new HashMap<String, BSPSampleDTO>();

    private VesselContainer<?> vesselContainer;

    public LabVessel getVessel() {
        return vessel;
    }

    public void setVessel(LabVessel vessel) {
        this.vessel = vessel;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public VesselContainer<?> getVesselContainer() {
        return vesselContainer;
    }

    public void setVesselContainer(VesselContainer<?> vesselContainer) {
        this.vesselContainer = vesselContainer;
    }

    public BSPSampleDTO getBspInfoDetails(String sampleName) {
        return bspInfoDetails.get(sampleName);
    }

    public void updateVessel(String barcode) {
        if (barcode != null && !barcode.equals("")) {
            this.vessel = labVesselDao.findByIdentifier(barcode);
            this.barcode = barcode;
            if (vessel != null) {
                vesselContainer = vessel.getContainerRole();
            }
        }
    }

    /**
     * This method get index information for a sample instance.
     *
     * @param sample the sample to get the indexes from.
     * @return a string representing all indexes for this sample.
     */
    public String indexValueForSample(SampleInstance sample) {
        StringBuilder indexInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class)) {
                MolecularIndexReagent indexReagent = (MolecularIndexReagent) reagent;
                indexInfo.append(indexReagent.getMolecularIndexingScheme().getName());
                indexInfo.append(" - ");
                for (MolecularIndexingScheme.IndexPosition hint : indexReagent.getMolecularIndexingScheme().getIndexes().keySet()) {
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
     * @return a string representing all reagents (except indexes) for this sample.
     */
    public String reagentInfoForSample(SampleInstance sample) {
        StringBuilder reagentInfo = new StringBuilder();
        for (Reagent reagent : sample.getReagents()) {
            if (!(OrmUtil.proxySafeIsInstance(reagent, MolecularIndexReagent.class))) {
                reagentInfo.append(reagent.getReagentName());
                reagentInfo.append(" - ");
                reagentInfo.append(reagent.getLot());
                reagentInfo.append("\n");
            }
        }
        return reagentInfo.toString();
    }

    /**
     * This method gets a list of all vessel names for the selected vessel.
     *
     * @return a list of all vessel positions for the vessel.
     */
    public List<VesselPosition> getPositionNameList() {
        List<VesselPosition> positions = new ArrayList<VesselPosition>();
        if (vessel != null) {
            Iterator<String> iterator = vessel.getVesselGeometry().getPositionNames();
            while (iterator.hasNext()) {
                positions.add(VesselPosition.getByName(iterator.next()));
            }
        }
        return positions;
    }

    /**
     * This method gets all of the samples instances at a vessel position. If there is not a vessel container
     * the samples are taken from the vessel directly.
     *
     * @param position the vessel position to get samples from
     * @return a list of samples at the vessel position
     */
    public List<SampleInstance> getSampleInstancesAtPosition(VesselPosition position) {
        if (vesselContainer != null) {
            return vesselContainer.getSampleInstancesAtPositionList(position);
        } else {
            return vessel.getSampleInstancesList();
        }
    }

    /**
     * This method gets the vessel at the a specific postion.  If this vessel is in a container it does the lookup
     * by the position of that container. Otherwise it returns itself.
     *
     * @param position the position to get the vessel from..
     * @return the vessel at the position.
     */
    public LabVessel getVesselAtPosition(VesselPosition position) {
        LabVessel vesselAtPosition;
        if (vesselContainer != null && !vesselContainer.hasAnonymousVessels()) {
            vesselAtPosition = vesselContainer.getVesselAtPosition(position);
        } else {
            vesselAtPosition = vessel;
        }
        return vesselAtPosition;
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
                    bspSampleDTO = sampleDataFetcher.fetchSingleSampleFromBSP(sample.getStartingSample().getSampleKey());
                } catch (RuntimeException re) {
                    bspSampleDTO = null;
                    addWarnMessage("BSP Warning: " + re.getLocalizedMessage());
                }
            }
            bspInfoDetails.put(sample.getStartingSample().getSampleKey(), bspSampleDTO);
        }
    }

}

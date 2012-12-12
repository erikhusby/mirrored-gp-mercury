package org.broadinstitute.gpinformatics.mercury.presentation.vessel;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.control.dao.vessel.LabVesselDao;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndex;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexReagent;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.MolecularIndexingScheme;
import org.broadinstitute.gpinformatics.mercury.entity.reagent.Reagent;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.LabVessel;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.inject.Inject;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

@ManagedBean
@ViewScoped
public class VesselListBean implements Serializable {

    @Inject
    private BSPUserList bspUserList;
    @Inject
    private LabVesselDao labVesselDao;

    private LabVessel selectedVessel;

    public LabVessel getSelectedVessel() {
        return selectedVessel;
    }

    public void setSelectedVessel(LabVessel selectedVessel) {
        this.selectedVessel = selectedVessel;
    }

    public String getUserNameById(Long id) {
        BspUser user = bspUserList.getById(id);
        String username = "";
        if (user != null) {
            username = bspUserList.getById(id).getUsername();
        }
        return username;
    }

    public Set<SampleInstance> getAllSamples(LabVessel vessel) {
        Set<SampleInstance> allSamples = new HashSet<SampleInstance>();
        allSamples.addAll(vessel.getSampleInstances());
        if (vessel.getContainerRole() != null) {
            allSamples.addAll(vessel.getContainerRole().getSampleInstances());
        }
        return allSamples;
    }

    public Set<String> getPdoKeys(LabVessel vessel) {
        Set<String> pdoKeys = new HashSet<String>();

        for (SampleInstance sample : getAllSamples(vessel)) {
            pdoKeys.add(sample.getStartingSample().getProductOrderKey());
        }
        pdoKeys.remove(null);
        return pdoKeys;
    }

    public Set<String> getIndexes(LabVessel vessel) {
        Set<String> indexes = new HashSet<String>();
        StringBuilder indexInfo = new StringBuilder();
        for (SampleInstance sample : getAllSamples(vessel)) {
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
                    indexes.add(indexInfo.toString());
                    indexInfo.delete(0, indexInfo.length());
                }
            }
        }
        return indexes;
    }
}



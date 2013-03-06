package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselGeometry;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.Set;

@Entity
@Audited
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<RunChamber> {


    @Enumerated(EnumType.STRING)
    private FLOWCELL_TYPE flowcellType;

    // todo jmt how is this different from label?
    private String flowcellBarcode;

    @Embedded
    VesselContainer<RunChamber> vesselContainer = new VesselContainer<RunChamber>(this);

    protected IlluminaFlowcell(String label) {
        super(label);
        this.flowcellBarcode = label;
    }

    public IlluminaFlowcell() {
    }

    @Override
    public Set<LabEvent> getTransfersFrom() {
        return vesselContainer.getTransfersFrom();
    }

    @Override
    public Set<LabEvent> getTransfersTo() {
        return vesselContainer.getTransfersTo();
    }

    @Override
    public VesselGeometry getVesselGeometry() {
        return VesselGeometry.FLOWCELL;
    }

    @Override
    public CONTAINER_TYPE getType() {
        return CONTAINER_TYPE.FLOWCELL;
    }

    @Override
    public VesselContainer<RunChamber> getContainerRole() {
        return this.vesselContainer;
    }

    public enum FLOWCELL_TYPE {
        EIGHT_LANE,MISEQ
    }

    public IlluminaFlowcell(FLOWCELL_TYPE flowcellType, String flowcellBarcode) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
        this.flowcellType = flowcellType;
    }
        
/*
    todo jmt need something similar in VesselContainer
    public void addChamber(LabVessel library,int laneNumber) {
        if (flowcellType == FLOWCELL_TYPE.EIGHT_LANE) {
            if (laneNumber < 1 || laneNumber > 8) {
                throw new RuntimeException("Lane numbers are 1-8");
            }
        }
        else if (flowcellType == FLOWCELL_TYPE.MISEQ) {
            if (laneNumber != 1) {
                throw new RuntimeException("Miseq flowcells only have a single lane");
            }
        }
        IlluminaRunChamber runChamber = new IlluminaRunChamber(this,laneNumber,library);
        runChambers.add(runChamber);
    }
*/

    @Override
    public Iterable<RunChamber> getChambers() {
        return this.vesselContainer.getContainedVessels();
    }

    @Override
    public String getCartridgeName() {
        return this.flowcellBarcode;
    }

    @Override
    public String getCartridgeBarcode() {
        return this.flowcellBarcode;
    }

}

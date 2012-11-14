package org.broadinstitute.gpinformatics.mercury.entity.run;


import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.notice.StatusNote;
import org.broadinstitute.gpinformatics.mercury.entity.sample.SampleInstance;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainer;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselContainerEmbedder;
import org.hibernate.envers.Audited;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.Collection;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury")
public class IlluminaFlowcell extends AbstractRunCartridge implements VesselContainerEmbedder<RunChamber> {

    // todo jmt fix this
    @Transient
    private IlluminaRunConfiguration runConfiguration;

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

    public IlluminaFlowcell(FLOWCELL_TYPE flowcellType,String flowcellBarcode,IlluminaRunConfiguration runConfig) {
        super(flowcellBarcode);
        this.flowcellBarcode = flowcellBarcode;
        this.runConfiguration = runConfig;
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

    /**
     * In the illumina world, one sets the run configuration
     * when the flowcell is made.  But other technologies
     * might have their run configuration set later
     * in the process.
     * @return
     */
    public IlluminaRunConfiguration getRunConfiguration() {
        return this.runConfiguration;
    }

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

package org.broadinstitute.gpinformatics.mercury.entity.run;

import org.broadinstitute.gpinformatics.mercury.control.hsa.state.DemultiplexState;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.annotation.Nullable;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Audited
public class IlluminaSequencingRun extends SequencingRun {

    public static final String RUN_FORMAT_PATTERN = "yyMMdd";

    private String setupReadStructure;

    private String actualReadStructure;

    private Double imagedAreaPerMM2;

    private String lanesSequenced;

    @Transient
    private boolean cancelled;

    @OneToMany(mappedBy = "illuminaSequencingRun", cascade = CascadeType.PERSIST)
    private Set<IlluminaSequencingRunChamber> sequencingRunChambers = new HashSet<>();

    public IlluminaSequencingRun(IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Long operator,
                                 boolean isTestRun,
                                 Date runDate, String runDirectory) {
        super(runName, runBarcode, machineName, operator, isTestRun, runDate, flowcell, runDirectory);

        for (VesselPosition vesselPosition: flowcell.getFlowcellType().getVesselGeometry().getVesselPositions()) {
            if (vesselPosition.name().startsWith("LANE")) {
                int laneNum = Integer.parseInt(vesselPosition.name().replace("LANE", ""));
                addSequencingRunChamber(new IlluminaSequencingRunChamber(this, laneNum));
            }
        }
    }

    protected IlluminaSequencingRun() {
    }

    public String getSetupReadStructure() {
        return setupReadStructure;
    }

    public void setSetupReadStructure(String setupReadStructure) {
        this.setupReadStructure = setupReadStructure;
    }

    public String getActualReadStructure() {
        return actualReadStructure;
    }

    public void setActualReadStructure(String actualReadStructure) {
        this.actualReadStructure = actualReadStructure;
    }

    public Double getImagedAreaPerMM2() {
        return imagedAreaPerMM2;
    }

    public void setImagedAreaPerMM2(Double imagedAreaPerMM2) {
        this.imagedAreaPerMM2 = imagedAreaPerMM2;
    }

    public String getLanesSequenced() {
        return lanesSequenced;
    }

    public void setLanesSequenced(String lanesSequenced) {
        this.lanesSequenced = lanesSequenced;
    }

    public Set<IlluminaSequencingRunChamber> getSequencingRunChambers() {
        return sequencingRunChambers;
    }

    @Nullable
    public IlluminaSequencingRunChamber getSequencingRunChamber(VesselPosition vesselPosition) {
        for (IlluminaSequencingRunChamber sequencingRunChamber : sequencingRunChambers) {
            if (sequencingRunChamber.getLanePosition() == vesselPosition) {
                return sequencingRunChamber;
            }
        }
        return null;
    }

    @Nullable
    public IlluminaSequencingRunChamber getSequencingRunChamber(int laneNumber) {
        for (IlluminaSequencingRunChamber sequencingRunChamber : sequencingRunChambers) {
            if (sequencingRunChamber.getLaneNumber() == laneNumber) {
                return sequencingRunChamber;
            }
        }
        return null;
    }

    public void addSequencingRunChamber(IlluminaSequencingRunChamber sequencingRunChamber) {
        sequencingRunChambers.add(sequencingRunChamber);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}

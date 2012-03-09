package org.broadinstitute.sequel.entity.vessel;


import org.broadinstitute.sequel.entity.labevent.SectionTransfer;
import org.broadinstitute.sequel.entity.notice.StatusNote;
import org.broadinstitute.sequel.entity.reagent.Reagent;
import org.broadinstitute.sequel.entity.sample.StateChange;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.entity.labevent.LabEvent;
import org.broadinstitute.sequel.entity.sample.SampleInstance;
import org.broadinstitute.sequel.entity.sample.SampleSheet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A traditional plate.
 */
public class StaticPlate extends AbstractLabVessel implements SBSSectionable, VesselContainer<PlateWell> {

    private Map<String, PlateWell> mapPositionToWell = new HashMap<String, PlateWell>();

    public StaticPlate(String label) {
        super(label);
    }

    @Override
    public LabVessel getContainingVessel() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public LabVessel getVesselAtPosition(String position) {
        return this.mapPositionToWell.get(position);
    }

    @Override
    public Collection<PlateWell> getContainedVessels() {
        return this.mapPositionToWell.values();
    }

    @Override
    public void addContainedVessel(PlateWell child, String position) {
        this.mapPositionToWell.put(position, child);
    }

    @Override
    public Collection<LabEvent> getEvents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public SBSSection getSection() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void addStateChange(StateChange stateChange) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Set<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if (this.mapPositionToWell.isEmpty()) {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                for (SampleSheet sampleSheet : labVessel.getSampleSheets()) {
                    sampleInstances.addAll(sampleSheet.getSampleInstances());
                }
            }
        } else {
            for (String position : this.mapPositionToWell.keySet()) {
                sampleInstances.addAll(getSampleInstancesInPosition(position));
            }
        }
        return sampleInstances;
    }

    @Override
    public Collection<SampleInstance> getSampleInstances(SampleSheet sheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StateChange> getStateChanges() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Project> getAllProjects() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void logNote(StatusNote statusNote) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getVolume() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Float getConcentration() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void applyReagent(Reagent r) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        throw new RuntimeException("I haven't been written yet.");
    }

    // todo jmt should this be getWellAtPosition().getSampleInstances()? Perhaps not, because the wells may not exist
    public Set<SampleInstance> getSampleInstancesInPosition(String wellPosition) {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        if(getSampleSheetAuthorities().isEmpty()) {
            sampleInstances = Collections.emptySet();
        } else {
            for (LabVessel labVessel : getSampleSheetAuthorities()) {
                // todo jmt generalize to handle rack and tube
                if (labVessel instanceof RackOfTubes) {
                    RackOfTubes rackOfTubes = (RackOfTubes) labVessel;
                    Set<SampleInstance> sampleInstancesInPosition = rackOfTubes.getSampleInstancesInPosition(wellPosition);
                    for (SampleInstance sampleInstance : sampleInstancesInPosition) {
                        PlateWell wellAtPosition = getWellAtPosition(wellPosition);
                        if (wellAtPosition != null) {
                            for (Reagent reagent : wellAtPosition.getAppliedReagents()) {
                                if(reagent.getMolecularEnvelopeDelta() != null) {
                                    MolecularEnvelope molecularEnvelope = sampleInstance.getMolecularState().getMolecularEnvelope();
                                    if(molecularEnvelope == null) {
                                        sampleInstance.getMolecularState().setMolecularEnvelope(reagent.getMolecularEnvelopeDelta());
                                    } else {
                                        molecularEnvelope.surroundWith(reagent.getMolecularEnvelopeDelta());
                                    }
                                }
                            }
                        }
                    }

                    sampleInstances.addAll(sampleInstancesInPosition);
                }
            }
        }
        return sampleInstances;
    }

    public PlateWell getWellAtPosition(String position) {
        return this.mapPositionToWell.get(position);
    }

    public Map<String, PlateWell> getMapPositionToWell() {
        return this.mapPositionToWell;
    }

    @Override
    public void applyTransfer(SectionTransfer sectionTransfer) {
        List<WellName> wells = sectionTransfer.getSourceSection().getWells();
        StaticPlate sourcePlate = (StaticPlate) sectionTransfer.getSourceVessel();
        StaticPlate targetPlate = (StaticPlate) sectionTransfer.getTargetVessel();
        for (int wellIndex = 0; wellIndex < wells.size(); wellIndex++) {
            WellName sourceWellName = wells.get(wellIndex);
            WellName targetWellName = sectionTransfer.getTargetSection().getWells().get(wellIndex);
            if (!sourcePlate.getMapPositionToWell().isEmpty()) {
                PlateWell sourceWell = sourcePlate.getWellAtPosition(sourceWellName.getWellName());
                if (sourceWell != null) {
                    Collection<Reagent> reagents = sourceWell.getReagentContents();
                    for (Reagent reagent : reagents) {
                        PlateWell plateWell = targetPlate.getWellAtPosition(targetWellName.getWellName());
                        if (plateWell == null) {
                            plateWell = new PlateWell(targetPlate, targetWellName);
                            targetPlate.addContainedVessel(plateWell, targetWellName.getWellName());
                        }
                        plateWell.applyReagent(reagent);
                    }
                }
            }
        }
    }
}

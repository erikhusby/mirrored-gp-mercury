package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class BaseGoop implements Goop {

    private static Log gLog = LogFactory.getLog(BaseGoop.class);

    private final String name;
        
    private final Collection<LabVessel> containers = new HashSet<LabVessel>();
    
    private final Collection<Project> allProjects = new HashSet<Project>();

    private final Collection<StatusNote> statusNotes = new HashSet<StatusNote>();
    
    private final Collection<SampleSheet> sampleSheets = new HashSet<SampleSheet>();
    
    private final Collection<Reagent> appliedReagents = new HashSet<Reagent>();
    
    private float volume;

    private float concentration;

    public BaseGoop(String name,SampleSheet sampleSheet) {
        if (name == null) {
             throw new IllegalArgumentException("name must be non-null in BaseSample.BaseSample");
        }
        if (sampleSheet == null) {
             throw new IllegalArgumentException("sampleSheet must be non-null in BaseGoop.BaseGoop");
        }
        this.name = name;
        sampleSheets.add(sampleSheet);
    }


    @Override
    public String getLabCentricName() {
        return name;
    }

    @Override
    public StatusNote getLatestNote() {
        throw new RuntimeException("I haven't been written yet.");
    }


    @Override
    public Float getVolume() {
        return volume;
    }

    @Override
    public Float getConcentration() {
        return concentration;
    }

    @Override
    public void logNote(StatusNote statusNote) {
        statusNotes.add(statusNote);
    }

    @Override
    public Collection<StatusNote> getAllStatusNotes() {
        return statusNotes;
    }


    @Override
    public Collection<SampleSheet> getSampleSheets() {
        return sampleSheets;
    }

    @Override
    public void addSampleSheet(SampleSheet sampleSheet) {
        sampleSheets.add(sampleSheet);
    }

    @Override
    public Collection<Project> getAllProjects() {
        Collection<Project> allProjects = new HashSet<Project>();
        for (SampleSheet sampleSheet : getSampleSheets()) {
            for (SampleInstance sampleInstance : sampleSheet.getSamples()) {
                if (sampleInstance.getProject() != null) {
                    allProjects.add(sampleInstance.getProject());
                }
            }
        }
        return Collections.unmodifiableCollection(allProjects);
    }

    @Override
    public void applyReagent(Reagent r) {
        appliedReagents.add(r);
        
        if (r.getMolecularEnvelopeDelta() != null) {
            Collection<SampleSheet> newSampleSheets = new HashSet<SampleSheet>();
            MolecularEnvelope envelopeDelta = r.getMolecularEnvelopeDelta();
            for (SampleSheet originalSampleSheet : getSampleSheets()) {
                SampleSheet newSampleSheet = originalSampleSheet.createBranch();
                for (SampleInstance sampleInstance : newSampleSheet.getSamples()) {
                    sampleInstance.getMolecularState().getMolecularEnvelope().surroundWith(envelopeDelta);
                }
                newSampleSheets.add(newSampleSheet);
            }
            clearSampleSheets();
            for (SampleSheet newSampleSheet : newSampleSheets) {
                addSampleSheet(newSampleSheet);
            }
        }
    }

    @Override
    public void applyGoop(Goop goop) {
        for (SampleSheet sampleSheet : goop.getSampleSheets()) {
            for (SampleInstance addedInstance: sampleSheet.getSamples()) {
                Collection<SampleInstance> sampleInstances = getSampleInstancesFor(addedInstance.getStartingSample());
                if (!sampleInstances.isEmpty()) {
                    // if this goop already contains the sample that you're
                    // trying to add, we might have a problem.
                    for (SampleInstance containedInstance : sampleInstances) {
                        if (!containedInstance.getMolecularState().getMolecularStateTemplate() .equals(addedInstance.getMolecularState().getMolecularStateTemplate())) {
                            // it's okay to have the same sample in this goop, as long as, for example,
                            // each instance of it has a different molecular index.

                            // but if we see the same sample, one with one adaptor and
                            // one without it, things look weird.
                            throw new RuntimeException("Sample " + containedInstance.getStartingSample().getSampleName() + " appears to be in two disparate states in " + getLabCentricName());
                        }
                    }
                }
            }
            addSampleSheet(sampleSheet);
        }
    }

    @Override
    public Collection<SampleInstance> getSampleInstancesFor(StartingSample rootSample) {
        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (SampleSheet sampleSheet : getSampleSheets()) {
            for (SampleInstance sampleInstance : sampleSheet.getSamples()) {
                if (rootSample.equals(sampleInstance.getStartingSample())) {
                    sampleInstances.add(sampleInstance);
                }
            }
        }
        return sampleInstances;
    }

    private void clearSampleSheets() {
        sampleSheets.clear();
    }

    @Override
    public Collection<Reagent> getAppliedReagents() {
        return appliedReagents;
    }

    @Override
    public void replaceSampleSheet(SampleSheet oldSheet, SampleSheet newSheet) {
        if (!sampleSheets.remove(oldSheet)) {
            throw new RuntimeException(getLabCentricName() + " does not contain sample sheet " + oldSheet);    
        }
        else {
            sampleSheets.add(newSheet);
        }
    }

    @Override
    public void branchFor(Project p, SampleSheet sampleSheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void branchAll(Project p) {
        for (SampleSheet oldSheet : getSampleSheets()) {
            SampleSheet newSheet = oldSheet.createBranch();
            for (SampleInstance sampleInstance : newSheet.getSamples()) {
                sampleInstance.setProject(p);
            }
            replaceSampleSheet(oldSheet, newSheet);
        }
    }

    @Override
    public void branchAll(ReadBucket bucket) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public void branchFor(ReadBucket bucket, SampleSheet sampleSheet) {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Collection<ReadBucket> getReadBuckets() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

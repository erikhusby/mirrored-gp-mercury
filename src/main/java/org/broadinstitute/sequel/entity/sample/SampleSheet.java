package org.broadinstitute.sequel.entity.sample;

import org.broadinstitute.sequel.entity.bsp.BSPSample;
import org.broadinstitute.sequel.entity.labevent.LabEventTraverser;
import org.broadinstitute.sequel.entity.vessel.LabVessel;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.SequenceGenerator;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A collection of sample metadata.
 */
@Entity
public class SampleSheet {

    @Id
    @SequenceGenerator(name = "SEQ_SAMPLE_SHEET", sequenceName = "SEQ_SAMPLE_SHEET")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_SAMPLE_SHEET")
    private Long sampleSheetId;

    @ManyToMany(targetEntity = BSPSample.class, cascade = CascadeType.PERSIST)
    private Collection<StartingSample> startingSamples = new HashSet<StartingSample>();

    @ManyToMany(mappedBy = "sampleSheets")
    private Set<LabVessel> labVessels = new HashSet<LabVessel>();

    public SampleSheet() {}

    public Collection<StartingSample> getStartingSamples() {
        return startingSamples;
    }

    public void addStartingSample(StartingSample startingSample) {
        startingSamples.add(startingSample);
    }

    public Collection<SampleInstance> getSampleInstances(LabVessel container) {
        Collection<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (StartingSample startingSample : startingSamples) {
            SampleInstance sampleInstance = startingSample.createSampleInstance();
            for (StateChange stateChange : LabEventTraverser.getStateChangesPriorToAndIncluding(this, container)) {
                // ordering of the state changes is critical...
                // doing it root-to-branch means that "nearest ancestor"
                //
                sampleInstance.applyChange(stateChange);
            }
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }

    public Collection<SampleInstance> getSampleInstances() {
        Set<SampleInstance> sampleInstances = new HashSet<SampleInstance>();
        for (StartingSample startingSample : startingSamples) {
            SampleInstance sampleInstance = startingSample.createSampleInstance();
            sampleInstances.add(sampleInstance);
        }
        return sampleInstances;
    }

    public Set<LabVessel> getLabVessels() {
        return labVessels;
    }
}

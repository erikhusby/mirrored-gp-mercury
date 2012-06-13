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

    @ManyToMany(cascade = CascadeType.PERSIST) // todo jmt hbm2ddl is still not generating an index!
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


    public Set<LabVessel> getLabVessels() {
        return labVessels;
    }
}

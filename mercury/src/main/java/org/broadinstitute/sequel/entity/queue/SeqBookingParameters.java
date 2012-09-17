package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.run.SequencingTechnology;

import java.util.Collection;

public class SeqBookingParameters implements LabWorkQueueParameters {
    
    public SequencingTechnology getSequencingTechnology() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public Collection<LabWorkQueueParameters> getOptionsFor(SequencingTechnology sequencingTechnology) {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    @Override
    public String toText() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

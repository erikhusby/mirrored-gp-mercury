package org.broadinstitute.gpinformatics.mercury.entity.queue;


import org.broadinstitute.gpinformatics.mercury.entity.run.SequencingTechnology;

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

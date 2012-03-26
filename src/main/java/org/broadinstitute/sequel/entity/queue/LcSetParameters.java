package org.broadinstitute.sequel.entity.queue;


import org.broadinstitute.sequel.entity.run.SequencingTechnology;

/**
 * The set of options for an LC set ticket.
 */
public class LcSetParameters implements LabWorkQueueParameters {

    public String getImportant() {
        throw new RuntimeException("Not implemented");
    }

    public Boolean isQcSequencingRequired()  {
        throw new RuntimeException("Not implemented");
    }

    public String getQTPPriorityType()  {
        throw new RuntimeException("Not implemented");
    }

    public Boolean isGoldStandard()  {
        throw new RuntimeException("Not implemented");
    }

    public SequencingTechnology getSequencingTechnology()  {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public String toText() {
        return "Lc Set Options";
    }
}

package org.broadinstitute.sequel.entity.reagent;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.broadinstitute.sequel.entity.analysis.ReferenceSequence;

public class PrimerPair {

    private static Log gLog = LogFactory.getLog(PrimerPair.class);

    public ReferenceSequence getReferenceSequence() {
        throw new RuntimeException("Method not yet implemented.");

    }
    public Long getSiteStart() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
    public Long getSiteStop() {
        throw new RuntimeException("Method not yet implemented.");

    }

    public Sequence get3PrimeSequence() {
        throw new RuntimeException("Method not yet implemented.");
    }

    public Sequence get5Prime() {
        throw new RuntimeException("Method not yet implemented.");
    }
    
}

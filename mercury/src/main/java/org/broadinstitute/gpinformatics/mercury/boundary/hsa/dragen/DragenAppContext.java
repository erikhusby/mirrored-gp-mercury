package org.broadinstitute.gpinformatics.mercury.boundary.hsa.dragen;

public class DragenAppContext {

    private Dragen dragen;

    public DragenAppContext(Dragen dragen) {
        this.dragen = dragen;
    }

    public Dragen getInstance() {
        return dragen;
    }
}

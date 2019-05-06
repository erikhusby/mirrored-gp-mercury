package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen;

import javax.enterprise.context.Dependent;

@Dependent
public class DragenAppContext {

    private Dragen dragen;

    public DragenAppContext() {
    }

    public DragenAppContext(Dragen dragen) {
        this.dragen = dragen;
    }

    public Dragen getInstance() {
        return dragen;
    }
}

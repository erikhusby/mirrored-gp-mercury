package org.broadinstitute.sequel.presentation.zims;

import org.broadinstitute.sequel.boundary.zims.IlluminaRunResource;
import org.broadinstitute.sequel.entity.zims.ZimsIlluminaRun;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class IlluminaRunQuery extends AbstractJsfBean {

    @Inject
    private IlluminaRunResource illuminaRunResource;

    private String runName;
    private ZimsIlluminaRun run;

    public void query() {
        run = illuminaRunResource.getRun(runName);
    }

    public String getRunName() {
        return runName;
    }

    public void setRunName(String runName) {
        this.runName = runName;
    }

    public ZimsIlluminaRun getRun() {
        return run;
    }

    public void setRun(ZimsIlluminaRun run) {
        this.run = run;
    }
}

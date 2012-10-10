package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.List;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ResearchProjectForm extends AbstractJsfBean {

    @Inject
    private ResearchProjectDetail detail;

    private List<BspUser> projectManagers = new ArrayList<BspUser>();

    private List<BspUser> scientists = new ArrayList<BspUser>();

    // TODO: integrate with real quotes
    private List<Long> fundingSources = new ArrayList<Long>();

    // TODO: integrate with real sample cohorts
    private List<Long> sampleCohorts = new ArrayList<Long>();

    // TODO: integrate with real IRBs (?)
    private List<Long> irbs = new ArrayList<Long>();

    private boolean irbNotEngaged;

    public void initForm() {
        irbNotEngaged = !detail.getProject().isIrbEngaged();
    }

    public String create() {
        return redirect("list");
    }

    public List<Long> completeFundingSource(String query) {
        return generateIds(query, 5);
    }

    public List<Long> completeSampleCohorts(String query) {
        return generateIds(query, 5);
    }

    public List<Long> completeIrbs(String query) {
        return generateIds(query, 5);
    }

    private List<Long> generateIds(String prefix, int num) {
        List<Long> ids = new ArrayList<Long>();
        for (int i = 0; i < 3; i++) {
            ids.add(Long.parseLong(prefix + i));
        }
        return ids;
    }

    public List<BspUser> getScientists() {
        return scientists;
    }

    public void setScientists(List<BspUser> scientists) {
        this.scientists = scientists;
    }

    public List<BspUser> getProjectManagers() {
        return projectManagers;
    }

    public void setProjectManagers(List<BspUser> projectManagers) {
        this.projectManagers = projectManagers;
    }

    public List<Long> getFundingSources() {
        return fundingSources;
    }

    public void setFundingSources(List<Long> fundingSources) {
        this.fundingSources = fundingSources;
    }

    public List<Long> getSampleCohorts() {
        return sampleCohorts;
    }

    public void setSampleCohorts(List<Long> sampleCohorts) {
        this.sampleCohorts = sampleCohorts;
    }

    public List<Long> getIrbs() {
        return irbs;
    }

    public void setIrbs(List<Long> irbs) {
        this.irbs = irbs;
    }

    public boolean isIrbNotEngaged() {
        return irbNotEngaged;
    }

    public void setIrbNotEngaged(boolean irbNotEngaged) {
        this.irbNotEngaged = irbNotEngaged;
    }
}

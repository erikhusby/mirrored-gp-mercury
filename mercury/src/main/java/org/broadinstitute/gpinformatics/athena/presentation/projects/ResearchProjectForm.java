package org.broadinstitute.gpinformatics.athena.presentation.projects;

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
public class ResearchProjectForm {

    @Inject
    private ResearchProjectDetail detail;

    private Long addPersonId;

    private List<Long> scientists = new ArrayList<Long>();

    private List<Long> projectManagers = new ArrayList<Long>();

    private List<Long> fundingSources = new ArrayList<Long>();

    private List<Long> sampleCohorts = new ArrayList<Long>();

    private List<Long> irbs = new ArrayList<Long>();

    private boolean irbNotEngaged;

    public void initForm() {
        irbNotEngaged = !detail.getProject().isIrbEngaged();
    }

    public List<Long> completePerson(String query) {
        return generateIds(query, 5);
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

    public List<Long> getScientists() {
        return scientists;
    }

    public void setScientists(List<Long> scientists) {
        this.scientists = scientists;
    }

    public List<Long> getProjectManagers() {
        return projectManagers;
    }

    public void setProjectManagers(List<Long> projectManagers) {
        this.projectManagers = projectManagers;
    }

    public Long getAddPersonId() {
        return addPersonId;
    }

    public void setAddPersonId(Long addPersonId) {
        this.addPersonId = addPersonId;
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

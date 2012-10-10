package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
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

    @Inject
    private ResearchProjectDao researchProjectDao;

    private List<BspUser> projectManagers = new ArrayList<BspUser>();

    private List<BspUser> scientists = new ArrayList<BspUser>();

    // TODO: integrate with real quotes
    private List<Long> fundingSources = new ArrayList<Long>();

    // TODO: integrate with real sample cohorts
    private List<Long> sampleCohorts = new ArrayList<Long>();

    // TODO: integrate with real IRBs (?)
    private List<Long> irbs = new ArrayList<Long>();

    public String create() {
        ResearchProject project = detail.getProject();
        if (projectManagers != null) {
            for (BspUser projectManager : projectManagers) {
                project.addPerson(RoleType.PM, projectManager.getUserId());
            }
        }
        if (scientists != null) {
            for (BspUser scientist : scientists) {
                project.addPerson(RoleType.SCIENTIST, scientist.getUserId());
            }
        }
        if (fundingSources != null) {
            for (Long fundingSource : fundingSources) {
                project.addFunding(new ResearchProjectFunding(project, fundingSource.toString()));
            }
        }
        // TODO: sample cohorts
        if (irbs != null) {
            for (Long irb : irbs) {
                // TODO: use correct IRB type
                project.addIrbNumber(new ResearchProjectIRB(project, ResearchProjectIRB.IrbType.OTHER, irb.toString()));
            }
        }

        project.setCreatedBy(14567L);

        researchProjectDao.persist(project);
        addInfoMessage("Research project created.", "Research project \"" + project.getTitle() + "\" has been created.");
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
}

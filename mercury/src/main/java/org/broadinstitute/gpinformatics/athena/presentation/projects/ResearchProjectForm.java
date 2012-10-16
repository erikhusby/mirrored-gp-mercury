package org.broadinstitute.gpinformatics.athena.presentation.projects;

import org.broadinstitute.bsp.client.users.BspUser;
import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.person.RoleType;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectFunding;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPUserList;
import org.broadinstitute.gpinformatics.mercury.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
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

    @Inject
    private BSPUserList bspUserList;

    @Inject
    private FacesContext facesContext;

    private List<BspUser> projectManagers;

    private List<BspUser> broadPIs;

    private List<BspUser> scientists;

    private List<BspUser> externalCollaborators;

    // TODO: integrate with real quotes
    private List<Long> fundingSources;

    // TODO: integrate with real sample cohorts
    private List<Long> sampleCohorts;

    // TODO: change to a text field to be parsed as comma-separated
    private List<Long> irbs;

    public void initForm() {
        // Only initialize the form on postback. Otherwise, we'll leave the form as the user submitted it.
        if (!facesContext.isPostback()) {

            // Add current user as a PM only if this is a new research project being created
            if (detail.getProject().getResearchProjectId() == null) {
                projectManagers = new ArrayList<BspUser>();
                String username = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
                BspUser user = bspUserList.getByUsername(username);
                if (user != null) {
                    projectManagers.add(user);
                } else {
                    projectManagers.add(bspUserList.getUsers().get(0));
                }
            } else {
                projectManagers = makeBspUserList(detail.getProject().getProjectManagers());
                broadPIs = makeBspUserList(detail.getProject().getBroadPIs());
                scientists = makeBspUserList(detail.getProject().getScientists());
                externalCollaborators = makeBspUserList(detail.getProject().getExternalCollaborators());
            }
        }
    }

    private List<BspUser> makeBspUserList(Long[] userIds) {
        List<BspUser> users = new ArrayList<BspUser>();
        for (Long userId : userIds) {
            BspUser user = bspUserList.getById(userId);
            if (user != null) {
                users.add(user);
            }
        }
        return users;
    }

    public String create() {
        // TODO: move some of this logic down into boundary, or deeper!

        ResearchProject project = detail.getProject();
        if (projectManagers != null) {
            for (BspUser projectManager : projectManagers) {
                project.addPerson(RoleType.PM, projectManager.getUserId());
            }
        }
        if (broadPIs != null) {
            for (BspUser broadPI : broadPIs) {
                project.addPerson(RoleType.BROAD_PI, broadPI.getUserId());
            }
        }
        if (scientists != null) {
            for (BspUser scientist : scientists) {
                project.addPerson(RoleType.SCIENTIST, scientist.getUserId());
            }
        }
        if (externalCollaborators != null) {
            for (BspUser externalCollaborator : externalCollaborators) {
                project.addPerson(RoleType.EXTERNAL, externalCollaborator.getUserId());
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
        // TODO: store BspUser in SessionScoped bean on login.
        String username = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        BspUser user = bspUserList.getByUsername(username);
        if (user != null) {
            project.setCreatedBy(user.getUserId());
        } else {
            project.setCreatedBy(1L);
        }

        researchProjectDao.persist(project);
        addInfoMessage("Research project created.", "Research project \"" + project.getTitle() + "\" has been created.");
        return redirect("list");
    }

    public String edit() {
        // TODO: try to do away with merge
        researchProjectDao.getEntityManager().merge(detail.getProject());
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

    public List<BspUser> getProjectManagers() {
        return projectManagers;
    }

    public void setProjectManagers(List<BspUser> projectManagers) {
        this.projectManagers = projectManagers;
    }

    public List<BspUser> getBroadPIs() {
        return broadPIs;
    }

    public void setBroadPIs(List<BspUser> broadPIs) {
        this.broadPIs = broadPIs;
    }

    public List<BspUser> getScientists() {
        return scientists;
    }

    public void setScientists(List<BspUser> scientists) {
        this.scientists = scientists;
    }

    public List<BspUser> getExternalCollaborators() {
        return externalCollaborators;
    }

    public void setExternalCollaborators(List<BspUser> externalCollaborators) {
        this.externalCollaborators = externalCollaborators;
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

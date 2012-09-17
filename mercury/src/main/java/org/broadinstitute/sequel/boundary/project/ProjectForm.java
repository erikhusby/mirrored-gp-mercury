package org.broadinstitute.sequel.boundary.project;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.BasicProject;
import org.broadinstitute.sequel.entity.project.Project;
import org.broadinstitute.sequel.presentation.AbstractJsfBean;

import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
@RequestScoped
public class ProjectForm extends AbstractJsfBean {

    private String projectName;

    @Inject private DB db;
    @Inject private ProjectDetail projectDetail;

    public String create() {
        Project project = new BasicProject(projectName, null);

        try {
            db.addProject(project);
        } catch (IllegalArgumentException e) {

            // should probably not be using FacesContext in a boundary object
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.addMessage(null, new FacesMessage(e.getMessage()));
            return null;
        }

        projectDetail.setProjectName(project.getProjectName());
        // "detail" refers to a JSF view, which probably doesn't belong in a boundary object
        return redirect("detail");
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }
}

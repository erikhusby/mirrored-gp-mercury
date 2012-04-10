package org.broadinstitute.sequel.presentation.converter;

import org.broadinstitute.sequel.entity.DB;
import org.broadinstitute.sequel.entity.project.WorkflowDescription;

import javax.enterprise.context.RequestScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.inject.Inject;

/**
 * Faces converter for WorkflowDescription. Allows WorkflowDescriptions to be
 * used in select* components.
 *
 * This does not need to be configured beyond the @FacesConverter annotation,
 * either globally or at the point of usage, to be used. Injection of DAO relies
 * on Seam Faces.
 *
 * This must be @RequestScoped as described by the warning at
 * http://docs.jboss.org/seam/3/3.1.0.Final/reference/en-US/html/artifacts.html#enhanced_artifacts.
 *
 * @author breilly
 */
@RequestScoped
@FacesConverter(forClass = WorkflowDescription.class)
public class WorkflowDescriptionConverter implements Converter {

    @Inject private DB db;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return db.findByWorkflowDescriptionName(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object value) {
        return ((WorkflowDescription) value).getWorkflowName();
    }
}

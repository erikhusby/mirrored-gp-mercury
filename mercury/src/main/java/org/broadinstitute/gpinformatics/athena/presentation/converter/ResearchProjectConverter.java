package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.control.dao.ResearchProjectDao;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProject;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class ResearchProjectConverter implements Converter {
    @Inject
    private ResearchProjectDao researchProjectDao;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        ResearchProject researchProject = researchProjectDao.findByBusinessKey(value);
        researchProject.setOriginalTitle(researchProject.getTitle());
        return researchProject;
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        // check for null because the converter might be passed null due to an ajax partial update
        return object == null ? "" : ((ResearchProject) object).getBusinessKey();
    }
}

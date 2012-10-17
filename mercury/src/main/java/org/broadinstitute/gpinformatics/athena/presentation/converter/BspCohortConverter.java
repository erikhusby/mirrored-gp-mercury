package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.entity.project.Cohort;
import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPCohortList;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
public class BspCohortConverter implements Converter {

    @Inject
    private BSPCohortList userList;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return userList.getById(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        Cohort cohort = (Cohort) object;
        if (cohort == null) {
            return null;
        }

        return cohort.getCohortId();
    }
}

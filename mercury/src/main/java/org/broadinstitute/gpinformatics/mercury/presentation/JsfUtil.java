package org.broadinstitute.gpinformatics.mercury.presentation;

import javax.enterprise.inject.Produces;
import javax.faces.context.FacesContext;

/**
 * @author breilly
 */
public class JsfUtil {

    @Produces
    public FacesContext getFacesContext() {
        return FacesContext.getCurrentInstance();
    }
}

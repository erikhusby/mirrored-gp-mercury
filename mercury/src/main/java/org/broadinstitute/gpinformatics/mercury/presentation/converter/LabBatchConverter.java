package org.broadinstitute.gpinformatics.mercury.presentation.converter;

import org.broadinstitute.gpinformatics.athena.presentation.converter.AbstractConverter;
import org.broadinstitute.gpinformatics.mercury.control.dao.workflow.LabBatchDAO;
import org.broadinstitute.gpinformatics.mercury.entity.workflow.LabBatch;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author Scott Matthews
 *         Date: 12/18/12
 *         Time: 1:45 PM
 */
@Named
public class LabBatchConverter extends AbstractConverter {

    @Inject
    private LabBatchDAO batchDAO;

    @Override
    public LabBatch getAsObject(FacesContext facesContext, UIComponent uiComponent, String s) {
        return batchDAO.findByName(s);
    }

    @Override
    public String getAsString(FacesContext facesContext, UIComponent uiComponent, Object o) {
        return o == null ? "" : ((LabBatch) o).getBatchName();
    }
}

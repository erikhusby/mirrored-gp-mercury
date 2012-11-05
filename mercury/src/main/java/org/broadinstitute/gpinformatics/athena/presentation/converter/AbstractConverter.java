package org.broadinstitute.gpinformatics.athena.presentation.converter;

import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.event.ValueChangeEvent;

/**
 * @author breilly
 */
public abstract class AbstractConverter implements Converter {
    /**
     * Value-change listener do the work that would normally be done in the Update Model Values phase. This guarantees
     * that the model value that a view parameter is bound to will be set even when rendering the view after a
     * validation error. Set this as a valueChangeListener on the f:viewParam component:
     *
     *   <f:viewParam name="foo" value="#{fooForm.foo}" converter="#{fooConverter}" valueChangeListener="#{fooConverter.updateModel}">
     *
     * Solution derived from: https://cwiki.apache.org/MYFACES/how-the-immediate-attribute-works.html
     *
     * @param event    the value-change event
     */
    public void updateModel(ValueChangeEvent event) {
        UIInput input = (UIInput) event.getComponent();
        input.getValueExpression("value").setValue(FacesContext.getCurrentInstance().getELContext(), event.getNewValue());
        // prevent setter being called again during update-model phase
        input.setLocalValueSet(false);
    }
}

package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.infrastructure.bsp.BSPMaterialTypeList;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class MaterialTypeConverter implements Converter {

    @Inject
    private BSPMaterialTypeList materialTypeCache;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null) {
            return null;
        }

        return materialTypeCache.getByFullName(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        org.broadinstitute.bsp.client.sample.MaterialType materialType =
                (org.broadinstitute.bsp.client.sample.MaterialType) object;
        if (materialType == null) {
            return null;
        }
        return materialType.getFullName();
    }
}

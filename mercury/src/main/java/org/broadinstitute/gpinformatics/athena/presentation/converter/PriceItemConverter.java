package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.infrastructure.quote.PriceItem;
import org.broadinstitute.gpinformatics.infrastructure.quote.PriceListCache;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

@Named
public class PriceItemConverter implements Converter {

    @Inject
    private PriceListCache priceListCache;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        if (value == null) {
            return null;
        }

        return priceListCache.findByConcatenatedKey(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        PriceItem priceItem = (PriceItem) object;
        if (priceItem == null) {
            return null;
        }
        return priceItem.getPlatformName() + "|" + priceItem.getCategoryName() + "|" + priceItem.getName();
    }
}

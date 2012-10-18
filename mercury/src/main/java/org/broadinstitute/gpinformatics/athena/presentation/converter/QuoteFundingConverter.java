package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.infrastructure.quote.Funding;
import org.broadinstitute.gpinformatics.infrastructure.quote.QuoteFundingList;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * @author breilly
 */
@Named
public class QuoteFundingConverter implements Converter {

    @Inject
    private QuoteFundingList fundingList;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return fundingList.getById(value);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        Funding funding = (Funding) object;
        if (funding == null) {
            return null;
        }

        return funding.getFundingTypeAndName();
    }
}

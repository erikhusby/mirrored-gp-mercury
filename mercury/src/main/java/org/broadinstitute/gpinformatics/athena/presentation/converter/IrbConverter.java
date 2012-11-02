package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;

@Named
public class IrbConverter implements Converter {

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        int index = value.trim().lastIndexOf(":");

        if (index < 1) {
            return new Irb("undefined", null);
        }

        String name = value.substring(0, index).trim();
        String typeString = value.substring(index + 1).trim();
        ResearchProjectIRB.IrbType type = ResearchProjectIRB.IrbType.findByDisplayName(typeString);

        return new Irb(name, type);
    }

    @Override
    public String getAsString(FacesContext context, UIComponent component, Object object) {
        Irb irb = (Irb) object;
        if (irb == null) {
            return null;
        }

        return getFullIrbString(irb.getName(), irb.getIrbType().getDisplayName());
    }

    private String getFullIrbString(String irbName, String irbType) {
        String returnValue = irbName;
        if (irbType != null) {
            returnValue += " : " + irbType;
        }

        return returnValue;
    }

    public Irb createIrb(String irbName, ResearchProjectIRB.IrbType type, int irbNameMaxLength) {
        if (irbNameMaxLength <= type.getDisplayName().length() + 4) {
            throw new IllegalArgumentException("The max length of IRB does not allow for a name");
        }

        String returnName = irbName;
        int lengthOfFullString = getFullIrbString(irbName, type.getDisplayName()).length();
        if (lengthOfFullString > irbNameMaxLength) {
            returnName = irbName.substring(0, irbName.length() - (lengthOfFullString - irbNameMaxLength));
        }

        return new Irb(returnName, type);
    }
}

package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Named
public class IrbConverter implements Converter {

    private static final int IRB_NAME_MAX_LENGTH = 250;

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        return getAsObject(value);
    }

    public static Object getAsObject(String value) {
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

    public List<Irb> completeIrbs(String query) {
        String trimmedQuery = query.trim();

        if (trimmedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        List<Irb> irbsForQuery = new ArrayList<Irb>();
        for (ResearchProjectIRB.IrbType type : ResearchProjectIRB.IrbType.values()) {
            Irb irb = createIrb(trimmedQuery, type, IRB_NAME_MAX_LENGTH);
            irbsForQuery.add(irb);
        }

        return irbsForQuery;
    }

    public int getIrbMaxLength() {
        return IRB_NAME_MAX_LENGTH;
    }

    /**
     * This creates a valid IRB object out of the type.
     *
     * @param irbName The name of the irb
     * @param type The irb type
     * @param irbNameMaxLength The maximum length name that can be display
     *
     * @return The irb object
     */
    public Irb createIrb(String irbName, ResearchProjectIRB.IrbType type, int irbNameMaxLength) {

        // If the type + the space-colon-space is longer than max length, then we cannot have a unique name.
        if (type.getDisplayName().length() + 4 > irbNameMaxLength) {
            throw new IllegalArgumentException("IRB type: " + type.getDisplayName() + " is too long to allow for a name");
        }

        // Strip off any long name to the maximum number of characters
        String returnName = irbName;
        int lengthOfFullString = getFullIrbString(irbName, type.getDisplayName()).length();
        if (lengthOfFullString > irbNameMaxLength) {
            returnName = irbName.substring(0, irbName.length() - (lengthOfFullString - irbNameMaxLength));
        }

        return new Irb(returnName, type);
    }

    public static List<Irb> getIrbs(String irbList) {
        if (irbList == null) {
            return Collections.emptyList();
        }

        String[] irbArray = irbList.split(",");
        List<Irb> irbs = new ArrayList<Irb> ();
        for (String irb : irbArray) {
            irbs.add((Irb) IrbConverter.getAsObject(irb.trim()));
        }

        return irbs;
    }
}

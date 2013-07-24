package org.broadinstitute.gpinformatics.athena.presentation.converter;

import org.apache.commons.lang3.StringUtils;
import org.broadinstitute.gpinformatics.athena.entity.project.Irb;
import org.broadinstitute.gpinformatics.athena.entity.project.ResearchProjectIRB;
import org.broadinstitute.gpinformatics.infrastructure.common.TokenInput;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IrbConverter {

    private static final int IRB_NAME_MAX_LENGTH = 250;

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

    public static List<Irb> getIrbs(String irbList) {
        if (irbList == null) {
            return Collections.emptyList();
        }

        String[] irbArray = irbList.split(TokenInput.TOKEN_INPUT_SEPARATOR);
        List<Irb> irbs = new ArrayList<>();
        for (String irb : irbArray) {
            irbs.add((Irb) IrbConverter.getAsObject(irb.trim()));
        }

        return irbs;
    }

    public static String getJsonString(String query) throws JSONException {
        JSONArray itemList = new JSONArray();

        String trimmedQuery = query.trim();
        if (!StringUtils.isBlank(trimmedQuery)) {
            for (ResearchProjectIRB.IrbType type : ResearchProjectIRB.IrbType.values()) {
                Irb irb = createIrb(trimmedQuery, type, IRB_NAME_MAX_LENGTH);
                itemList.put(TokenInput.getJSONObject(irb.getDisplayName(), irb.getDisplayName()));
            }
        }
        return itemList.toString();
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
    public static Irb createIrb(String irbName, ResearchProjectIRB.IrbType type, int irbNameMaxLength) {

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

    private static String getFullIrbString(String irbName, String irbType) {
        String returnValue = irbName;
        if (irbType != null) {
            returnValue += " : " + irbType;
        }

        return returnValue;
    }

    public static String getIrbCompleteData(String[] irbNumbers) throws JSONException {
        JSONArray itemList = new JSONArray();
        for (String irbNumber : irbNumbers) {
            itemList.put(TokenInput.getJSONObject(irbNumber, irbNumber));
        }

        return itemList.toString();
    }
}

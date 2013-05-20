package org.broadinstitute.gpinformatics.athena.presentation;

/**
 * This class is for any generic {@link Displayable} item that you might need to display in the UI. A great helper
 * object for your JSP code for various things that have a business key and name but not much else.
 *
 * @author <a href="mailto:dinsmore@broadinstitute.org">Michael Dinsmore</a>
 */
public class DisplayableItem implements Displayable {
    private String businessKey;

    private String displayName;

    public DisplayableItem (String businessKey, String displayName) {
        this.businessKey = businessKey;
        this.displayName = displayName;
    }

    public String getBusinessKey() {
        return businessKey;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }
}

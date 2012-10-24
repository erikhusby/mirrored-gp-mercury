package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

/**
* @author Scott Matthews
*         Date: 10/11/12
*         Time: 9:31 AM
*/
public class NextTransition {
    private String self;
    private String description;
    private String iconUrl;
    private String id;

    public NextTransition ( String selfIn, String descriptionIn, String iconUrlIn, String idIn ) {
        self = selfIn;
        description = descriptionIn;
        iconUrl = iconUrlIn;
        id = idIn;
    }

    public String getSelf ( ) {
        return self;
    }

    public String getDescription ( ) {
        return description;
    }

    public String getIconUrl ( ) {
        return iconUrl;
    }

    public String getId ( ) {
        return id;
    }
}

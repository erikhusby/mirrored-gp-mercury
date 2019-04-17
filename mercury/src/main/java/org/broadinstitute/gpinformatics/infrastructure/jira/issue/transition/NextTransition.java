package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

/**
* represents information of the next status that a Jira ticket can transition to.
*/
public class NextTransition {
    private String self;
    private String name;
    private String description;
    private String iconUrl;
    private String id;

    public NextTransition() {
    }

    public NextTransition (String selfIn, String nameIn, String descriptionIn, String iconUrlIn, String idIn) {
        self = selfIn;
        name = nameIn;
        description = descriptionIn;
        iconUrl = iconUrlIn;
        id = idIn;
    }


    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}

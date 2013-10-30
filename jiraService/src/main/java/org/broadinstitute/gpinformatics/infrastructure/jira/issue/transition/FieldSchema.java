package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

/**
* @author Scott Matthews
*         Date: 10/11/12
*         Time: 9:31 AM
*/
public class FieldSchema {

    private String type;
    private String system;
    private String items;
    private String custom;
    private String customId;

    public FieldSchema() {
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSystem() {
        return system;
    }

    public void setSystem(String system) {
        this.system = system;
    }

    public String getItems() {
        return items;
    }

    public void setItems(String items) {
        this.items = items;
    }

    public String getCustom() {
        return custom;
    }

    public void setCustom(String custom) {
        this.custom = custom;
    }

    public String getCustomId() {
        return customId;
    }

    public void setCustomId(String customId) {
        this.customId = customId;
    }
}

package org.broadinstitute.gpinformatics.infrastructure.jira.issue.transition;

/**
* @author Scott Matthews
*         Date: 10/11/12
*         Time: 9:31 AM
*/
public class FieldSchema {

    private String type;
    private String items;
    private String custom;
    private String customId;

    public FieldSchema ( String typeIn, String itemsIn, String customIn, String customIdIn ) {
        type = typeIn;
        items = itemsIn;
        custom = customIn;
        customId = customIdIn;
    }

    public String getType ( ) {
        return type;
    }

    public String getItems ( ) {
        return items;
    }

    public String getCustom ( ) {
        return custom;
    }

    public String getCustomId ( ) {
        return customId;
    }
}

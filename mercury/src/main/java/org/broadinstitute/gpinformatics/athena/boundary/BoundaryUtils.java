package org.broadinstitute.gpinformatics.athena.boundary;

import org.broadinstitute.gpinformatics.athena.entity.common.StatusType;

import javax.faces.model.SelectItem;
import java.util.ArrayList;
import java.util.List;

/**
 * Collection of useful methods for working with xhtml pages.
 */
public class BoundaryUtils {
    public static List<SelectItem> buildEnumFilterList(StatusType[] statuses) {
        List<SelectItem> items = new ArrayList<SelectItem>();
        items.add(new SelectItem("", "Any"));
        for (StatusType status : statuses) {
            items.add(new SelectItem(status.getDisplayName()));
        }
        return items;
    }
}

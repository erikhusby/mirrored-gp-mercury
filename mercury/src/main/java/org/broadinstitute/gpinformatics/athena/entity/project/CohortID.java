package org.broadinstitute.gpinformatics.athena.entity.project;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/12/12
 * Time: 3:37 PM
 */
public class CohortID {

    public final String value;

    public CohortID(String value) {
        if ((value == null) || StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Id is invalid. Must be non-null and non-empty.");
        }
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}

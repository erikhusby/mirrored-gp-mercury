package org.broadinstitute.pmbridge.entity.common;

import org.apache.commons.lang.StringUtils;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/11/12
 * Time: 3:39 PM
 */
public class Id {

    public final String value;

    public Id(String value) {
        if ((value == null ) || StringUtils.isBlank(value)) {
            throw new IllegalArgumentException("Id is invalid. Must be non-null and non-empty.");
        }
        this.value = value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Id)) return false;

        final Id id = (Id) o;

        if (!value.equals(id.value)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return "Id{" +
                "value='" + value + '\'' +
                '}';
    }

}

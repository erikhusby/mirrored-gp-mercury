package org.broadinstitute.pmbridge.entity.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

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
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return value;
    }
}

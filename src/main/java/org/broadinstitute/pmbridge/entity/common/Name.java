package org.broadinstitute.pmbridge.entity.common;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/11/12
 * Time: 3:57 PM
 */
public class Name {

    public final String name;
    public final static String UNSPECIFIED = "Unspecified";

    public Name(String name) {
        if ((name == null ) || StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Value for name is invalid. Must be non-null and non-empty.");
        }
        this.name = name;
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
        return ToStringBuilder.reflectionToString(this);
    }
}

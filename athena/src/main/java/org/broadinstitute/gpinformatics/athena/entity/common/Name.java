package org.broadinstitute.gpinformatics.athena.entity.common;

import org.apache.commons.lang.StringUtils;

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
        if ((name == null) || StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Value for name is invalid. Must be non-null and non-empty. Name supplied was : " + name);
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Name)) return false;

        final Name name1 = (Name) o;

        if (!name.equals(name1.name)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "Name{" +
                "name='" + name + '\'' +
                '}';
    }
}

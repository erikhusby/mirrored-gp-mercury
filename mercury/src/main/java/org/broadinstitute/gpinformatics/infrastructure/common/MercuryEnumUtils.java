package org.broadinstitute.gpinformatics.infrastructure.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MercuryEnumUtils {

    /**
     * Convert a list of enums to a list of enum names.
     *
     * @param enums the enums to convert
     *
     * @return the strings
     */
    public static <T extends Enum<?>> List<String> convertToStrings(Collection<T> enums) {
        List<String> names = new ArrayList<>(enums.size());
        for (T enumInstance : enums) {
            names.add(enumInstance.name());
        }

        return names;
    }
}

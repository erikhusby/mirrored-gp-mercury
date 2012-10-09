package org.broadinstitute.gpinformatics.athena.entity.common;

import java.util.Collection;

/**
 * @author Scott Matthews
 *         Date: 10/9/12
 *         Time: 1:55 PM
 */
public class AthenaUtilities {

    public static String flattenCollectionOfStrings(Collection<String> stringCollection) {
        return flattenCollectionOfStrings(stringCollection, ", ");
    }

    public static String flattenCollectionOfStrings(Collection<String> stringCollection,
                                                    String delimiter) {

        StringBuilder flattened = new StringBuilder();

        boolean first =true;

        for(String currentValue:stringCollection) {
            if(!first) {
                flattened.append(delimiter);
            }
            flattened.append(currentValue);
        }

        return flattened.toString();
    }

}

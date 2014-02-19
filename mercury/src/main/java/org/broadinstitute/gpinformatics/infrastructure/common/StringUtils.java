/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2014 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;

public class StringUtils {
    /**
     * Splits camel case input string into words for example productOrder would return "product order"
     *
     * @see <a href="http://stackoverflow.com/questions/2559759/how-do-i-convert-camelcase-into-human-readable-names-in-java">How do I convert CamelCase into human-readable names in Java?</a>
     */
    public static String splitCamelCase(String inputString) {
        return inputString.replaceAll(
                String.format("%s|%s|%s",
                        "(?<=[A-Z])(?=[A-Z][a-z])",
                        "(?<=[^A-Z])(?=[A-Z])",
                        "(?<=[A-Za-z])(?=[^A-Za-z])"), " "
        );
    }
}

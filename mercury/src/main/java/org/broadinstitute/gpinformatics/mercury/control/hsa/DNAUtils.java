package org.broadinstitute.gpinformatics.mercury.control.hsa;

import org.apache.commons.lang3.StringUtils;

public class DNAUtils {

    public static String reverseComplement(String dna) {
        String reverse = StringUtils.reverse(dna).toUpperCase();
        StringBuilder newString = new StringBuilder();
        for (int i = 0; i < reverse.length(); i++) {
            if (reverse.charAt(i) == 'A') {
                newString.append("T");
            } else if (reverse.charAt(i) == 'T') {
                newString.append("A");
            } else if (reverse.charAt(i) == 'G') {
                newString.append("C");
            } else if (reverse.charAt(i) == 'C') {
                newString.append("G");
            }
        }
        return newString.toString();
    }
}

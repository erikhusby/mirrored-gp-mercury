package org.broadinstitute.gpinformatics.infrastructure.common;

import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orders things that look like PDO-2, PDO-100, RP-3 considering first the component before the dash lexically and
 * the component after the dash numerically.
 *
 */
public class BusinessKeyComparator implements Comparator<String> {

    /**
     * Generic pattern for business keys ABC-123 where the alpha part is at least 2 uppercase alpha characters and
     * the numeric part is at least one digit
     */
    private static final Pattern BUSINESS_KEY_PATTERN = Pattern.compile("^([A-Z]{2,})-([0-9]{1,})$");

    @Override
    public int compare(String s, String s1) {
        if (s == s1) {
            return 0;
        }

        if (s == null) {
            return -1;
        }

        if (s1 == null) {
            return 1;
        }

        Matcher matcher = BUSINESS_KEY_PATTERN.matcher(s);

        if (! matcher.matches()) {
            return -1;
        }

        Matcher matcher1 = BUSINESS_KEY_PATTERN.matcher(s1);
        if (! matcher1.matches() ) {
            return 1;
        }

        String key = matcher.group(1);
        String key1 = matcher1.group(1);
        if (! key.equals(key1)) {
            return key.compareTo(key1);
        }


        Integer issue = Integer.valueOf(matcher.group(2));
        Integer issue1 = Integer.valueOf(matcher1.group(2));

        return issue.compareTo(issue1);

    }
}

package org.broadinstitute.gpinformatics.mercury.boundary.lims;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class StringsTest {

    protected static final int minLength = 4;

    /**
     * Return true if the character is printable IN ASCII. Not using
     * Character.isLetterOrDigit(); applies to all unicode ranges
     */
    protected static boolean isStringChar(char ch) {
        if (ch >= 'a' && ch <= 'z')
            return true;
        if (ch >= 'A' && ch <= 'Z')
            return true;
        if (ch >= '0' && ch <= '9')
            return true;
        switch (ch) {
        case '/':
        case '-':
        case ':':
        case '.':
        case ',':
        case '_':
        case '$':
        case '%':
        case '\'':
        case '(':
        case ')':
        case '[':
        case ']':
        case '<':
        case '>':
            return true;
        }
        return false;
    }

    /**
     * Process one file
     */
    public static List<String> process(String fileName, InputStream inStream) {
        List<String> output = new ArrayList<>();
        try {
            int i;
            char ch;

            // This line alone cuts the runtime by about 66% on large files.
            BufferedInputStream is = new BufferedInputStream(inStream);
            StringBuffer sb = new StringBuffer();

            // Read a byte, cast it to char, check if part of printable string.
            while ((i = is.read()) != -1) {
                ch = (char) i;
                if (isStringChar(ch) || (sb.length() > 0 && ch == ' '))
                // If so, build up string.
                {
                    sb.append(ch);
                } else {
                    // if not, see if anything to output.
                    if (sb.length() == 0) {
                        continue;
                    }
                    if (sb.length() >= minLength) {
                        //report(fileName, sb);
                        output.add(sb.toString());
                    }
                    sb.setLength(0);
                }
            }
            is.close();
        } catch (IOException e) {
            System.out.println("IOException: " + e);
        }
        return output;
    }

    protected static void report(String fName, StringBuffer theString) {
        System.out.println(theString);
    }
}

/*
 * Copyright (c) Ian F. Darwin, http://www.darwinsys.com/, 1996-2002.
 * All rights reserved. Software written by Ian F. Darwin and others.
 * $Id: LICENSE,v 1.8 2004/02/09 03:33:38 ian Exp $
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS''
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Java, the Duke mascot, and all variants of Sun's Java "steaming coffee
 * cup" logo are trademarks of Sun Microsystems. Sun's, and James Gosling's,
 * pioneering role in inventing and promulgating (and standardizing) the Java
 * language and environment is gratefully acknowledged.
 *
 * The pioneering role of Dennis Ritchie and Bjarne Stroustrup, of AT&T, for
 * inventing predecessor languages C and C++ is also gratefully acknowledged.
 */

package org.broadinstitute.gpinformatics.infrastructure.common;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Strings {

    private static final Log log = LogFactory.getLog(Strings.class);

    private static final int minLength = 4;

    /**
     * Return true if the character is printable IN ASCII. Not using
     * Character.isLetterOrDigit(); applies to all unicode ranges
     */
    private static boolean isStringChar(char ch) {
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
    public static List<String> process(InputStream inStream) {
        List<String> output = new ArrayList<>();
        try {
            int i;
            char ch;

            // This line alone cuts the runtime by about 66% on large files.
            BufferedInputStream is = new BufferedInputStream(inStream);
            StringBuilder sb = new StringBuilder();

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
                        output.add(sb.toString());
                    }
                    sb.setLength(0);
                }
            }
            is.close();
        } catch (IOException e) {
            log.error("Failed to parse file", e);
        }
        return output;
    }
}
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

package org.broadinstitute.gpinformatics.infrastructure.parsers.poi;

import org.broadinstitute.gpinformatics.infrastructure.parsers.ColumnHeader;
import org.broadinstitute.gpinformatics.infrastructure.widget.daterange.DateUtils;
import org.testng.Assert;

import java.text.ParseException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Validations common to all ColumnHeaders
 */
public class PoiSpreadsheetValidator {
    /**
     * Validate row data based on ColumnHeader.
     */
    public static <T extends ColumnHeader> void validateSpreadsheetRow(Map<String, String> spreadsheetRowValues, Class<T> headerType) {
        for (String key : spreadsheetRowValues.keySet()) {
            ColumnHeader header = getHeaderByKey(key, headerType);
            assertThat(header, notNullValue());
            String value = spreadsheetRowValues.get(header.getText());

            if (header.isRequiredValue()) {
                assertThat(value, not(isEmptyOrNullString()));
                if (header.isStringColumn()) {
                    assertThat(value, not(isEmptyOrNullString()));
                }
            }

            if (header.isDateColumn()) {
                try {
                    DateUtils.parseDate(value);
                } catch (ParseException e) {
                    Assert.fail("could not parse " + value, e);
                }
            }
        }
    }


    private static <T extends ColumnHeader> T getHeaderByKey(String key, Class<T> headerType) {
        for (T header : headerType.getEnumConstants()) {
            if (key.equals(header.getText())) {
                return header;
            }
        }
        return null;
    }
}

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

package org.broadinstitute.gpinformatics.athena.presentation;

import org.json.JSONException;

public interface JsonDecorator {
    /**
     * Create a json representation of an object.
     *
     * @return a json representation of an object.
     *
     * @throws JSONException
     */
    String getJson() throws JSONException;
}

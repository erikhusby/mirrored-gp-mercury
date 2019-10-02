/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2019 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */

package org.broadinstitute.gpinformatics.mercury.integration.sap;

import org.broadinstitute.gpinformatics.infrastructure.ExternalServiceRuntimeException;
import org.broadinstitute.gpinformatics.infrastructure.sap.SAPProductPriceCache;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Alternative;

@Alternative
@ApplicationScoped
public class SAPProductPriceCacheThrowsRuntimeExceptions extends SAPProductPriceCache {

    public static final String RUNTIME_ERROR_MESSAGE = "There was an error";

    @Override
    public synchronized void refreshCache() {
        throw new ExternalServiceRuntimeException(RUNTIME_ERROR_MESSAGE);
    }
}

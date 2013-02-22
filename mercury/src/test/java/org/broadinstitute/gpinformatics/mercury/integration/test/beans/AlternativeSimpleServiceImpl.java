package org.broadinstitute.gpinformatics.mercury.integration.test.beans;

import javax.enterprise.inject.Alternative;

/**
 * @author breilly
 */
@Alternative
public class AlternativeSimpleServiceImpl implements SimpleService {

    @Override
    public String getName() {
        return "AlternativeSimpleServiceImpl";
    }
}

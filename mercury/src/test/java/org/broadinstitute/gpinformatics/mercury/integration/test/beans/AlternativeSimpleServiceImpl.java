package org.broadinstitute.gpinformatics.mercury.integration.test.beans;

import org.broadinstitute.gpinformatics.mercury.presentation.Theme;

import javax.enterprise.inject.Alternative;
import javax.inject.Inject;

/**
 * @author breilly
 */
@Alternative
public class AlternativeSimpleServiceImpl implements SimpleService {

    @Inject
    private Theme theme;

    @Override
    public String getName() {
        return "AlternativeSimpleServiceImpl";
    }
}

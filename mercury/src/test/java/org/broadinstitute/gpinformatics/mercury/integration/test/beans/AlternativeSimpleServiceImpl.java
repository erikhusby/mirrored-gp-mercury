package org.broadinstitute.gpinformatics.mercury.integration.test.beans;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Alternative;

/**
 * @author breilly
 */
@Alternative
@Dependent
public class AlternativeSimpleServiceImpl implements SimpleService {

    public AlternativeSimpleServiceImpl(){}

    @Override
    public String getName() {
        return "AlternativeSimpleServiceImpl";
    }
}

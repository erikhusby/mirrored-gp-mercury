package org.broadinstitute.gpinformatics.mercury.integration.test.beans;

import javax.enterprise.context.Dependent;
import javax.enterprise.inject.Default;

/**
 * @author breilly
 */
@Default
@Dependent
public class SimpleServiceImpl implements SimpleService {

    @Override
    public String getName() {
        return "SimpleServiceImpl";
    }
}

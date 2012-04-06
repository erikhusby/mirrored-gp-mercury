package org.broadinstitute.sequel.test.beans;

import org.broadinstitute.sequel.presentation.Theme;

import javax.inject.Inject;

/**
 * @author breilly
 */
public class SimpleServiceImpl implements SimpleService {

    @Override
    public String getName() {
        return "SimpleServiceImpl";
    }
}

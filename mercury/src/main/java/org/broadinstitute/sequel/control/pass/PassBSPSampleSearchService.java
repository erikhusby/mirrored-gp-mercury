package org.broadinstitute.sequel.control.pass;


import org.broadinstitute.sequel.presentation.pass.PassSample;

import java.util.List;

public interface PassBSPSampleSearchService {

    void lookupSampleDataInBSP(List<PassSample> samples);

}

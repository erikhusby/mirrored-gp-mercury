package org.broadinstitute.gpinformatics.mercury.control.pass;


import org.broadinstitute.gpinformatics.mercury.presentation.pass.PassSample;

import java.util.List;

public interface PassBSPSampleSearchService {

    void lookupSampleDataInBSP(List<PassSample> samples);

}

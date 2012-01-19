package org.broadinstitute.sequel;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;

import javax.enterprise.inject.Alternative;
import java.util.Collection;

@Alternative
public class QABSPConnector implements BSPConnector {

    private static Log gLog = LogFactory.getLog(QABSPConnector.class);


    @Override
    public BSPPlatingResponse sendAliquotRequests(Collection<BSPPlatingRequest> aliquotRequests) {
        throw new RuntimeException("qa aliquotter");
    }
}

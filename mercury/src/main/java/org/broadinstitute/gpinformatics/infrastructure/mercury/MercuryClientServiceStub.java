package org.broadinstitute.gpinformatics.infrastructure.mercury;

import org.broadinstitute.gpinformatics.athena.presentation.DisplayableItem;
import org.broadinstitute.gpinformatics.infrastructure.deployment.Stub;

import javax.enterprise.inject.Alternative;
import java.util.Collection;
import java.util.Collections;

/**
 * Implementation of MercuryClientProducer, for test purposes.
 * @author epolk
 */
@Stub
@Alternative
public class MercuryClientServiceStub implements MercuryClientService {
    private static final Long TEST_CREATOR = 1111L;

    @Override
    public Collection<DisplayableItem> getReferenceSequences() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getSequenceAligners() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getAnalysisTypes() {
        return Collections.emptyList();
    }

    @Override
    public Collection<DisplayableItem> getReagentDesigns() {
        return Collections.emptyList();
    }

    @Override
    public DisplayableItem getReagentDesign(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getSequenceAligner(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getReferenceSequence(String businessKey) {
        return null;
    }

    @Override
    public DisplayableItem getAnalysisType(String businessKey) {
        return null;
    }
}

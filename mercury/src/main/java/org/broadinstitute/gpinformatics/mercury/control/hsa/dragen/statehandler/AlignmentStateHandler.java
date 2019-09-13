package org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.statehandler;

import org.broadinstitute.gpinformatics.mercury.control.hsa.FastQListBuilder;
import org.broadinstitute.gpinformatics.mercury.control.hsa.dragen.AlignmentTask;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.AlignmentState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Task;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;
import java.io.IOException;

@Dependent
public class AlignmentStateHandler extends StateHandler {

    @Inject
    private FastQListBuilder fastQListBuilder;

    @Override
    public boolean onEnter(State state) throws IOException {

        if (!OrmUtil.proxySafeIsInstance(state, AlignmentState.class)) {
            throw new RuntimeException("Expect only alignment states");
        }

        AlignmentState alignmentState = OrmUtil.proxySafeCast(state, AlignmentState.class);

        for (Task t: alignmentState.getTasks()) {
            if (OrmUtil.proxySafeIsInstance(t, AlignmentTask.class)) {
                AlignmentTask alignmentTask = OrmUtil.proxySafeCast(t, AlignmentTask.class);
                if (!alignmentTask.getOutputDir().exists()) {
                    alignmentTask.getOutputDir().mkdir();
                }

                if (!alignmentTask.getFastQList().exists()) {
                    MercurySample mercurySample = alignmentState.getMercurySamples().iterator().next();
                    boolean foundSample = fastQListBuilder.buildSingle(
                            alignmentState.getSequencingRunChambers().iterator().next(),
                            mercurySample, alignmentTask.getFastQList());
                    if (!foundSample) {
                        throw new RuntimeException(
                                "Failed to find sample " + mercurySample.getSampleKey() + " in alignment task " + alignmentTask.getTaskId() );
                    }
                }
            }
        }
        return true;
    }
}

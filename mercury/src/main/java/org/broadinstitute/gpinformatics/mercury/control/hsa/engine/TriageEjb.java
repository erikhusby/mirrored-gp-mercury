package org.broadinstitute.gpinformatics.mercury.control.hsa.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.broadinstitute.gpinformatics.mercury.control.dao.hsa.StateMachineDao;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.FiniteStateMachine;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.GenericState;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.State;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.Status;
import org.broadinstitute.gpinformatics.mercury.control.hsa.state.TriageStateMachineDecorator;
import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;

import javax.ejb.Stateful;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Stateful
@RequestScoped
public class TriageEjb {

    private static final Log log = LogFactory.getLog(TriageEjb.class);

    @Inject
    private StateMachineDao stateMachineDao;

    public TriageStateMachineDecorator findOrCreateTriageStateMachine() {
        FiniteStateMachine fsm = stateMachineDao.findByIdentifier(TriageStateMachineDecorator.NAME);
        if (fsm == null) {
            fsm = new FiniteStateMachine();
            fsm.setStateMachineName(TriageStateMachineDecorator.NAME);
            fsm.setStatus(Status.TRIAGE);

            List<State> states = new ArrayList<>();
            State state = new GenericState("OverrideInSpec", fsm, Collections.emptySet());
            states.add(state);

            fsm.setStates(states);
        }
        return new TriageStateMachineDecorator(fsm);
    }

    // TODO JW comment
    public void addToOverrideState(Collection<MercurySample> mercurySamples, String overrideComment) {
        TriageStateMachineDecorator triageStateMachine = findOrCreateTriageStateMachine();
        triageStateMachine.getOverrideInSpecState().getMercurySamples().addAll(mercurySamples);
        stateMachineDao.persist(triageStateMachine.getFiniteStateMachine());
    }

    public Set<MercurySample> fetchSamplesInSpecOverride() {
        TriageStateMachineDecorator triageStateMachine = findOrCreateTriageStateMachine();
        return triageStateMachine.getOverrideInSpecState().getMercurySamples();
    }
}

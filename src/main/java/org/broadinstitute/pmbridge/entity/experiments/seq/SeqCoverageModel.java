package org.broadinstitute.pmbridge.entity.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:27 AM
 */
public abstract class SeqCoverageModel {

//    protected abstract SeqCoverageModel getConcreteModel();

    protected abstract CoverageModelType getConcreteModelType();

}

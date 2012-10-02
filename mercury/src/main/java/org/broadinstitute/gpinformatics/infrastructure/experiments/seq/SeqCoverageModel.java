package org.broadinstitute.gpinformatics.infrastructure.experiments.seq;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 5/18/12
 * Time: 11:27 AM
 */
public abstract class SeqCoverageModel {

    protected abstract CoverageModelType getConcreteModelType();

}

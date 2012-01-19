package org.broadinstitute.sequel;

public interface DevelopmentProject extends Project {

    public Iterable<ExperimentalCondition> getConditions();


    public static interface ExperimentalCondition {

        public String getName();
    }
}

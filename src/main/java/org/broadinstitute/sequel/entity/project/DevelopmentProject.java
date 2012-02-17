package org.broadinstitute.sequel.entity.project;

public interface DevelopmentProject extends Project {

    public Iterable<ExperimentalCondition> getConditions();


    public static interface ExperimentalCondition {

        public String getName();
    }
}

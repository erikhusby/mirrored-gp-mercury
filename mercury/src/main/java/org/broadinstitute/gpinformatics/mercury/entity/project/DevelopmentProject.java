package org.broadinstitute.gpinformatics.mercury.entity.project;

public abstract class DevelopmentProject extends Project {

    public abstract Iterable<ExperimentalCondition> getConditions();


    public static interface ExperimentalCondition {

        public String getName();
    }
}

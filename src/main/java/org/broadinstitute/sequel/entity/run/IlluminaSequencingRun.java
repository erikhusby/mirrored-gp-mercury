package org.broadinstitute.sequel.entity.run;

import org.broadinstitute.sequel.entity.person.Person;

public class IlluminaSequencingRun implements SequencingRun {

    public IlluminaSequencingRun(IlluminaFlowcell flowcell,
                                 String runName,
                                 String runBarcode,
                                 String machineName,
                                 Person operator,
                                 boolean isTestRun) {

    }

    @Override
    public String getRunName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getRunBarcode() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public String getMachineName() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Person getOperator() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public boolean isTestRun() {
        throw new RuntimeException("I haven't been written yet.");
    }

    @Override
    public Iterable<RunCartridge> getSampleCartridge() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

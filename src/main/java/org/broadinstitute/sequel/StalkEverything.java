package org.broadinstitute.sequel;



public class StalkEverything extends AbstractStalker {

    @Override
    public void stalk(LabEvent event) {
        throw new RuntimeException("Method not yet implemented.");
    }

    @Override
    public void stalk() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

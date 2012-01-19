package org.broadinstitute.sequel;



public class StalkEverything extends AbstractStalker {

    @Override
    public void stalk(LabEvent event) {
        SampleSheetAlertUtil.doAlert(event.getEventOperator().getLogin() + " completed " + event.getEventName() + " at " + event.getEventDate(),event.getAllSampleSheets(),true);

    }

    @Override
    public void stalk() {
        throw new RuntimeException("I haven't been written yet.");
    }
}

package org.broadinstitute.gpinformatics.infrastructure.search;

/**
 * Restrict search entity types to availability in this enum
 * Created by jsacco on 7/22/2014.
 */
public enum SearchEntityType {
    LAB_VESSEL( "Lab Vessel", "LabVessel" ),
    LAB_EVENT( "Lab Event", "LabEvent" );

    private String displayText, formValue;

    private SearchEntityType( String displayText, String formValue ) {
        this.displayText = displayText;
        this.formValue = formValue;
    }

    public String getDisplayText(){
        return this.displayText;
    }

    public String getFormValue() {
        return this.formValue;
    }

}

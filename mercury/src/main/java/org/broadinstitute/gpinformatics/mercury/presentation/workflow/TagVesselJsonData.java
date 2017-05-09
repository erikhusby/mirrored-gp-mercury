package org.broadinstitute.gpinformatics.mercury.presentation.workflow;

/**
 * Class to map & persist JSON data to TagVesselActionBean
 *
 */

public class TagVesselJsonData {

     private   String position;
     private   String selection;
     private   String devCondition;

     public String getPosition() { return position; }

     public String getSelection() {
          return selection;
     }

     public void setPosition(String position) {
          this.position = position;
     }

     public void setSelection(String selection) {
          this.selection = selection;
     }

     public void setDevCondition(String devCondtion) {
          this.devCondition = devCondtion;
     }

     public String getDevCondition() {
          return devCondition;
     }
}

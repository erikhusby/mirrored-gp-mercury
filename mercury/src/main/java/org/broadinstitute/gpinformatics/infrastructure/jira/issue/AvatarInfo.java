package org.broadinstitute.gpinformatics.infrastructure.jira.issue;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Scott Matthews
 *         Date: 10/3/12
 *         Time: 3:43 PM
 */
public class AvatarInfo {

    private String sixteenSquare;
    private String fortyEightSquare;


    public String getSixteenSquare() {
        return sixteenSquare;
    }

    @JsonProperty("16x16")
    public void setSixteenSquare(String sixteenSquareIn) {
        sixteenSquare = sixteenSquareIn;
    }

    public String getFortyEightSquare() {
        return fortyEightSquare;
    }

    @JsonProperty("48x48")
    public void setFortyEightSquare(String fortyEightSquareIn) {
        fortyEightSquare = fortyEightSquareIn;
    }
}

/*
 * The Broad Institute
 * SOFTWARE COPYRIGHT NOTICE AGREEMENT
 * This software and its documentation are copyright 2013 by the
 * Broad Institute/Massachusetts Institute of Technology. All rights are reserved.
 *
 * This software is supplied without any warranty or guaranteed support
 * whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 * use, misuse, or functionality.
 */


package org.broadinstitute.gpinformatics.mercury.entity.rework;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEvent;
import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class ReworkEntry extends RapSheetEntry {
    @Enumerated(EnumType.STRING)
    @NotNull
    private ReworkReason reworkReason;

    @Enumerated(EnumType.STRING)
    @NotNull
    private ReworkLevel reworkLevel;

    @NotNull
    @Enumerated(EnumType.STRING)
    private LabEventType reworkStep;

    public ReworkEntry(RapSheet rapSheet, ReworkReason reworkReason, ReworkLevel reworkLevel, LabEventType reworkStep,
                       LabVesselPosition labVesselPosition) {
        super(rapSheet, labVesselPosition);
        this.reworkReason = reworkReason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
    }

    public ReworkEntry() {

    }

    public ReworkReason getReworkReason() {
        return reworkReason;
    }

    public void setReworkReason(ReworkReason reason) {
        this.reworkReason = reason;
    }

    public ReworkLevel getReworkLevel() {
        return reworkLevel;
    }

    public void setReworkLevel(ReworkLevel reworkLevel) {
        this.reworkLevel = reworkLevel;
    }

    public LabEventType getReworkStep() {
        return reworkStep;
    }

    public void setReworkStep(LabEventType reworkStep) {
        this.reworkStep = reworkStep;
    }
}

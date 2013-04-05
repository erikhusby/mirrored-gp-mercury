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


package org.broadinstitute.gpinformatics.mercury.entity.rapsheet;

import org.broadinstitute.gpinformatics.mercury.entity.labevent.LabEventType;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.validation.constraints.NotNull;

@Entity
@Audited
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

    public ReworkEntry(ReworkReason reworkReason, ReworkLevel reworkLevel, LabEventType reworkStep,
                       LabVesselPosition labVesselPosition) {
        setLabVesselPosition(labVesselPosition);
        this.reworkReason = reworkReason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
    }

    public ReworkEntry() {

    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> labVesselComment,
                       ReworkReason reason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        super(labVesselPosition, labVesselComment);
        this.reworkReason = reason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
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

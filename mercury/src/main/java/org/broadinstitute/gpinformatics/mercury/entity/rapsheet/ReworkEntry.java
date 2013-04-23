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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

/**
 * A ReworkEntry is marks that a sample needs to be reworked (activeRework = true) or that it has been reworked
 * (activeRework = false). Other information on the event are why the rework is being requested, and to what level
 * it is being reworked; see (@link(ReworkReason))
 */
@Entity
@Audited
public class ReworkEntry extends RapSheetEntry {
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkReason reworkReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReworkLevel reworkLevel;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private LabEventType reworkStep;

    /**
     * Active rework is work that is in a bucket, pdo, lcset, or whatever.
     * IE, It is Actively being worked on, and is not available to put into
     * a bucket.
     *
     * This is a Big B Boolean, because hibernate was complaining about not being able
     * so assign null to a primitive.
     */
    @Column(nullable = false)
    private boolean activeRework = false;

    public ReworkEntry() {

    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> labVesselComment,
                       ReworkReason reason, ReworkLevel reworkLevel, LabEventType reworkStep) {
        super(labVesselPosition, labVesselComment);
        this.reworkReason = reason;
        this.reworkLevel = reworkLevel;
        this.reworkStep = reworkStep;
    }

    public ReworkEntry(LabVesselPosition labVesselPosition, LabVesselComment<ReworkEntry> rapSheetComment) {
        super(labVesselPosition, rapSheetComment);
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

    public boolean isActiveRework() {
        return activeRework;
    }

    public void setActiveRework(boolean activeRework) {
        this.activeRework = activeRework;
    }

    /**
     * The lab recognizes these types of rework. They refer to them as Type 1, 2 or 3. This will tell the lab
     * what they need to rework and how it effects the rest of the batch.
     */
    public static enum ReworkLevel {
        ONE_SAMPLE_HOLD_REST_BATCH("Type 1", "Rework one sample and hold up the rest of the batch."),
        ONE_SAMPLE_RELEASE_REST_BATCH("Type 2", "Rework one sample let the rest of the batch proceed. "),
        ENTIRE_BATCH("Type 3", "Rework all samples in the batch.");

        private final String value;
        private final String description;

        private ReworkLevel(String value, String description) {
            this.value = value;
            this.description = description;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Why the rework is happening. This list needs to be added to.
     */
    public static enum ReworkReason {
        MACHINE_ERROR("Machine Error"),
        UNKNOWN_ERROR("Unknown Error");

        private final String value;

        ReworkReason(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}

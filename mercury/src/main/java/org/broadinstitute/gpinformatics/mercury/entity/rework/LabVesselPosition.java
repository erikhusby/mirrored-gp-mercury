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

import org.broadinstitute.gpinformatics.mercury.entity.sample.MercurySample;
import org.broadinstitute.gpinformatics.mercury.entity.vessel.VesselPosition;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Audited
@Table(schema = "mercury")
public class LabVesselPosition {
    @SuppressWarnings("UnusedDeclaration")
    @Id
    @SequenceGenerator(name = "SEQ_LV_POS", schema = "mercury", sequenceName = "SEQ_LV_POS")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LV_POS")
    private Long labVesselPositionId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private List<MercurySample> mercurySamples;

    @Enumerated(EnumType.STRING)
    VesselPosition vesselPosition;

    public LabVesselPosition() {
    }

    public List<MercurySample> getMercurySamples() {
        if (mercurySamples==null){
            mercurySamples=new ArrayList<MercurySample>();
        }

        return mercurySamples;
    }

    public LabVesselPosition(VesselPosition vesselPosition,MercurySample ... mercurySamples) {
        this.vesselPosition = vesselPosition;
        Collections.addAll(getMercurySamples(),mercurySamples);
    }

    public void setMercurySamples(List<MercurySample> mercurySamples) {

        this.mercurySamples = mercurySamples;
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }

    public void setVesselPosition(VesselPosition vesselPosition) {
        this.vesselPosition = vesselPosition;
    }
}

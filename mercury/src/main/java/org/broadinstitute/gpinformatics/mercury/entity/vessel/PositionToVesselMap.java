package org.broadinstitute.gpinformatics.mercury.entity.vessel;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Many to many association between container and lab vessel.  Holds position of vessel in container.
 */
@Entity
@Audited
@Table(schema = "mercury", name = "LV_MAP_POSITION_TO_VESSEL"/*,
        uniqueConstraints = @UniqueConstraint(name = "", columnNames = {"", ""})*/
)
public class PositionToVesselMap {

    @SuppressWarnings("unused")
    @Id
    @SequenceGenerator(name = "SEQ_LV_MAP_POSITION_TO_VESSEL", schema = "mercury",
            sequenceName = "SEQ_LV_MAP_POSITION_TO_VESSEL")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_LV_MAP_POSITION_TO_VESSEL")
    @Column(name = "LV_MAP_POSITION_TO_VESSEL_ID")
    private Long mapPositionToVesselId;

    @ManyToOne
    @JoinColumn(name = "LAB_VESSEL")
    private LabVessel container;

    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "MAP_POSITION_TO_VESSEL")
    private LabVessel containee;

    @Enumerated(EnumType.STRING)
    @Column(name = "MAPKEY")
    private VesselPosition vesselPosition;

    public PositionToVesselMap(LabVessel container, LabVessel containee, VesselPosition vesselPosition) {
        this.container = container;
        this.containee = containee;
        this.vesselPosition = vesselPosition;
    }

    public PositionToVesselMap() {
    }

    public LabVessel getContainer() {
        return container;
    }

    public LabVessel getContainee() {
        return containee;
    }

    public VesselPosition getVesselPosition() {
        return vesselPosition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        PositionToVesselMap that = (PositionToVesselMap) o;

        return new EqualsBuilder()
                .append(mapPositionToVesselId, that.mapPositionToVesselId)
                .append(getContainer(), that.getContainer())
                .append(getContainee(), that.getContainee())
                .append(getVesselPosition(), that.getVesselPosition())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getContainer())
                .append(getContainee())
                .append(getVesselPosition())
                .toHashCode();
    }
}

package org.broadinstitute.gpinformatics.mercury.entity.reagent;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Entity
@Audited
@Table(schema = "mercury", name = "umi",
        uniqueConstraints = {@UniqueConstraint(columnNames = {"location", "length", "spacer_length"})})
public class UniqueMolecularIdentifier {
    public enum UMILocation {
        BEFORE_FIRST_READ("Before First Read"),
        INLINE_FIRST_READ("Inline First Read"),
        BEFORE_FIRST_INDEX_READ("Before First Index Read"),
        BEFORE_SECOND_INDEX_READ("Before Second Index Read"),
        BEFORE_SECOND_READ("Before Second Read"),
        INLINE_SECOND_READ("Inline Second Read");

        private final String displayName;

        private static final Map<String, UMILocation> MAP_NAME_TO_LOCATION =
                new HashMap<>(UMILocation.values().length);

        UMILocation(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }

        public static UMILocation getByName(String locationName) {
            return MAP_NAME_TO_LOCATION.get(locationName);
        }

        static {
            for (UMILocation location : UMILocation.values()) {
                MAP_NAME_TO_LOCATION.put(location.getDisplayName(), location);
            }
        }
    }

    @Id
    @SequenceGenerator(name = "SEQ_UMI", schema = "mercury", sequenceName = "SEQ_UMI")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_UMI")
    @Column(name = "UMI_ID")
    private Long uniqueMolecularIdentifierId;

    @Enumerated(EnumType.STRING)
    private UMILocation location;

    private Long length;

    @Column(name = "SPACER_LENGTH")
    private Long spacerLength;

    @OneToMany(mappedBy = "uniqueMolecularIdentifier")
    private Set<UMIReagent> umiReagents = new HashSet<>();

    /** For JPA. */
    public UniqueMolecularIdentifier() {
    }

    public UniqueMolecularIdentifier(UMILocation location, Long length, long spacerLength) {
        this.location = location;
        this.length = length;
        this.spacerLength = spacerLength;
    }

    public UMILocation getLocation() {
        return location;
    }

    public void setLocation(UMILocation location) {
        this.location = location;
    }

    public Long getLength() {
        return length;
    }

    public void setLength(Long length) {
        this.length = length;
    }

    public Long getSpacerLength() {
        return spacerLength;
    }

    public void setSpacerLength(Long spacerLength) {
        this.spacerLength = spacerLength;
    }

    public String getDisplayName() {
        return String.format("%dM%dS %s", length, spacerLength, location.getDisplayName());
    }

    public UMIReagent getUmiReagent() {
        if (umiReagents.isEmpty()) {
            return null;
        }
        return umiReagents.iterator().next();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !(OrmUtil.proxySafeIsInstance(o, UniqueMolecularIdentifier.class))) {
            return false;
        }

        UniqueMolecularIdentifier umi = OrmUtil.proxySafeCast(o, UniqueMolecularIdentifier.class);

        EqualsBuilder equalsBuilder = new EqualsBuilder().append(location, umi.getLocation());
        equalsBuilder.append(length, umi.getLength());
        equalsBuilder.append(spacerLength, umi.getSpacerLength());
        return equalsBuilder.isEquals();
    }

    @Override
    public int hashCode() {
        HashCodeBuilder hashCodeBuilder = new HashCodeBuilder().append(location)
                .append(length).append(spacerLength);
        return hashCodeBuilder.hashCode();
    }
}

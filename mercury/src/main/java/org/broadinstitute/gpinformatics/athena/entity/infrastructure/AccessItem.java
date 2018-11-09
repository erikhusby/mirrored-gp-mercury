package org.broadinstitute.gpinformatics.athena.entity.infrastructure;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "ACCESS_ITEM", schema = "athena")
public class AccessItem {

    @Id
    @SequenceGenerator(name = "SEQ_ACCESS_CONTROL_ITEM", schema = "athena", sequenceName = "SEQ_ACCESS_CONTROL_ITEM")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_ACCESS_CONTROL_ITEM")
    private Long id;

    private String itemValue;

    @ManyToOne
    @JoinColumn(name = "ACCESS_CONTROL")
    private AccessControl accessControl;

    public AccessItem() {
    }

    public AccessItem(String itemValue) {
        this.itemValue = itemValue;
    }

    public String getItemValue() {
        return itemValue;
    }

    public AccessControl getAccessControl() {
        return accessControl;
    }

    public void setAccessControl(
            AccessControl accessControl) {
        this.accessControl = accessControl;
    }

    void remove() {
        accessControl = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        AccessItem that = (AccessItem) o;

        return new EqualsBuilder()
                .append(itemValue, that.itemValue)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(itemValue)
                .toHashCode();
    }
}

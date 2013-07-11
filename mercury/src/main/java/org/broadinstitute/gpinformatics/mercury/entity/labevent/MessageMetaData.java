package org.broadinstitute.gpinformatics.mercury.entity.labevent;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.broadinstitute.gpinformatics.mercury.entity.OrmUtil;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * Mercury entity to represent MetaData associated with a
 */
@Entity
@Audited
@Table(schema = "mercury")
public class MessageMetaData {


    @Id
    @SequenceGenerator(name = "SEQ_MESSAGE_META_DATA", schema = "mercury", sequenceName = "SEQ_MESSAGE_META_DATA")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MESSAGE_META_DATA")
    private Long metaDataId;


    private String name;

    private String value;

    protected MessageMetaData() {
    }

    public MessageMetaData(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !OrmUtil.proxySafeIsInstance(o, MessageMetaData.class)) {
            return false;
        }

        MessageMetaData that = (MessageMetaData) o;


        return new EqualsBuilder().append(getName(), that.getName()).append(getValue(), that.getValue()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(getName()).append(getValue()).toHashCode();
    }
}

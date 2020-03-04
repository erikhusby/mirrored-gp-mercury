package org.broadinstitute.gpinformatics.athena.entity.work;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.hibernate.envers.Audited;

import javax.annotation.Nonnull;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.text.MessageFormat;

/**
 * This holds a single piece of message data
 */
@Entity
@Audited
@Table(name= "MESSAGE_DATA_VALUE", schema = "athena")
public class MessageDataValue {

    protected MessageDataValue() {
    }

    public MessageDataValue(@Nonnull String key, @Nonnull String value) {
        this.key = key;
        this.value = value;
    }

    @Id
    @SequenceGenerator(name = "SEQ_MESSAGE_DATA_VALUE", schema = "athena", sequenceName = "SEQ_MESSAGE_DATA_VALUE", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MESSAGE_DATA_VALUE")
    private Long messageDataValueId;

    @Column(name = "KEY", nullable = false)
    @Nonnull
    private String key;

    @Column(name = "VALUE", nullable = false)
    @Nonnull
    private String value;

    @Nonnull
    public String getKey() {
        return key;
    }

    @Nonnull
    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MessageFormat.format("({0}, {1})", key, value);
    }

    public boolean booleanValue() {
        boolean booleanValue = false;
        if (NumberUtils.isCreatable(value)) {
            booleanValue = BooleanUtils.toBoolean(NumberUtils.createDouble(value).intValue());
        }
        return booleanValue;
    }
}

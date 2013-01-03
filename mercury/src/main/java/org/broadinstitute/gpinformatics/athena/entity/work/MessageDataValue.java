package org.broadinstitute.gpinformatics.athena.entity.work;

import org.hibernate.envers.Audited;

import javax.persistence.*;
import java.text.MessageFormat;

/**
 * This holds a single piece of message data
 */
@Entity
@Audited
@Table(name= "MESSAGE_DATA_VALUE", schema = "athena")
public class MessageDataValue {

    MessageDataValue() {
    }

    public MessageDataValue(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Id
    @SequenceGenerator(name = "SEQ_MESSAGE_DATA_VALUE", schema = "athena", sequenceName = "SEQ_MESSAGE_DATA_VALUE", allocationSize = 50)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_MESSAGE_DATA_VALUE")
    private Long messageDataValueId;

    @Column(name = "KEY")
    private String key;

    @Column(name = "VALUE")
    private String value;

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return MessageFormat.format("({0}, {1})", key, value);
    }
}

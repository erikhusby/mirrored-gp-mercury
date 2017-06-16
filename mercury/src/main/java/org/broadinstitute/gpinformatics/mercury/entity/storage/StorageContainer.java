package org.broadinstitute.gpinformatics.mercury.entity.storage;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import java.util.Date;

@Entity
@Audited
@Table(schema = "mercury")
public class StorageContainer {

    @Id
    @SequenceGenerator(name = "SEQ_STORAGE_CONTAINER", schema = "mercury", sequenceName = "SEQ_STORAGE_CONTAINER")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "SEQ_STORAGE_CONTAINER")
    @Column(name = "STORAGE_CONTAINER_ID", nullable = true)
    private Long storageContainerId;

    private Date createdOn;

    private Date lastUpdatedOn;
}

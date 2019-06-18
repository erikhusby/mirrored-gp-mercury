package org.broadinstitute.gpinformatics.athena.entity.orders;

import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

/**
 * This class associates a person with a product order kit
 */
@Entity
@Audited
@Table(name = "product_order_kit_person", schema = "athena")
public class ProductOrderKitPerson {

    @Id
    @SequenceGenerator(name = "seq_pdo_kit_person", schema = "athena", sequenceName = "seq_pdo_kit_person")
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seq_pdo_kit_person")
    private Long productOrderKitPersonId;

    /**
     * This is eager fetched because this class' whole purpose is to bridge a specific person and PDO Kit. If you
     * ever only need the ID, you should write a specific projection query in the DAO
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name="PRODUCT_ORDER_KIT")
    private ProductOrderKit productOrderKit;

    /** Person ID is BSP User ID. */
    @Column(name = "PERSON_ID")
    private Long personId;

    protected ProductOrderKitPerson() { }

    public ProductOrderKitPerson(ProductOrderKit productOrderKit, Long personId) {
        this.productOrderKit = productOrderKit;
        this.personId = personId;
    }

    public ProductOrderKit getProductOrderKit() {
        return productOrderKit;
    }

    public Long getPersonId() {
        return personId;
    }
}

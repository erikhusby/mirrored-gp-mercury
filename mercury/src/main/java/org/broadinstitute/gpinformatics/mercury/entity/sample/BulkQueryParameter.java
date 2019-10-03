package org.broadinstitute.gpinformatics.mercury.entity.sample;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * Maps a Global Temporary Table that is loaded with (e.g.) sample IDs from BSP during User Defined Searches.
 */
@Entity
@Table(schema = "mercury")
public class BulkQueryParameter {
    @Id
    private String param;

    public BulkQueryParameter(String param) {
        this.param = param;
    }

    /**
     * For JPA.
     */
    protected BulkQueryParameter() {
    }

    public String getParam() {
        return param;
    }
}

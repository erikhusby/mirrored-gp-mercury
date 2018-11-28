package org.broadinstitute.gpinformatics.athena.entity.products;

import org.broadinstitute.gpinformatics.mercury.entity.run.AttributeArchetype;
import org.hibernate.envers.Audited;

import javax.persistence.Entity;
import javax.persistence.Transient;

/**
 * Contains the genotyping chip configurable attributes of a product order.
 */
@Audited
@Entity
public class GenotypingProductOrderMapping extends AttributeArchetype {
    public static final String GENOTYPING_CHIP_OVERRIDE_CONFIG = "Genotyping Chip Override";
    public static final String CALL_RATE_THRESHOLD = "call_rate_threshold";
    public static final String CLUSTER_LOCATION = "cluster_location_unix";
    public static final String MAPPING_GROUP = "genotypingProductOrderMapping";
    public static final String ATTRIBUTES_GROUP = "genotypingProductOrder";
    public static final String CALL_RATE_OVERRIDE_MAPPING = "Call Threshold Override";
    public static final String CLUSTER_FILE_OVERRIDE_MAPPING = "Cluster File Override";

    public GenotypingProductOrderMapping() {
    }

    public GenotypingProductOrderMapping(String mappingName, String callRateThreshold, String clusterLocation) {
        super(GENOTYPING_CHIP_OVERRIDE_CONFIG, mappingName);
        addOrSetAttribute(CALL_RATE_THRESHOLD, callRateThreshold);
        addOrSetAttribute(CLUSTER_LOCATION, clusterLocation);
    }

    public String getCallRateThreshold() {
        return getAttributeMap().get(CALL_RATE_THRESHOLD);
    }

    @Transient
    public void setCallRateThreshold(String zCallThreshold) {
        addOrSetAttribute(CALL_RATE_THRESHOLD, zCallThreshold);
    }

    public String getClusterLocation() {
        return getAttributeMap().get(CLUSTER_LOCATION);
    }

    @Transient
    public void setClusterLocation(String clusterLocation) {
        addOrSetAttribute(CLUSTER_LOCATION, clusterLocation);
    }
}

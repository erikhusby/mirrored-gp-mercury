package org.broadinstitute.gpinformatics.mercury.boundary.vessel;

import clover.org.apache.commons.lang3.tuple.Pair;
import org.broadinstitute.gpinformatics.mercury.boundary.labevent.MetadataBean;
import org.broadinstitute.gpinformatics.mercury.entity.Metadata;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RowMetadata {
    private String well;
    private BigDecimal volume;
    private String plateBarcode;
    private Boolean positiveControl;
    private Boolean negativeControl;
    private List<Metadata> metadata = new ArrayList<>();
    private List<MetadataBean> metadataBeans = new ArrayList<>();
    private Map<String, Pair<Integer, Metadata>> mapKeyNameToMetadata = new HashMap<>();

    public RowMetadata() {
    }

    public RowMetadata(String plateBarcode, String well, BigDecimal volume, Boolean positiveControl,
                       Boolean negativeControl, List<Metadata> metadata) {
        this.plateBarcode = plateBarcode;
        this.well = well;
        this.volume = volume;
        this.positiveControl = positiveControl;
        this.negativeControl = negativeControl;
        this.metadata = metadata;
    }

    public String getWell() {
        return well;
    }

    public void setWell(String well) {
        this.well = well;
    }

    public void setVolume(BigDecimal volume) {
        this.volume = volume;
    }

    public void setPositiveControl(Boolean positiveControl) {
        this.positiveControl = positiveControl;
    }

    public void setNegativeControl(Boolean negativeControl) {
        this.negativeControl = negativeControl;
    }

    public BigDecimal getVolume() {
        return volume;
    }

    public String getPlateBarcode() {
        return plateBarcode;
    }

    public void setPlateBarcode(String plateBarcode) {
        this.plateBarcode = plateBarcode;
    }

    public Boolean getPositiveControl() {
        return positiveControl;
    }

    public Boolean getNegativeControl() {
        return negativeControl;
    }

    public List<Metadata> getMetadata() {
        if (metadata.isEmpty() && !metadataBeans.isEmpty()) {
            for (MetadataBean metadataBean: this.metadataBeans ) {
                Metadata.Key key = Metadata.Key.fromDisplayName(metadataBean.getName());
                metadata.add(Metadata.createMetadata(key, metadataBean.getValue()));
            }
        }
        return metadata;
    }

    public void setMetadata(List<Metadata> metadata) {
        System.out.println("Set metadata Called: " + getMetadataBeans().size());
        this.metadata = metadata;
    }

    public List<MetadataBean> getMetadataBeans() {
        return metadataBeans;
    }

    public void setMetadataBeans(List<MetadataBean> metadataBeans) {
        this.metadataBeans = metadataBeans;
    }

    public Pair<Integer, Metadata> findByKeyName(String keyName) {
        if (mapKeyNameToMetadata.isEmpty()) {
            mapKeyNameToMetadata = new HashMap<>();
            for (int i = 0; i < getMetadata().size(); i++) {
                Metadata currMeta = getMetadata().get(i);
                mapKeyNameToMetadata.put(currMeta.getKey().getDisplayName(), Pair.of(i, currMeta));
            }
        }
        return mapKeyNameToMetadata.get(keyName);
    }


    public boolean isControl() {
        return positiveControl != null && negativeControl != null;
    }
}

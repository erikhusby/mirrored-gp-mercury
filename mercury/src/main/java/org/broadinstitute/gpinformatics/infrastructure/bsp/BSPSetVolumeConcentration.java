package org.broadinstitute.gpinformatics.infrastructure.bsp;

import java.io.Serializable;

public interface BSPSetVolumeConcentration extends Serializable {

    public static String VALID_COMMUNICATION_PREFIX = "updated volume and concentration for";

    public void setVolumeAndConcentration(String barcode, double volume, double concentration);
    public String[] getResult();
    public boolean isValidResult();
}

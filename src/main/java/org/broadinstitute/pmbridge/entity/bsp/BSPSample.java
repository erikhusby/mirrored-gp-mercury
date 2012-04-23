package org.broadinstitute.pmbridge.entity.bsp;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.broadinstitute.pmbridge.infrastructure.bsp.BSPSampleDTO;

import javax.persistence.Transient;
import java.math.BigDecimal;

/**
 * Created by IntelliJ IDEA.
 * User: mccrory
 * Date: 4/2/12
 * Time: 5:24 PM
 */
public class BSPSample {

    private final SampleId id;
    private BSPSampleDTO bspDTO;

    public BSPSample(SampleId id) {
        this.id = id;
    }

    public BSPSample(SampleId id, BSPSampleDTO bspDTO) {
        this.id = id;
        this.bspDTO = bspDTO;
    }

    public SampleId getId() {
        return id;
    }

    public BSPSampleDTO getBspDTO() {
        return bspDTO;
    }

    public void setBspDTO(BSPSampleDTO bspDTO) {
        this.bspDTO = bspDTO;
    }


    @Transient
    /**
     * Has the underlying BSP DTO been initialized?
     */
    public boolean hasBSPDTOBeenInitialized() {
        return bspDTO != null;
    }

    @Transient
    /**
     * Gets the container id from the underlying
     * BSP DTO.
     */
    public String getContainerId() {
        return bspDTO.getContainerId();
    }

    @Transient
    /**
     * Gets the patient id from the underlying
     * BSP DTO.
     */
    public String getPatientId() {
        return bspDTO.getPatientId();
    }

    @Transient
    /**
     * Gets the organism name from the
     * underlying BSP DTO.
     */
    public String getOrganism() {
        return bspDTO.getOrganism();
    }


    @Transient
    /**
     * Gets the volume from the
     * underlying BSP DTO.
     */
    public BigDecimal getVolume() {
        return bspDTO.getVolume();
    }

    @Transient
    /**
     * Gets the conc from the
     * underlying BSP DTO.
     */
    public BigDecimal getConcentration() {
        return bspDTO.getConcentration();
    }



    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
     }
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}

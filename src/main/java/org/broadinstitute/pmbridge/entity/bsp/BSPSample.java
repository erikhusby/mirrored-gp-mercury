package org.broadinstitute.pmbridge.entity.bsp;

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
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof BSPSample)) return false;

        final BSPSample bspSample = (BSPSample) o;

        if (bspDTO != null ? !bspDTO.equals(bspSample.bspDTO) : bspSample.bspDTO != null) return false;
        if (id != null ? !id.equals(bspSample.id) : bspSample.id != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (bspDTO != null ? bspDTO.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "BSPSample{" +
                "id=" + id +
                ", bspDTO=" + bspDTO +
                '}';
    }
}

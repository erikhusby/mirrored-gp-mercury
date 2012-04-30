package org.broadinstitute.sequel.entity.vessel;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.Serializable;

/**
 * When we receive an event from lab, one of the
 * first things we do in the LabEvent class is
 * verify that the molecular state of the incoming
 * sample is what is expected.
 *
 * What we're defending against here is an accident
 * that can destroy a sample.  Each step in the lab
 * (think automation) has preconditions which are a
 * combination of concentration, volume, molecular
 * envelope status, RNA/DNA, etc.  That's what
 * this class attempts to define.
 *
 * If we make an association between something like
 * event name and ExpectedMolecularState in the database,
 * then changing the required inputs for a lab step
 * becomes a simple configuration change.
 *
 * Does the new PCR protocol accept lower concentration?
 * Yes!  Just alter the table to reflect the min concentration.
 * Can we now put primer complexes onto the middle
 * of an adpator?  Yes!  Just change the configuration.
 *
 * Interpret nulls as "any value accepted".
 *
 * LabEvents and LabWorkQueues both make use of expected
 * molecular state.
 */
@Entity
public class MolecularState implements Serializable {

    @Id
    private Long molecularStateId;

    public enum DNA_OR_RNA {
        DNA,
        RNA
    };

    public enum STRANDEDNESS {
        DOUBLE_STRANDED,
        SINGLE_STRANDED
    }

    private MolecularEnvelope molecularEnvelope;
    private DNA_OR_RNA nucleicAcidState;
    private STRANDEDNESS strand;
    private MolecularStateTemplate molecularStateTemplate = new MolecularStateTemplate();

    /**
     * The molecular envelope that contains
     * the sample.  This is the "outer" most
     * envelope.
     * @return
     */
    public MolecularEnvelope getMolecularEnvelope() {
        return molecularEnvelope;
    }

    public void setMolecularEnvelope(MolecularEnvelope molecularEnvelopeDelta) {
        molecularEnvelope = molecularEnvelopeDelta;
    }

    /**
     * Is the target sample in the evenlope DNA or RNA?
     * @return
     */
    public DNA_OR_RNA getNucleicAcidState() {
        return nucleicAcidState;
    }

    /**
     * Is the sample stranded on an island,
     * all by itself?
     *
     * Just kidding.  Is it double stranded
     * or has it been denatured?
     * @return
     */
    public STRANDEDNESS getStrand() {
        return strand;
    }

    /**
     * While it might be stored as a metric and
     * considered a metric, concentration is a
     * special metric and is somewhat tied
     * up with the molecular state.
     * @return
     */
    public Float getConcentration() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * Fluid volume.  Could be considered a metric
     * as well, but it is critical for molecular
     * state.
     * @return
     */
    public Float getVolume() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * What's the generalized template used to determine
     * whether a {@link Goop} has consistent state?
     * @return
     */
    public MolecularStateTemplate getMolecularStateTemplate() {
        return molecularStateTemplate;
    }

}

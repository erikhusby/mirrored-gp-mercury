package org.broadinstitute.sequel.entity.vessel;

/**
 * Implementation of MolecularState
 */
public class MolecularStateImpl implements MolecularState {
    private MolecularEnvelope molecularEnvelope;
    private DNA_OR_RNA nucleicAcidState;
    private STRANDEDNESS strand;
    private MolecularStateTemplate molecularStateTemplate = new MolecularStateTemplate();

    @Override
    public MolecularEnvelope getMolecularEnvelope() {
        return molecularEnvelope;
    }

    @Override
    public DNA_OR_RNA getNucleicAcidState() {
        return nucleicAcidState;
    }

    @Override
    public STRANDEDNESS getStrand() {
        return strand;
    }

    @Override
    public Float getConcentration() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public Float getVolume() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public MolecularStateTemplate getMolecularStateTemplate() {
        return molecularStateTemplate;
    }
}

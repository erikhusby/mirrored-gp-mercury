package org.broadinstitute.sequel.entity.vessel;

import org.broadinstitute.sequel.entity.run.SequencingTechnology;

/**
 * Some molecular thing that attaches
 * to some other molecular thing.
 *
 * The subinterface we probably care
 * about is DNA-specific most of the time.
 * For example, a DNA appendage.  I might
 * call it an adaptor, but that's
 * a bit too loaded.
 *
 * Probably you want DNAAppendage
 */
public interface MolecularAppendage {

    public String getAppendageName();

    /**
     * Does this appendage render the construct
     * useful only to a single sequencing
     * technology?
     * @return
     */
    public SequencingTechnology getSequencingTechnology();
}

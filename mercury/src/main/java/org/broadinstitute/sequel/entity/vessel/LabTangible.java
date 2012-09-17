package org.broadinstitute.sequel.entity.vessel;

import javax.persistence.Transient;
import java.util.Collection;

/**
 * Some tangible thing, or just enough
 * information about said thing, that
 * the lab can take this and go do
 * some lab work.
 *
 * This might seem like an over-generalization,
 * but when you see all the lists of "things"
 * that our users keep (libraries, aliquots,
 * plates, tubes, flowcells, lanes, runs,
 * etc.), you realize that being able
 * to just group "things" is a very
 * useful feature.  A LabTangible
 * is our basic "thing".
 */
public interface LabTangible {




}
